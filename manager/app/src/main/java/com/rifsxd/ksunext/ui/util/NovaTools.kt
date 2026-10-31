package com.rifsxd.ksunext.ui.util

import android.content.Context
import com.android.apksig.ApkSigner
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// ===================== Bootloop rescue (Nova Boot Guard module) =====================

const val NOVA_GUARD_ID = "aaa_nova_bootguard"
private const val MODULES_DIR = "/data/adb/modules"
private const val GUARD_FLAG = "/data/adb/nova_rescue_triggered"
private const val GUARD_COUNT = "/data/adb/nova_boot_count"

data class BootGuardStatus(val installed: Boolean, val triggered: String?, val bootCount: Int)

suspend fun readBootGuardStatus(): BootGuardStatus = withContext(Dispatchers.IO) {
    val installed = SuFile("$MODULES_DIR/$NOVA_GUARD_ID").exists()
    val flag = Shell.cmd("cat $GUARD_FLAG 2>/dev/null").exec().out.joinToString(" ").trim().ifEmpty { null }
    val cnt = Shell.cmd("cat $GUARD_COUNT 2>/dev/null").exec().out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
    BootGuardStatus(installed, flag, cnt)
}

suspend fun installBootGuard(context: Context): Boolean = withContext(Dispatchers.IO) {
    val tmp = File(context.cacheDir, "nova_guard")
    tmp.deleteRecursively()
    tmp.mkdirs()
    val names = listOf("module.prop", "post-fs-data.sh", "service.sh")
    for (n in names) {
        context.assets.open("nova_bootguard/$n").use { input ->
            File(tmp, n).outputStream().use { out -> input.copyTo(out) }
        }
    }
    val dst = "$MODULES_DIR/$NOVA_GUARD_ID"
    val result = Shell.cmd(
        "mkdir -p '$dst'",
        "cp '${tmp.path}/module.prop' '${tmp.path}/post-fs-data.sh' '${tmp.path}/service.sh' '$dst/'",
        "chmod 755 '$dst/post-fs-data.sh' '$dst/service.sh'",
        "chmod 644 '$dst/module.prop'",
        "chown -R root:root '$dst' || true",
        "echo 0 > $GUARD_COUNT",
        "rm -f $GUARD_FLAG"
    ).exec()
    tmp.deleteRecursively()
    result.isSuccess
}

suspend fun removeBootGuard(): Boolean = withContext(Dispatchers.IO) {
    Shell.cmd("rm -rf '$MODULES_DIR/$NOVA_GUARD_ID'", "rm -f $GUARD_COUNT $GUARD_FLAG").exec().isSuccess
}

suspend fun disableAllModulesNow(): Boolean = withContext(Dispatchers.IO) {
    val script = "for m in $MODULES_DIR/*; do b=\$(basename \"\$m\"); " +
        "[ -d \"\$m\" ] && [ \"\$b\" != \"$NOVA_GUARD_ID\" ] && touch \"\$m/disable\"; done; true"
    Shell.cmd(script).exec().isSuccess
}

suspend fun enableAllModulesNow(): Boolean = withContext(Dispatchers.IO) {
    Shell.cmd("rm -f $MODULES_DIR/*/disable; true").exec().isSuccess
}

suspend fun clearBootGuardFlag(): Boolean = withContext(Dispatchers.IO) {
    Shell.cmd("rm -f $GUARD_FLAG").exec().isSuccess
}

// ===================== Root usage stats (from sulog) =====================

data class AppRootStat(val uid: Int, val label: String, val pkg: String, val count: Int, val last: String?)

suspend fun loadRootStats(context: Context): Pair<List<AppRootStat>, Int> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val uidMap = HashMap<Int, String>()
    // "package:com.example uid:10123"
    Shell.cmd("pm list packages -U").exec().out.forEach { line ->
        val p = line.substringAfter("package:", "").substringBefore(" ")
        val u = line.substringAfter("uid:", "").trim().toIntOrNull()
        if (p.isNotEmpty() && u != null && !uidMap.containsKey(u)) uidMap[u] = p
    }
    val entries = listSulogFiles().flatMap { parseSulogLines(readSulogFile(it.path)) }
    val relevant = entries.filter {
        it.eventType == SulogEventType.RootExecve ||
            it.eventType == SulogEventType.SuCompat ||
            it.eventType == SulogEventType.IoctlGrantRoot
    }
    val stats = relevant.groupBy { it.fields["uid"]?.toIntOrNull() ?: -1 }.map { (uid, list) ->
        val pkg = uidMap[uid] ?: ""
        val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
            ?: pkg.ifEmpty {
                list.firstNotNullOfOrNull { e -> e.fields["comm"]?.takeIf { c -> c.isNotBlank() } } ?: "uid $uid"
            }
        AppRootStat(uid, label, pkg, list.size, list.lastOrNull { it.timestampText != null }?.timestampText)
    }.sortedByDescending { it.count }
    stats to relevant.size
}

// ===================== Hide manager (random package repack) =====================

object ManagerHider {
    private const val KEY_FILE = "nova_sign_key.p12"

    fun keyFile(context: Context): File = File(context.filesDir, KEY_FILE)
    fun hasKey(context: Context): Boolean = keyFile(context).exists()
    fun deleteKey(context: Context): Boolean = keyFile(context).delete()

