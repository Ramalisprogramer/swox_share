package com.swox.share

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralizes the "Nearby Devices" + "Precise Location" permission requirements
 * that gate the whole app at startup.
 *
 * Swox Share connects devices over a local Wi-Fi hotspot (LocalOnlyHotspot /
 * WifiNetworkSpecifier), so:
 *  - ACCESS_FINE_LOCATION is required on every supported Android version,
 *    since creating/joining the hotspot depends on it (see HotspotHelper /
 *    WifiConnectHelper).
 *  - NEARBY_WIFI_DEVICES is required starting Android 13 (API 33), since that
 *    is the dedicated "Nearby Wi-Fi devices" runtime permission Google
 *    introduced for Wi-Fi scanning/connecting on newer versions.
 */
object PermissionUtils {

    /** Required permissions, adapted to the current device's Android version. */
    fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return permissions.toTypedArray()
    }

    /** True only if every permission from [requiredPermissions] is granted. */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
