package com.rifsxd.ksunext.ui.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.rifsxd.ksunext.Natives
import java.io.File
import java.security.MessageDigest
import java.util.Calendar
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------------------
// Module script scan
// ---------------------------------------------------------------------------

data class ScanFinding(val file: String, val line: Int, val text: String, val reason: String)

object ModuleScanner {
    private val rules: List<Pair<Regex, String>> = listOf(
        Regex("""\brm\s+(-\w+\s+)+/(data(?!/adb|/local/tmp)|sdcard|storage|system|vendor|mnt|\*)""") to "Deletes system or personal data",
        Regex("""\bdd\s+[^\n]*of=/dev/block""") to "Writes straight to a storage partition",
        Regex("""\b(mkfs(\.\w+)?|make_ext4fs)\b""") to "Formats storage",
        Regex("""/dev/block/[^\s]*(boot|vbmeta|recovery|super|userdata|efs|modem)""") to "Touches a critical partition",
        Regex("""setenforce\s+0""") to "Turns SELinux off",
        Regex("""(curl|wget)[^\n|]*\|\s*(sh|bash|su)\b""") to "Runs code downloaded from the internet",
        Regex("""base64\s+(-d|--decode)[^\n]*\|\s*(sh|bash)""") to "Runs hidden (encoded) code",
        Regex("""chmod\s+(-R\s+)?777\s+/(system|vendor|data|sdcard)""") to "Opens permissions on system folders",
        Regex("""\breboot\b[^\n]*(bootloader|download|recovery|edl)""") to "Reboots into bootloader or recovery",
        Regex("""\bnc\s+(-\w+\s+)*-l|/dev/tcp/""") to "Opens a network connection"
    )

    fun scan(context: Context, uris: List<Uri>): List<ScanFinding> {
        val out = mutableListOf<ScanFinding>()
        for (uri in uris) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    ZipInputStream(ins.buffered()).use { zis ->
                        var e = zis.nextEntry
                        while (e != null && out.size < 12) {
                            val n = e.name
                            if (!e.isDirectory && (n.endsWith(".sh") || n.endsWith("update-binary") || n.endsWith("updater-script"))) {
                                val lines = String(zis.readBytes(), Charsets.UTF_8).split("\n")
                                for ((i, raw) in lines.withIndex()) {
                                    val line = raw.trim()
                                    if (line.isEmpty() || line.startsWith("#")) continue
                                    for ((rx, why) in rules) {
                                        if (rx.containsMatchIn(line) && out.size < 12) {
                                            out.add(ScanFinding(n, i + 1, line.take(140), why))
                                            break
                                        }
                                    }
                                }
                            }
                            e = zis.nextEntry
                        }
                    }
                }
            }
        }
        return out
    }
}

// ---------------------------------------------------------------------------
// Self check
// ---------------------------------------------------------------------------

data class CheckResult(val title: String, val ok: Boolean, val detail: String)

object SelfCheck {
    private fun prop(key: String): String = runCatching {
        Runtime.getRuntime().exec(arrayOf("getprop", key)).inputStream.bufferedReader().readText().trim()
    }.getOrDefault("")

    fun run(context: Context): List<CheckResult> {
        val r = mutableListOf<CheckResult>()
        r.add(CheckResult("RootX recognized by kernel", Natives.isManager, if (Natives.isManager) "Kernel accepts this manager" else "Kernel does not recognize this app"))

        val se = runCatching { File("/sys/fs/selinux/enforce").readText().trim() }.getOrDefault("?")
        r.add(CheckResult("SELinux enforcing", se == "1", when (se) { "1" -> "Enforcing"; "0" -> "Permissive"; else -> "Could not read" }))

        val suPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su", "/su/bin/su")
        val foundSu = suPaths.filter { File(it).exists() }
        r.add(CheckResult("No su binary in common paths", foundSu.isEmpty(), if (foundSu.isEmpty()) "None found" else foundSu.joinToString()))

        val magiskPaths = listOf("/sbin/.magisk", "/data/adb/magisk", "/data/adb/magisk.db", "/cache/magisk.log")
        val foundMagisk = magiskPaths.filter { File(it).exists() }
        r.add(CheckResult("No Magisk traces", foundMagisk.isEmpty(), if (foundMagisk.isEmpty()) "None found" else foundMagisk.joinToString()))

        val vbs = prop("ro.boot.verifiedbootstate")
        r.add(CheckResult("Verified boot state", vbs == "green", if (vbs.isEmpty()) "Unknown" else vbs))

        val tags = android.os.Build.TAGS ?: ""
        r.add(CheckResult("Release-keys build", !tags.contains("test-keys"), tags.ifEmpty { "Unknown" }))

        val dbg = prop("ro.debuggable")
        r.add(CheckResult("Not a debuggable build", dbg != "1", if (dbg.isEmpty()) "Unknown" else "ro.debuggable=$dbg"))

        val mounts = runCatching { File("/proc/self/mounts").readLines() }.getOrDefault(emptyList())
        val badMounts = mounts.filter { l -> val x = l.lowercase(); x.contains("ksu") || x.contains("magisk") || x.contains("kernelsu") }
        r.add(CheckResult("No suspicious mounts", badMounts.isEmpty(), if (badMounts.isEmpty()) "Clean" else "${badMounts.size} suspicious line(s)"))

        val adb = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) }.getOrDefault(0)
        r.add(CheckResult("USB debugging off", adb == 0, if (adb == 0) "Off" else "On"))
        return r
    }
}

