package com.swox.share

import android.app.Application
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ConnectionStatus {
    IDLE,
    STARTING_HOTSPOT,
    WAITING_FOR_RECEIVER,
    CONNECTING_WIFI,
    CONNECTED,
    TRANSFERRING,
    COMPLETE,
    ERROR
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = AppPreferences(application)
    val transferEngine = TransferEngine(application)
    private val hotspotHelper = HotspotHelper(application)
    private val wifiConnectHelper = WifiConnectHelper(application)
    private var startSignal: CompletableDeferred<Unit>? = null

    var receiverConnected by mutableStateOf(false)
        private set

    var transferStarted by mutableStateOf(false)
        private set

    var transferPaused by mutableStateOf(false)
        private set

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    var language by mutableStateOf(prefs.language)
        private set

    var selectedFileUri by mutableStateOf<Uri?>(null)
        private set

    var selectedFileName by mutableStateOf<String?>(null)
        private set

    var selectedFileSize by mutableStateOf(0L)
        private set

    var fileValidationMessage by mutableStateOf(context.getString(R.string.not_validated))
        private set

    var transferProgress by mutableStateOf(TransferProgress())
        private set

    var localIp by mutableStateOf(NetworkUtils.getLocalIpAddress())
        private set

    var wifiSsid by mutableStateOf(NetworkUtils.getWifiSsid(application))
        private set

    var isNetworkAvailable by mutableStateOf(NetworkUtils.isNetworkAvailable(application))
        private set

    var hasRequiredPermissions by mutableStateOf(PermissionUtils.hasAllRequiredPermissions(context))
        private set

    var scannedDevices = mutableStateListOf<String>()
        private set

    var isScanning by mutableStateOf(false)
        private set

    var hotspotName by mutableStateOf("")
    var hotspotPassword by mutableStateOf("")

    var qrContent by mutableStateOf("")
        private set

    var connectionPayload by mutableStateOf<NetworkUtils.ConnectionPayload?>(null)
        private set

    var manualConnectIp by mutableStateOf("")
    var scannedQrData by mutableStateOf("")

    var connectionTargetIp by mutableStateOf("")
        private set

    var connectionStatus by mutableStateOf(ConnectionStatus.IDLE)
        private set

    var statusMessage by mutableStateOf("")
        private set

    var isSenderMode by mutableStateOf(false)
        private set

    var receivedFile by mutableStateOf<File?>(null)
        private set

    var transferHistory = mutableStateListOf<String>()
        private set

    var securityLogs = mutableStateListOf<String>()
        private set

    var transferLogs = mutableStateListOf<String>()
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    var isTransferring by mutableStateOf(false)
        private set

    var sessionId by mutableStateOf("")
        private set

    val deviceName: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    init {
        transferEngine.setProgressCallback { progress ->
            viewModelScope.launch(Dispatchers.Main) {
                transferProgress = progress
            }
        }

        refreshHistory()
        refreshNetworkInfo()
        refreshPermissionsStatus()
    }

    fun refreshNetworkInfo() {
        localIp = NetworkUtils.getLocalIpAddress()
        wifiSsid = NetworkUtils.getWifiSsid(getApplication())
        isNetworkAvailable = NetworkUtils.isNetworkAvailable(getApplication())
    }

    /**
     * Re-checks whether all permissions from [PermissionUtils.requiredPermissions]
     * are currently granted. Called at startup; safe to call again from the UI
     * layer if the app ever needs to re-evaluate permission state.
     */
    fun refreshPermissionsStatus() {
        hasRequiredPermissions = PermissionUtils.hasAllRequiredPermissions(context)
    }

    fun setTheme(mode: String) {
        themeMode = mode
        prefs.themeMode = mode
        logSecurity("Tema dəyişdirildi: $mode")
    }

    fun changeLanguage(lang: String) {
        language = lang
        prefs.language = lang
        logSecurity("Dil dəyişdirildi: $lang")
    }

    fun setSelectedFile(uri: Uri?, name: String?, size: Long) {
        selectedFileUri = uri
        selectedFileName = name
        selectedFileSize = size
        validateFile()
    }

    fun validateFile() {
        fileValidationMessage = when {
            selectedFileUri == null -> context.getString(R.string.file_not_selected)
            selectedFileSize <= 0 -> context.getString(R.string.file_size_not_read)
            else -> context.getString(R.string.file_valid)
        }
    }

    fun prepareSendSession(
        onReady: () -> Unit,
        onTransferStart: () -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        val uri = selectedFileUri

        if (uri == null) {
            lastError = context.getString(R.string.file_not_selected)
            connectionStatus = ConnectionStatus.ERROR
            onComplete(false)
            return
        }

        isSenderMode = true
        isTransferring = true
        receiverConnected = false
        transferStarted = false
        transferPaused = false
        connectionStatus = ConnectionStatus.STARTING_HOTSPOT
        statusMessage = getApplication<Application>().getString(R.string.hotspot_starting)
        sessionId = UUID.randomUUID().toString().take(8)

        viewModelScope.launch {
            try {

                val hotspotResult =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        withContext(Dispatchers.Main) {

                            suspendCancellableCoroutine<Result<NetworkUtils.HotspotInfo>> { cont ->

                                Log.d("SWOX_HOTSPOT", "startHotspot çağırıldı")

                                hotspotHelper.startHotspot { result ->

                                    Log.d("SWOX_HOTSPOT", "Hotspot result gəldi: $result")

                                    if (cont.isActive) {
                                        cont.resume(result)
                                    }

                                }
                            }
                        }

                    } else {
                        Result.failure(
                            UnsupportedOperationException(getApplication<Application>().getString(R.string.hotspot_ip_required))
                        )
                    }

                val hotspotInfo = hotspotResult.getOrElse {

                    lastError = it.message ?: getApplication<Application>().getString(R.string.hotspot_failed_reason)
                    connectionStatus = ConnectionStatus.ERROR
                    isTransferring = false
                    onComplete(false)
                    return@launch

                }

                hotspotName = hotspotInfo.ssid
                hotspotPassword = hotspotInfo.password

                statusMessage = getApplication<Application>().getString(R.string.hotspot_ready_taking_ip)

                delay(2500)

                try {

                    var ip = ""

                    for (i in 0 until 10) {

                        ip = NetworkUtils.getLocalIpAddress(preferHotspot = true)

                        Log.d("SWOX_IP", "Attempt $i -> '$ip'")

                        if (ip.isNotBlank()) {
                            localIp = ip
                            break
                        }

                        delay(500)
                    }

                    if (ip.isBlank()) {

                        lastError = getApplication<Application>().getString(R.string.hotspot_ip_not_found)
                        connectionStatus = ConnectionStatus.ERROR
                        isTransferring = false
                        onComplete(false)
                        return@launch

                    }

                } catch (e: Exception) {

                    lastError = getApplication<Application>().getString(R.string.network_info_not_found)
                    connectionStatus = ConnectionStatus.ERROR
                    isTransferring = false
                    onComplete(false)
                    return@launch

                }

                statusMessage = getApplication<Application>().getString(R.string.qr_preparing)

                val payload = NetworkUtils.ConnectionPayload(
                    hotspotName = hotspotInfo.ssid,
                    password = hotspotInfo.password,
                    ip = localIp,
                    port = NetworkUtils.TRANSFER_PORT,
                    sessionId = sessionId,
                    deviceName = deviceName
                )

                Log.d("SWOX_IP", "FINAL sender IP = $localIp")

                connectionPayload = payload
                qrContent = NetworkUtils.buildConnectionJson(payload)

                statusMessage = getApplication<Application>().getString(R.string.status_waiting_device)
                connectionStatus = ConnectionStatus.WAITING_FOR_RECEIVER

                onReady()

				startSignal = CompletableDeferred()

				launch {

					try {

						transferEngine.startSenderServer(
							localDeviceName = deviceName,

							onClientConnected = {
								viewModelScope.launch(Dispatchers.Main) {
									receiverConnected = true
									connectionStatus = ConnectionStatus.CONNECTED
									statusMessage = getApplication<Application>().getString(R.string.connecting_short)
								}

								Log.d("SWOX", "Client qoşuldu -> startSignal.complete()")
								startSignal?.complete(Unit)
							},

							onReady = { }
						)

					} catch (e: Exception) {

						if (startSignal?.isCompleted == false) {
							startSignal?.completeExceptionally(e)
						}

					}

				}

				Log.d("SWOX", "Client gözlənilir...")
				startSignal?.await()

				Log.d("SWOX", "Transfer başlayır!")

				connectionStatus = ConnectionStatus.TRANSFERRING
				statusMessage = getApplication<Application>().getString(R.string.file_sending)

                transferEngine.sendFileAfterConnect(uri, deviceName) { success ->

                    isTransferring = false

                    connectionStatus =
                        if (success)
                            ConnectionStatus.COMPLETE
                        else
                            ConnectionStatus.ERROR

                    if (success) {

                        addHistory("Göndərildi: ${selectedFileName ?: "fayl"}")
                        logTransfer("Fayl göndərildi: ${selectedFileName}")

                    } else {

                        lastError =
                            transferProgress.error ?: context.getString(R.string.send_failed)

                    }

                    viewModelScope.launch(Dispatchers.Main) {onComplete(success)}

                }   // sendFileAfterConnect callback bitir

                } catch (e: Exception) {

                    if (connectionStatus != ConnectionStatus.ERROR) {

                        lastError = e.message ?: context.getString(R.string.unknown_error)
                        connectionStatus = ConnectionStatus.ERROR
                        isTransferring = false
                        onComplete(false)

                    }

            } catch (e: Exception) {

                Log.e("SWOX_HOTSPOT", "prepareSendSession Exception", e)

                lastError = e.message ?: context.getString(R.string.unknown_error)
                connectionStatus = ConnectionStatus.ERROR
                isTransferring = false
                onComplete(false)

            }
        }
    }

    fun connectAndReceive(
        payload: NetworkUtils.ConnectionPayload,
        onConnected: () -> Unit,
        onTransferStart: () -> Unit,
        onComplete: (File?) -> Unit
    ) {
        isSenderMode = false
        isTransferring = true
        connectionPayload = payload
        connectionTargetIp = payload.ip
        connectionStatus = ConnectionStatus.CONNECTING_WIFI
        statusMessage = getApplication<Application>().getString(R.string.hotspot_connecting)

        viewModelScope.launch {
            try {
                val wifiResult = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Result<android.net.Network>> { cont ->
                        wifiConnectHelper.connectToHotspot(payload.hotspotName, payload.password) { result ->
                            if (cont.isActive) cont.resume(result)
                        }
                    }
                }

                wifiResult.getOrElse {
                    lastError = it.message ?: getApplication<Application>().getString(R.string.wifi_connect_failed)
                    connectionStatus = ConnectionStatus.ERROR
                    isTransferring = false
                    onComplete(null)
                    return@launch
                }

                delay(3000)

                connectionStatus = ConnectionStatus.CONNECTED
                statusMessage = getApplication<Application>().getString(R.string.connected_waiting_file)
                onConnected()
                onTransferStart()

                connectionStatus = ConnectionStatus.TRANSFERRING

                transferEngine.receiveFromSender(payload.ip) { file ->
                    viewModelScope.launch(Dispatchers.Main) {

                        isTransferring = false
                        receivedFile = file

                        connectionStatus =
                            if (file != null) ConnectionStatus.COMPLETE
                            else ConnectionStatus.ERROR

                        if (file != null) {
                            addHistory("Qəbul: ${file.name}")
                            logTransfer("Fayl qəbul edildi: ${file.name}")
                        } else {
                            lastError = transferProgress.error ?: context.getString(R.string.receive_failed)
                        }

                        wifiConnectHelper.disconnect()
                        onComplete(file)
                    }
                }

            } catch (e: Exception) {
                lastError = e.message
                connectionStatus = ConnectionStatus.ERROR
                isTransferring = false
                wifiConnectHelper.disconnect()
                onComplete(null)
            }
        }
    }

    fun connectManual(
        ssid: String,
        password: String,
        onConnected: () -> Unit = {},
        onTransferStart: () -> Unit = {},
        onComplete: (File?) -> Unit = {}
    ) {
        if (ssid.isBlank() || password.isBlank()) {
            lastError = getApplication<Application>().getString(R.string.ssid_password_empty)
            connectionStatus = ConnectionStatus.ERROR
            return
        }

        isSenderMode = false
        isTransferring = true

        connectionStatus = ConnectionStatus.CONNECTING_WIFI
        statusMessage = getApplication<Application>().getString(R.string.hotspot_connecting)

        val payload = NetworkUtils.ConnectionPayload(
            hotspotName = ssid,
            password = password,
            ip = manualConnectIp.trim(),
            port = NetworkUtils.TRANSFER_PORT,
            sessionId = "",
            deviceName = ""
        )

        connectionPayload = payload
        hotspotName = ssid
        hotspotPassword = password

        viewModelScope.launch {
            try {
                val wifiResult = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Result<android.net.Network>> { cont ->
                        wifiConnectHelper.connectToHotspot(ssid, password) { result ->
                            if (cont.isActive) {
                                cont.resume(result)
                            }
                        }
                    }
                }

                wifiResult.getOrElse {
                    lastError = it.message ?: getApplication<Application>().getString(R.string.wifi_not_connected)
                    connectionStatus = ConnectionStatus.ERROR
                    isTransferring = false
                    onComplete(null)
                    return@launch
                }

                delay(1000)

                connectionStatus = ConnectionStatus.CONNECTED
                statusMessage = getApplication<Application>().getString(R.string.connected_waiting_file)

                onConnected()
                onTransferStart()

                connectionStatus = ConnectionStatus.TRANSFERRING

                transferEngine.receiveFromSender(payload.ip) { file ->
                    isTransferring = false
                    receivedFile = file

                    connectionStatus =
                        if (file != null) ConnectionStatus.COMPLETE else ConnectionStatus.ERROR

                    if (file != null) {
                        addHistory("Qəbul: ${file.name}")
                        logTransfer("Fayl qəbul edildi: ${file.name}")
                    } else {
                        lastError = transferProgress.error ?: context.getString(R.string.receive_failed)
                    }

                    wifiConnectHelper.disconnect()

                    onComplete(file)
                }

            } catch (e: Exception) {
                lastError = e.message ?: context.getString(R.string.unknown_error)
                connectionStatus = ConnectionStatus.ERROR
                isTransferring = false

                wifiConnectHelper.disconnect()

                onComplete(null)
            }
        }
    }

    fun parseQrData(data: String): Boolean {
        scannedQrData = data.trim()
        val payload = NetworkUtils.parseConnectionPayload(scannedQrData)
        return if (payload != null && payload.ip.isNotBlank()) {
            connectionPayload = payload
            connectionTargetIp = payload.ip
            hotspotName = payload.hotspotName
            hotspotPassword = payload.password
            logTransfer("QR oxundu: ${payload.deviceName} @ ${payload.ip}")
            true
        } else {
            lastError = getApplication<Application>().getString(R.string.invalid_qr_format)
            false
        }
    }

    fun scanDevices() {
        viewModelScope.launch {

            isScanning = true
            scannedDevices.clear()

            val devices = withContext(Dispatchers.IO) {
                NetworkUtils.scanLocalDevices(localIp)
            }

            scannedDevices.addAll(devices)
            isScanning = false

            logTransfer("Cihaz axtarışı: ${devices.size} cihaz tapıldı")
        }
    }

    fun setConnectionTarget(ip: String) {
        connectionTargetIp = ip
        logTransfer("Hədəf cihaz: $ip")
    }

    fun cancelTransfer() {
        startSignal?.cancel()
        startSignal = null
        receiverConnected = false
        transferStarted = false
        transferPaused = false

        transferEngine.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hotspotHelper.stopHotspot()
        }
        wifiConnectHelper.disconnect()
        isTransferring = false
        connectionStatus = ConnectionStatus.IDLE
        statusMessage = getApplication<Application>().getString(R.string.status_stopped)
        logTransfer("Transfer ləğv edildi")
    }

    fun resetSession() {
        cancelTransfer()
        connectionPayload = null
        qrContent = ""
        sessionId = ""
        receivedFile = null
        lastError = null
        connectionStatus = ConnectionStatus.IDLE
        statusMessage = ""
        isSenderMode = false
    }

    fun refreshHistory() {
        viewModelScope.launch(Dispatchers.Main) {

            transferHistory.clear()
            transferHistory.addAll(
                prefs.getTransferHistory().map { formatLogEntry(it) }
            )

            securityLogs.clear()
            securityLogs.addAll(
                prefs.getSecurityLogs().map { formatLogEntry(it) }
            )

            transferLogs.clear()
            transferLogs.addAll(
                prefs.getTransferLogs().map { formatLogEntry(it) }
            )
        }
    }

    fun clearHistory() {
        prefs.clearHistory()
        refreshHistory()
    }

    fun resetSettings() {
        prefs.resetSettings()
        themeMode = "system"
        language = "system"
        logSecurity("Ayarlar sıfırlandı")
    }

    fun logSecurity(message: String) {
        prefs.addSecurityLog(message)
        refreshHistory()
    }

    fun logTransfer(message: String) {
        prefs.addTransferLog(message)
        refreshHistory()
    }

    private fun addHistory(entry: String) {
        prefs.addTransferHistory(entry)
        refreshHistory()
    }

    private fun formatLogEntry(raw: String): String {
        val parts = raw.split("|", limit = 2)
        if (parts.size == 2) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(parts[0].toLongOrNull() ?: 0L))
            return "$time — ${parts[1]}"
        }
        return raw
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "-"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.getDefault(), "%.2f MB", mb)
            else -> String.format(Locale.getDefault(), "%.1f KB", kb)
        }
    }

    fun getEstimatedTimeRemaining(): String {
        val progress = transferProgress
        if (progress.remainingSeconds >= 0) {
            val min = progress.remainingSeconds / 60
            val sec = progress.remainingSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
        }
        if (progress.percent <= 0) return "--:--"
        val totalSize = progress.fileSize.takeIf { it > 0 } ?: selectedFileSize
        if (totalSize <= 0) return "--:--"
        val remainingBytes = totalSize - progress.bytesTransferred
        val speedBytesPerSec = progress.speedMbps * 1024 * 1024
        if (speedBytesPerSec <= 0) return "--:--"
        val seconds = (remainingBytes / speedBytesPerSec).toInt()
        val min = seconds / 60
        val sec = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    fun getDeviceInfo(): String {
        return "$deviceName (Android ${Build.VERSION.RELEASE})"
    }

    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    override fun onCleared() {
        super.onCleared()
        startSignal?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hotspotHelper.stopHotspot()
        }
        wifiConnectHelper.disconnect()
        transferEngine.stop()
    }

    fun startTransfer() {
        if (transferPaused) {
            transferPaused = false
            connectionStatus = ConnectionStatus.TRANSFERRING
            statusMessage = getApplication<Application>().getString(R.string.file_sending)
            transferEngine.resume()
            return
        }

        if (transferStarted) return

        transferStarted = true
        transferPaused = false
        startSignal?.complete(Unit)
    }

    fun pauseTransfer() {
        if (!transferStarted || transferPaused) return
        transferPaused = true
        statusMessage = getApplication<Application>().getString(R.string.status_stopped)
        transferEngine.pause()
    }

    fun closeTransfer() {
        startSignal?.cancel()
        startSignal = null
        receiverConnected = false
        transferStarted = false
        transferPaused = false

        transferEngine.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hotspotHelper.stopHotspot()
        }
        wifiConnectHelper.disconnect()
        receivedFile?.delete()
        resetSession()
    }
}