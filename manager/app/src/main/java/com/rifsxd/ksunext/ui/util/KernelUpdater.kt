package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

/** In-app kernel updater: GitHub release -> download -> verify -> safety checks -> flash with the built-in AnyKernel flasher. */
object KernelUpdater {
    const val REPO = "namalxhero/a05m-kernel-6.6"
    private const val UA = "RootX-Kernel-Updater"

    data class Release(
        val tag: String,
        val name: String,
        val notes: String,
        val published: String,
        val zipUrl: String,
        val zipName: String,
        val zipSize: Long,
        val sha256: String?,
    )

    /** Running kernel release string, e.g. 6.6.89-RootX-r12 */
    fun currentRelease(): String = System.getProperty("os.version") ?: "unknown"

    /** Build number from a "-r<number>" suffix of the running kernel (0 if the kernel has none). */
    fun currentBuild(): Int =
        Regex("-r(\\d+)").find(currentRelease())?.groupValues?.get(1)?.toIntOrNull() ?: 0

    /** Last number inside a release tag such as "r12" or "kernel-12". */
    fun tagBuild(tag: String): Int =
        Regex("(\\d+)").findAll(tag).lastOrNull()?.value?.toIntOrNull() ?: 0

    private fun fetchText(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        c.connectTimeout = 15000
        c.readTimeout = 20000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    suspend fun fetchLatest(): Result<Release> = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", UA)
            c.connectTimeout = 15000
            c.readTimeout = 20000
            val code = c.responseCode
            if (code == 404) error("No kernel release has been published yet")
            if (code !in 200..299) error("GitHub returned HTTP $code")
            val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets") ?: JSONArray()
            var zipUrl = ""
            var zipName = ""
            var zipSize = 0L
            var shaUrl: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val n = a.getString("name")
                val u = a.getString("browser_download_url")
                if (n.endsWith(".zip") && zipUrl.isEmpty()) {
                    zipUrl = u
                    zipName = n
                    zipSize = a.optLong("size")
                }
                if (n.endsWith(".sha256")) shaUrl = u
            }
            if (zipUrl.isEmpty()) error("The release has no .zip file")
            val notes = json.optString("body")
            var sha: String? = Regex("(?i)sha-?256\\W+([0-9a-f]{64})").find(notes)?.groupValues?.get(1)
            if (sha == null && shaUrl != null) {
                sha = runCatching {
                    fetchText(shaUrl).trim().split(Regex("\\s+")).first().lowercase()
                }.getOrNull()?.takeIf { it.length == 64 }
            }
            Release(
                tag = json.getString("tag_name"),
                name = json.optString("name"),
                notes = notes,
                published = json.optString("published_at"),
                zipUrl = zipUrl,
                zipName = zipName,
                zipSize = zipSize,
                sha256 = sha?.lowercase(),
            )
        }
    }

    suspend fun download(context: Context, rel: Release, onProgress: (Long, Long) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "kernel_update")
            dir.deleteRecursively()
            dir.mkdirs()
            val out = File(dir, rel.zipName)
            val c = URL(rel.zipUrl).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", UA)
            c.instanceFollowRedirects = true
            c.connectTimeout = 15000
            c.readTimeout = 30000
            val code = c.responseCode
            if (code !in 200..299) error("Download failed: HTTP $code")
            val total = if (c.contentLengthLong > 0) c.contentLengthLong else rel.zipSize
            c.inputStream.use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(65536)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
            out
        }

    suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(65536)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }

    /** "0x<cert size>:<sha256 of cert>" of THIS app, the exact entry the kernel keeps in its manager list. */
    fun ownSignatureEntry(context: Context): String? {
        val pi = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val sig = pi.signingInfo?.apkContentsSigners?.firstOrNull() ?: return null
        val der = sig.toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(der).joinToString("") { "%02x".format(it) }
        return "0x%x:%s".format(der.size, hash)
    }

    private fun indexOf(buf: ByteArray, len: Int, needle: ByteArray): Int {
        val last = len - needle.size
        var i = 0
        while (i <= last) {
            if (buf[i] == needle[0]) {
                var j = 1
                while (j < needle.size && buf[i + j] == needle[j]) j++
                if (j == needle.size) return i
            }
            i++
        }
        return -1
    }

    private fun streamContains(input: InputStream, needle: ByteArray): Boolean {
        val buf = ByteArray(1 shl 20)
        var carry = 0
        while (true) {
            val n = input.read(buf, carry, buf.size - carry)
            if (n < 0) return false
            val len = carry + n
            if (indexOf(buf, len, needle) >= 0) return true
            carry = minOf(needle.size - 1, len)
            System.arraycopy(buf, len - carry, buf, 0, carry)
        }
    }

    /**
     * true  = the kernel inside the zip trusts this manager's signature
     * false = it does not (flashing would lock this manager out)
     * null  = no raw Image found, cannot verify
     */
    suspend fun zipTrustsManager(zip: File, entry: String): Boolean? = withContext(Dispatchers.IO) {
        val needle = entry.toByteArray(Charsets.US_ASCII)
        ZipFile(zip).use { z ->
            val e = z.getEntry("Image")
                ?: z.entries().asSequence().firstOrNull { it.name.substringAfterLast('/') == "Image" }
                ?: return@use null
            z.getInputStream(e).use { streamContains(it, needle) }
        }
    }

    /** Saves the current boot partition to /sdcard/RootX. Returns the file path. */
    suspend fun backupBoot(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = "/sdcard/RootX"
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dst = "$dir/boot_backup_$ts.img"
            Shell.cmd(
                "mkdir -p $dir",
                "dd if=/dev/block/by-name/boot of=$dst bs=4194304"
            ).exec()
            val size = Shell.cmd("stat -c %s $dst").exec().out.firstOrNull()?.trim()?.toLongOrNull() ?: 0L
            if (size < 1_000_000L) error("boot.img backup failed (size $size)")
            dst
        }
    }
}
