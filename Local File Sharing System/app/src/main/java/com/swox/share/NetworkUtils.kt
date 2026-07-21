package com.swox.share

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object NetworkUtils {

    const val TRANSFER_PORT = 8888

    data class ConnectionPayload(
        val hotspotName: String,
        val password: String,
        val ip: String,
        val port: Int,
        val sessionId: String,
        val deviceName: String
    )

    data class HotspotInfo(
        val ssid: String,
        val password: String
    )

    fun getLocalIpAddress(): String {
        return getLocalIpAddress(preferHotspot = true)
    }

    fun getLocalIpAddress(preferHotspot: Boolean): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val candidates = mutableListOf<Pair<String, String>>()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("169.254.")) continue
                        candidates.add(name to ip)
                    }
                }
            }
            if (preferHotspot) {

                candidates.firstOrNull { (name, _) ->

                    name.contains("ap") ||

                    name.contains("softap") ||

                    name.contains("wlan") ||

                    name.contains("wifi") ||

                    name.contains("p2p")

                }?.second?.let {

                    return it

                }

            }
            candidates.firstOrNull()?.second?.let { return it }
        } catch (_: Exception) {
        }
        return ""
    }

    fun getHotspotGatewayIp(context: Context): String? {

        return try {

            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val gateway = wifiManager.dhcpInfo.gateway

            if (gateway == 0) return null

            "%d.%d.%d.%d".format(
                gateway and 0xff,
                gateway shr 8 and 0xff,
                gateway shr 16 and 0xff,
                gateway shr 24 and 0xff
            )

        } catch (e: Exception) {

            null

        }

    }

    fun getWifiSsid(context: Context): String {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val ssid = info.ssid?.replace("\"", "") ?: "-"
            if (ssid == "<unknown ssid>") "-" else ssid
        } catch (_: Exception) {
            "-"
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun buildConnectionJson(payload: ConnectionPayload): String {
        return JSONObject().apply {
            put("hotspotName", payload.hotspotName)
            put("password", payload.password)
            put("ip", payload.ip)
            put("port", payload.port)
            put("sessionId", payload.sessionId)
            put("deviceName", payload.deviceName)
        }.toString()
    }

    fun parseConnectionPayload(data: String): ConnectionPayload? {
        return try {
            val json = when {
                data.trimStart().startsWith("{") -> JSONObject(data.trim())
                data.startsWith("swoxshare://") -> {
                    val cleaned = data.removePrefix("swoxshare://")
                    val parts = cleaned.split(":")
                    if (parts.size != 2) return null
                    JSONObject().apply {
                        put("hotspotName", "")
                        put("password", "")
                        put("ip", parts[0])
                        put("port", parts[1].toInt())
                        put("sessionId", "")
                        put("deviceName", "")
                    }
                }
                else -> return null
            }
            ConnectionPayload(
                hotspotName = json.optString("hotspotName", ""),
                password = json.optString("password", ""),
                ip = json.optString("ip", ""),
                port = json.optInt("port", TRANSFER_PORT),
                sessionId = json.optString("sessionId", ""),
                deviceName = json.optString("deviceName", "")
            ).takeIf { it.ip.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    @Deprecated("Use parseConnectionPayload", ReplaceWith("parseConnectionPayload(data)?.let { Pair(it.ip, it.port) }"))
    fun parseConnectionString(data: String): Pair<String, Int>? {
        val payload = parseConnectionPayload(data) ?: return null
        return payload.ip to payload.port
    }

    @Deprecated("Use buildConnectionJson", ReplaceWith("buildConnectionJson(payload)"))
    fun buildConnectionString(ip: String, port: Int = TRANSFER_PORT): String {
        return "swoxshare://$ip:$port"
    }

    fun scanLocalDevices(baseIp: String): List<String> {
        val prefix = baseIp.substringBeforeLast(".")
        val found = mutableListOf<String>()
        for (i in 1..254) {
            val ip = "$prefix.$i"
            if (ip == baseIp) continue
            try {
                val address = Inet4Address.getByName(ip)
                if (address.isReachable(200)) {
                    found.add(ip)
                }
            } catch (_: Exception) {
            }
            if (found.size >= 20) break
        }
        return found
    }
}

class HotspotHelper(private val context: Context) {

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    @SuppressLint("MissingPermission")
    fun startHotspot(
        onResult: (Result<NetworkUtils.HotspotInfo>) -> Unit
    ) {
        Log.d("SWOX_HOTSPOT", "startHotspot() called")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onResult(Result.failure(Exception(context.getString(R.string.hotspot_api_required))))
            return
        }

        val fineLocation =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocation || !coarseLocation) {
            onResult(
                Result.failure(
                    SecurityException(context.getString(R.string.precise_location_required))
                )
            )
            return
        }

        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiManager.startLocalOnlyHotspot(
            object : WifiManager.LocalOnlyHotspotCallback() {

                override fun onStarted(
                    reservation: WifiManager.LocalOnlyHotspotReservation
                ) {
                    this@HotspotHelper.reservation = reservation

                    Log.d("SWOX_HOTSPOT", "HOTSPOT STARTED")

                    try {
                        val info = extractHotspotInfo(reservation)

                        Log.d(
                            "SWOX_HOTSPOT",
                            "SSID=${info.ssid}"
                        )

                        onResult(Result.success(info))

                    } catch (e: Exception) {
                        onResult(Result.failure(e))
                    }
                }

                override fun onFailed(reason: Int) {
                    Log.e(
                        "SWOX_HOTSPOT",
                        "HOTSPOT FAILED: $reason"
                    )

                    onResult(
                        Result.failure(
                            Exception(context.getString(R.string.hotspot_failed_reason, reason))
                        )
                    )
                }

                override fun onStopped() {
                    Log.d(
                        "SWOX_HOTSPOT",
                        "HOTSPOT STOPPED"
                    )

                    reservation = null
                }

            },
            Handler(Looper.getMainLooper())
        )
    }

    private fun extractHotspotInfo(
        res: WifiManager.LocalOnlyHotspotReservation
    ): NetworkUtils.HotspotInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val config = res.softApConfiguration
            NetworkUtils.HotspotInfo(
                ssid = config.ssid ?: "SwoxShare",
                password = config.passphrase ?: ""
            )
        } else {
            @Suppress("DEPRECATION")
            val config = res.wifiConfiguration
            NetworkUtils.HotspotInfo(
                ssid = config?.SSID?.replace("\"", "") ?: "SwoxShare",
                password = config?.preSharedKey?.replace("\"", "") ?: ""
            )
        }
    }

    fun stopHotspot() {
        try {
            reservation?.close()
        } catch (_: Exception) {
        }
        reservation = null
    }
}