// ---------------------------------------------------------------------------
// Module presets and snapshot
// ---------------------------------------------------------------------------

object RootXPresets {
    private fun prefs(c: Context) = c.getSharedPreferences("rootx_presets", Context.MODE_PRIVATE)

    fun load(c: Context): Map<String, List<String>> {
        val out = linkedMapOf<String, List<String>>()
        runCatching {
            val o = JSONObject(prefs(c).getString("presets", "{}") ?: "{}")
            for (k in o.keys()) {
                val a = o.getJSONArray(k)
                out[k] = (0 until a.length()).map { a.getString(it) }
            }
        }
        return out
    }

    fun save(c: Context, name: String, ids: List<String>) {
        val o = JSONObject()
        val cur = load(c).toMutableMap()
        cur[name] = ids
        for ((k, v) in cur) o.put(k, JSONArray(v))
        prefs(c).edit().putString("presets", o.toString()).apply()
    }

    fun delete(c: Context, name: String) {
        val cur = load(c).toMutableMap()
        cur.remove(name)
        val o = JSONObject()
        for ((k, v) in cur) o.put(k, JSONArray(v))
        prefs(c).edit().putString("presets", o.toString()).apply()
    }

    fun saveSnapshot(c: Context, ids: List<String>) {
        prefs(c).edit().putString("snapshot", JSONArray(ids).toString()).apply()
    }

    fun loadSnapshot(c: Context): List<String>? = runCatching {
        val s = prefs(c).getString("snapshot", null) ?: return null
        val a = JSONArray(s)
        (0 until a.length()).map { a.getString(it) }
    }.getOrNull()
}

// ---------------------------------------------------------------------------
// PIN lock
// ---------------------------------------------------------------------------

object RootXPin {
    private fun prefs(c: Context) = c.getSharedPreferences("rootx_security", Context.MODE_PRIVATE)
    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(("rootx:" + pin).toByteArray()).joinToString("") { "%02x".format(it) }

    fun has(c: Context): Boolean = !prefs(c).getString("pin", null).isNullOrEmpty()
    fun length(c: Context): Int = prefs(c).getInt("pin_len", 4)
    fun set(c: Context, pin: String) { prefs(c).edit().putString("pin", hash(pin)).putInt("pin_len", pin.length).apply() }
    fun clear(c: Context) { prefs(c).edit().remove("pin").remove("pin_len").apply() }
    fun check(c: Context, pin: String): Boolean = prefs(c).getString("pin", null) == hash(pin)
}

// ---------------------------------------------------------------------------
// Launcher icon hide + dial code
// ---------------------------------------------------------------------------

object RootXIcon {
    private fun cn(c: Context) = ComponentName(c.packageName, "com.rifsxd.ksunext.LauncherAlias")

    fun isHidden(c: Context): Boolean = runCatching {
        c.packageManager.getComponentEnabledSetting(cn(c)) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }.getOrDefault(false)

    fun setHidden(c: Context, hidden: Boolean) {
        runCatching {
            c.packageManager.setComponentEnabledSetting(
                cn(c),
                if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val launch = Intent(context, com.rifsxd.ksunext.ui.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
    }
}

// ---------------------------------------------------------------------------
// Temporary and scheduled root
// ---------------------------------------------------------------------------

data class TempGrant(val pkg: String, val uid: Int, val until: Long)
data class RootWindow(val pkg: String, val uid: Int, val startHour: Int, val endHour: Int)

object RootTimer {
    const val ACTION_EXPIRE = "rootx.ROOT_EXPIRE"
    const val ACTION_WIN_ON = "rootx.WINDOW_ON"
    const val ACTION_WIN_OFF = "rootx.WINDOW_OFF"

    private fun prefs(c: Context) = c.getSharedPreferences("rootx_timer", Context.MODE_PRIVATE)

    fun setRoot(pkg: String, uid: Int, allow: Boolean): Boolean = runCatching {
        val cur = Natives.getAppProfile(pkg, uid)
        Natives.setAppProfile(cur.copy(name = pkg, currentUid = uid, allowSu = allow))
    }.getOrDefault(false)

    fun listTemps(c: Context): List<TempGrant> = runCatching {
        val a = JSONArray(prefs(c).getString("temps", "[]") ?: "[]")
        (0 until a.length()).map { val o = a.getJSONObject(it); TempGrant(o.getString("pkg"), o.getInt("uid"), o.getLong("until")) }
    }.getOrDefault(emptyList())

    private fun saveTemps(c: Context, l: List<TempGrant>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("pkg", it.pkg).put("uid", it.uid).put("until", it.until)) }
        prefs(c).edit().putString("temps", a.toString()).apply()
    }

    fun listWindows(c: Context): List<RootWindow> = runCatching {
        val a = JSONArray(prefs(c).getString("windows", "[]") ?: "[]")
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            RootWindow(o.getString("pkg"), o.getInt("uid"), o.getInt("start"), o.getInt("end"))
        }
    }.getOrDefault(emptyList())