    /** Random package name with exactly [length] characters: abc.xxxxxxx.yyyyyyy */
    fun randomPackage(length: Int): String {
        val rnd = SecureRandom()
        val letters = "abcdefghijklmnopqrstuvwxyz"
        fun word(n: Int) = buildString { repeat(n) { append(letters[rnd.nextInt(letters.length)]) } }
        val rest = length - 5
        val a = rest / 2
        val b = rest - a
        return word(3) + "." + word(a) + "." + word(b)
    }

    suspend fun hide(context: Context, password: String, onStep: (String) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val oldPkg = context.packageName
                require(oldPkg.length >= 9) { "Package name too short to randomize" }
                val newPkg = randomPackage(oldPkg.length)
                val kf = keyFile(context)
                require(kf.exists()) { "No signing key imported" }

                onStep("Loading signing key")
                val pass = password.toCharArray()
                val ks = KeyStore.getInstance("PKCS12")
                FileInputStream(kf).use { ks.load(it, pass) }
                val alias = ks.aliases().toList().firstOrNull { ks.isKeyEntry(it) }
                    ?: error("No private key found in this file")
                val key = ks.getKey(alias, pass) as PrivateKey
                val chain = ks.getCertificateChain(alias).map { it as X509Certificate }

                val work = File(context.cacheDir, "nova_hide")
                work.deleteRecursively()
                work.mkdirs()
                val patched = File(work, "patched.apk")
                val signed = File(work, "hidden.apk")

                onStep("Changing package name")
                patchApk(File(context.applicationInfo.sourceDir), patched, oldPkg, newPkg)

                onStep("Signing with your key")
                val signer = ApkSigner.SignerConfig.Builder("nova", key, chain).build()
                ApkSigner.Builder(listOf(signer))
                    .setInputApk(patched)
                    .setOutputApk(signed)
                    .setV1SigningEnabled(false)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
                    .setMinSdkVersion(26)
                    .build()
                    .sign()
                patched.delete()

                onStep("Installing")
                val r = Shell.cmd("pm install -r '${signed.absolutePath}'").exec()
                val out = (r.out + r.err).joinToString(" ")
                signed.delete()
                work.deleteRecursively()
                if (!r.isSuccess || !out.contains("Success")) error("Install failed: $out")
                newPkg
            }
        }

    suspend fun launch(pkg: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("monkey -p $pkg -c android.intent.category.LAUNCHER 1").exec().isSuccess
    }

    suspend fun uninstall(pkg: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("pm uninstall $pkg").exec().isSuccess
    }

    private class CountingOut(private val out: OutputStream) : OutputStream() {
        var count = 0L
        override fun write(b: Int) { out.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
        override fun flush() = out.flush()
        override fun close() = out.close()
    }

    /** Same-length replace, so binary XML string pools stay valid. */
    private fun replaceAll(data: ByteArray, old: ByteArray, new: ByteArray): ByteArray {
        if (old.isEmpty() || old.size != new.size) return data
        val res = data.copyOf()
        var i = 0
        while (i <= res.size - old.size) {
            var match = true
            for (j in old.indices) {
                if (res[i + j] != old[j]) { match = false; break }
            }
            if (match) {
                System.arraycopy(new, 0, res, i, new.size)
                i += old.size
            } else {
                i++
            }
        }
        return res
    }

    /** Rewrites the APK with the package name replaced in the manifest and XML resources, keeping native libs aligned. */
    private fun patchApk(src: File, dst: File, oldPkg: String, newPkg: String) {
        val o8 = oldPkg.toByteArray(Charsets.UTF_8)
        val n8 = newPkg.toByteArray(Charsets.UTF_8)
        val o16 = oldPkg.toByteArray(Charsets.UTF_16LE)
        val n16 = newPkg.toByteArray(Charsets.UTF_16LE)
        ZipFile(src).use { zf ->
            val counter = CountingOut(BufferedOutputStream(FileOutputStream(dst)))
            ZipOutputStream(counter).use { zos ->
                for (e in zf.entries().asSequence()) {
                    val name = e.name
                    if (e.isDirectory) continue
                    if (name.startsWith("META-INF/") &&
                        (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") ||
                            name.endsWith(".EC") || name.endsWith("MANIFEST.MF"))
                    ) continue

                    var bytes = zf.getInputStream(e).use { it.readBytes() }
                    if (name == "AndroidManifest.xml" || (name.startsWith("res/") && name.endsWith(".xml"))) {
                        bytes = replaceAll(replaceAll(bytes, o8, n8), o16, n16)
                    }

                    val ne = ZipEntry(name)
                    if (e.method == ZipEntry.STORED) {
                        val crc = CRC32()
                        crc.update(bytes)
                        ne.method = ZipEntry.STORED
                        ne.size = bytes.size.toLong()
                        ne.compressedSize = bytes.size.toLong()
                        ne.crc = crc.value
                        val align = if (name.endsWith(".so")) 4096 else 4
                        val nameLen = name.toByteArray(Charsets.UTF_8).size
                        val dataOff = counter.count + 30 + nameLen
                        var pad = ((align - (dataOff % align)) % align).toInt()
                        if (pad in 1..3) pad += align
                        if (pad > 0) {
                            val extra = ByteArray(pad)
                            extra[0] = 0x35
                            extra[1] = 0xD9.toByte()
                            extra[2] = ((pad - 4) and 0xFF).toByte()
                            extra[3] = (((pad - 4) shr 8) and 0xFF).toByte()
                            ne.extra = extra
                        }
                    } else {
                        ne.method = ZipEntry.DEFLATED
                    }
                    zos.putNextEntry(ne)
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        }
    }
}