class WifiConnectHelper(private val context: Context) {

    private var activeNetwork: Network? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun connectToHotspot(
        ssid: String,
        password: String,
        timeoutMs: Long = 30_000,
        onResult: (Result<Network>) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onResult(Result.failure(UnsupportedOperationException(context.getString(R.string.wifi_connect_android10_required))))
            return
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        disconnect()

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val latch = CountDownLatch(1)
        val resultRef = AtomicReference<Result<Network>>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activeNetwork = network
                cm.bindProcessToNetwork(network)
                resultRef.set(Result.success(network))
                latch.countDown()
            }

            override fun onUnavailable() {
                resultRef.set(Result.failure(Exception(context.getString(R.string.hotspot_connection_rejected))))
                latch.countDown()
            }
        }
        networkCallback = callback

        try {
            cm.requestNetwork(request, callback, timeoutMs.toInt())
            Thread {
                if (!latch.await(timeoutMs + 5000, TimeUnit.MILLISECONDS)) {
                    resultRef.compareAndSet(null, Result.failure(Exception(context.getString(R.string.connection_timeout))))
                    latch.countDown()
                }
                onResult(resultRef.get() ?: Result.failure(Exception(context.getString(R.string.connection_failed))))
            }.start()
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun disconnect() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            try {
                cm.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
        try {
            cm.bindProcessToNetwork(null)
        } catch (_: Exception) {
        }
        activeNetwork = null
    }
}