    private fun saveWindows(c: Context, l: List<RootWindow>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("pkg", it.pkg).put("uid", it.uid).put("start", it.startHour).put("end", it.endHour)) }
        prefs(c).edit().putString("windows", a.toString()).apply()
    }

    private fun pending(c: Context, action: String, pkg: String): PendingIntent {
        val i = Intent(c, RootTimerReceiver::class.java).setAction(action).putExtra("pkg", pkg)
        return PendingIntent.getBroadcast(c, (action + pkg).hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun alarm(c: Context, action: String, pkg: String, at: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(c, action, pkg))
    }

    private fun cancel(c: Context, action: String, pkg: String) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(c, action, pkg))
    }

    private fun nextAt(hour: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    fun inWindow(startHour: Int, endHour: Int): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) h in startHour until endHour else (h >= startHour || h < endHour)
    }

    fun minutesUntilMidnight(): Int {
        val cal = Calendar.getInstance()
        return ((24 - cal.get(Calendar.HOUR_OF_DAY)) * 60 - cal.get(Calendar.MINUTE)).coerceAtLeast(1)
    }

    fun grantTemporary(c: Context, pkg: String, uid: Int, minutes: Int): Boolean {
        if (!setRoot(pkg, uid, true)) return false
        val until = System.currentTimeMillis() + minutes * 60_000L
        saveTemps(c, listTemps(c).filter { it.pkg != pkg } + TempGrant(pkg, uid, until))
        alarm(c, ACTION_EXPIRE, pkg, until)
        return true
    }

    fun revokeNow(c: Context, pkg: String) {
        val t = listTemps(c).firstOrNull { it.pkg == pkg }
        if (t != null) setRoot(pkg, t.uid, false)
        saveTemps(c, listTemps(c).filter { it.pkg != pkg })
        cancel(c, ACTION_EXPIRE, pkg)
    }

    fun onExpire(c: Context, pkg: String) {
        val t = listTemps(c).firstOrNull { it.pkg == pkg } ?: return
        setRoot(pkg, t.uid, false)
        saveTemps(c, listTemps(c).filter { it.pkg != pkg })
    }

    fun addWindow(c: Context, pkg: String, uid: Int, startHour: Int, endHour: Int) {
        saveWindows(c, listWindows(c).filter { it.pkg != pkg } + RootWindow(pkg, uid, startHour, endHour))
        setRoot(pkg, uid, inWindow(startHour, endHour))
        alarm(c, ACTION_WIN_ON, pkg, nextAt(startHour))
        alarm(c, ACTION_WIN_OFF, pkg, nextAt(endHour))
    }

    fun removeWindow(c: Context, pkg: String) {
        val w = listWindows(c).firstOrNull { it.pkg == pkg }
        if (w != null) setRoot(pkg, w.uid, false)
        saveWindows(c, listWindows(c).filter { it.pkg != pkg })
        cancel(c, ACTION_WIN_ON, pkg)
        cancel(c, ACTION_WIN_OFF, pkg)
    }

    fun onWindow(c: Context, pkg: String, on: Boolean) {
        val w = listWindows(c).firstOrNull { it.pkg == pkg } ?: return
        setRoot(pkg, w.uid, on)
        alarm(c, if (on) ACTION_WIN_ON else ACTION_WIN_OFF, pkg, nextAt(if (on) w.startHour else w.endHour))
    }

    fun reschedule(c: Context) {
        val now = System.currentTimeMillis()
        for (t in listTemps(c)) {
            if (t.until <= now) onExpire(c, t.pkg) else alarm(c, ACTION_EXPIRE, t.pkg, t.until)
        }
        for (w in listWindows(c)) {
            setRoot(w.pkg, w.uid, inWindow(w.startHour, w.endHour))
            alarm(c, ACTION_WIN_ON, w.pkg, nextAt(w.startHour))
            alarm(c, ACTION_WIN_OFF, w.pkg, nextAt(w.endHour))
        }
    }
}

class RootTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra("pkg") ?: return
        when (intent.action) {
            RootTimer.ACTION_EXPIRE -> RootTimer.onExpire(context, pkg)
            RootTimer.ACTION_WIN_ON -> RootTimer.onWindow(context, pkg, true)
            RootTimer.ACTION_WIN_OFF -> RootTimer.onWindow(context, pkg, false)
        }
    }
}

class RootXBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) RootTimer.reschedule(context)
    }
}
