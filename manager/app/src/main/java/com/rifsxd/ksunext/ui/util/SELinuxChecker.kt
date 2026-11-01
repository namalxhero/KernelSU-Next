package com.rifsxd.ksunext.ui.util

import com.rifsxd.ksunext.R
import com.topjohnwu.superuser.io.SuFile
import java.io.File

private const val SELINUX_ENFORCE = "/sys/fs/selinux/enforce"

private fun fromEnforce(v: Int?): Int? = when (v) {
    1 -> R.string.selinux_status_enforcing
    0 -> R.string.selinux_status_permissive
    else -> null
}

/** Tries several ways to read the SELinux mode so it never shows "Unknown" just because root is unavailable. */
fun getSELinuxStatus(): Int {
    // 1) with root
    fromEnforce(
        runCatching { SuFile(SELINUX_ENFORCE).newInputStream().bufferedReader().use { it.readLine()?.trim()?.toIntOrNull() } }.getOrNull()
    )?.let { return it }
    // 2) plain read
    fromEnforce(runCatching { File(SELINUX_ENFORCE).readText().trim().toIntOrNull() }.getOrNull())?.let { return it }
    // 3) the getenforce tool
    runCatching {
        Runtime.getRuntime().exec(arrayOf("getenforce")).inputStream.bufferedReader().use { it.readText().trim().lowercase() }
    }.getOrNull()?.let {
        when (it) {
            "enforcing" -> return R.string.selinux_status_enforcing
            "permissive" -> return R.string.selinux_status_permissive
            "disabled" -> return R.string.selinux_status_disabled
            else -> {}
        }
    }
    // 4) system API: SELinux on but not readable by this app = enforcing on a production device
    val enabled = runCatching {
        Class.forName("android.os.SELinux").getMethod("isSELinuxEnabled").invoke(null) as Boolean
    }.getOrNull()
    if (enabled == false) return R.string.selinux_status_disabled
    if (enabled == true) return R.string.selinux_status_enforcing
    return if (File("/sys/fs/selinux").exists()) R.string.selinux_status_enforcing else R.string.selinux_status_disabled
}
