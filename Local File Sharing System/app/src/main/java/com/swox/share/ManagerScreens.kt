package com.swox.share

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import java.io.File
import java.security.MessageDigest

// --- QR ---

@Composable fun QRDataEncoder(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.qr_prepare), nav) {
        Text(stringResource(R.string.data) + ": ${vm.qrContent}")
        ActionButton(stringResource(R.string.refresh)) { vm.refreshNetworkInfo() }
        ActionButton(stringResource(R.string.create_qr)) { nav.navigate(ScreenRoutes.QR_GENERATOR) }
    }
}

@Composable fun QRImageCreator(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.qr_code), nav) {
        Text(if (QrUtils.generateQrBitmap(vm.qrContent) != null) stringResource(R.string.qr_created) else stringResource(R.string.error))
        ActionButton(stringResource(R.string.qr_generator)) { nav.navigate(ScreenRoutes.QR_GENERATOR) }
    }
}

@Composable fun QRExporter(nav: NavHostController, vm: AppViewModel) {
    val ctx = LocalContext.current
    ScreenScaffold((R.string.qr_export), nav) {
        Text(stringResource(R.string.copy_qr_data))
        val text = if (QrUtils.isValidConnectionQr(vm.qrContent)) stringResource(R.string.qr_valid) else stringResource(R.string.wrong_format)
    }
}

@Composable fun QRValidator(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.qr_code), nav) {
        Text(if (QrUtils.isValidConnectionQr(vm.qrContent)) stringResource(R.string.qr_valid) else stringResource(R.string.wrong_format))
        InfoRow(stringResource(R.string.data), vm.qrContent)
    }
}

@Composable fun QRDecoder(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.qr_scanner), nav) {
        val parsed = NetworkUtils.parseConnectionString(vm.scannedQrData)
        Text(if (parsed != null) stringResource(R.string.opened, parsed.first, parsed.second) else stringResource(R.string.no_data))
    }
}

@Composable fun QRScannerEngine(nav: NavHostController) {
    ScreenScaffold((R.string.qr_camera_engine), nav) {
        Text(stringResource(R.string.camera_engine_ready))
        Text(stringResource(R.string.camerax_ready))
    }
}

@Composable fun QRPermissionManager(nav: NavHostController) {
    val ctx = LocalContext.current
    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    ScreenScaffold((R.string.qr_camera_permission), nav) {
        Text(if (granted) stringResource(R.string.camera_permission_granted) else stringResource(R.string.camera_permission_required))
        if (!granted) ActionButton(stringResource(R.string.request_permission)) { launcher.launch(Manifest.permission.CAMERA) }
    }
}

@Composable fun QRConnectionHandler(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.qr_connection_manager), nav) {
        InfoRow(stringResource(R.string.target_ip), vm.connectionTargetIp.ifBlank { "-" })
        ActionButton(stringResource(R.string.connect)) { nav.navigate(ScreenRoutes.MANUAL_CONNECT) }
    }
}

// --- Hotspot / WiFi ---

@Composable fun HotspotManager(nav: NavHostController) {
    ScreenScaffold((R.string.hotspot_manager), nav) {
        Text(stringResource(R.string.hotspot_manager))
        ActionButton(stringResource(R.string.create_hotspot)) { nav.navigate(ScreenRoutes.HOTSPOT_CREATOR) }
        ActionButton(stringResource(R.string.hotspot_info)) { nav.navigate(ScreenRoutes.HOTSPOT_INFO) }
    }
}

@Composable fun HotspotCreator(nav: NavHostController) {
    ScreenScaffold((R.string.hotspot_create), nav) {
        Text(stringResource(R.string.open_hotspot_settings))
        Text(stringResource(R.string.hotspot_settings))
    }
}

@Composable fun HotspotPasswordManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.hotspot_password_manager), nav) {
        OutlinedTextField(vm.hotspotPassword, { vm.hotspotPassword = it }, label = { Text(stringResource(R.string.password)) }, modifier = Modifier.fillMaxWidth())
        ActionButton(stringResource(R.string.save)) { nav.popBackStack() }
    }
}

@Composable fun WifiConnector(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.wifi_connect), nav) {
        InfoRow(stringResource(R.string.ssid), vm.wifiSsid)
        InfoRow(stringResource(R.string.ip), vm.localIp)
        ActionButton(stringResource(R.string.refresh)) { vm.refreshNetworkInfo() }
    }
}

@Composable fun WifiScanner(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.wifi_scan), nav) {
        InfoRow(stringResource(R.string.current_wifi), vm.wifiSsid)
        ActionButton(stringResource(R.string.device_search)) { nav.navigate(ScreenRoutes.DEVICE_SCANNER) }
    }
}

// --- Preferences / Settings ---

@Composable fun PreferencesManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.preferences_manager), nav) {
        InfoRow(stringResource(R.string.theme), vm.themeMode)
        InfoRow(stringResource(R.string.language), vm.language)
        ActionButton(stringResource(R.string.theme)) { nav.navigate(ScreenRoutes.THEME) }
        ActionButton(stringResource(R.string.language)) { nav.navigate(ScreenRoutes.LANGUAGE) }
    }
}

@Composable fun ThemeStorage(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.theme_storage), nav) {
        Text(stringResource(R.string.saved_theme, vm.themeMode))
    }
}

@Composable fun LanguageStorageScreen(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.language_storage), nav) {
        Text(stringResource(R.string.saved_language, vm.language))
    }
}

@Composable fun UserSettings(nav: NavHostController) {
    ScreenScaffold((R.string.user_settings), nav) {
        ActionButton(stringResource(R.string.settings)) { nav.navigate(ScreenRoutes.SETTINGS) }
        ActionButton(stringResource(R.string.theme)) { nav.navigate(ScreenRoutes.THEME) }
    }
}

@Composable fun BackupManager(nav: NavHostController) {
    ScreenScaffold((R.string.backup), nav) {
        ActionButton(stringResource(R.string.backup_system)) { nav.navigate(ScreenRoutes.BACKUP_SYSTEM) }
        ActionButton(stringResource(R.string.auto_backup)) { nav.navigate(ScreenRoutes.AUTO_BACKUP) }
    }
}

@Composable fun RestoreManager(nav: NavHostController) {
    ScreenScaffold((R.string.restore), nav) {
        ActionButton(stringResource(R.string.backup_import)) { nav.navigate(ScreenRoutes.BACKUP_IMPORT) }
    }
}

@Composable fun CacheManager(nav: NavHostController) {
    val ctx = LocalContext.current
    var size by remember { mutableStateOf("-") }
    val cacheCleared = stringResource(R.string.cache_cleared)

    LaunchedEffect(Unit) {
        val dir = ctx.cacheDir
        size = formatDirSize(dir)
    }

    ScreenScaffold((R.string.cache_manager), nav) {
        InfoRow(stringResource(R.string.cache_size), size)
        ActionButton(stringResource(R.string.clear_cache)) {
            ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            showToast(ctx, cacheCleared)
        }
    }
}

@Composable fun DatabaseManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.database), nav) {
        InfoRow(stringResource(R.string.transfer_records), vm.transferHistory.size.toString())
        ActionButton(stringResource(R.string.history)) { nav.navigate(ScreenRoutes.TRANSFER_HISTORY) }
    }
}

@Composable fun SettingsValidator(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.settings_check), nav) {
    Text(stringResource(R.string.theme_status, if (vm.themeMode.isNotBlank()) "OK ✓" else stringResource(R.string.error)))
    Text(stringResource(R.string.language_status, if (vm.language.isNotBlank()) "OK ✓" else stringResource(R.string.error)))
    }
}

@Composable
fun SettingsResetManager(nav: NavHostController, vm: AppViewModel) {
    val context = LocalContext.current
    val resetDone = stringResource(R.string.reset_done)

    ScreenScaffold((R.string.reset_settings), nav) {
        Text(stringResource(R.string.all_settings_deleted))

        ActionButton(stringResource(R.string.reset)) {
            vm.resetSettings()
            showToast(context, resetDone)
        }
    }
}


// --- Security ---

@Composable fun SecurityManager(nav: NavHostController) {
    ScreenScaffold((R.string.security_manager), nav) {
        ActionButton(stringResource(R.string.encryption)) { nav.navigate(ScreenRoutes.ENCRYPTION) }
        ActionButton(stringResource(R.string.key_manager)) { nav.navigate(ScreenRoutes.KEY_MANAGER) }
        ActionButton(stringResource(R.string.logs)) { nav.navigate(ScreenRoutes.SECURITY_LOGGER) }
    }
}

@Composable fun EncryptionManager(nav: NavHostController) {
    ScreenScaffold((R.string.encryption_manager), nav) {
        Text(stringResource(R.string.transfer_encryption_preparing))
        Text(stringResource(R.string.aes256_planned))
    }
}

@Composable fun KeyManager(nav: NavHostController) {
    ScreenScaffold((R.string.key_manager), nav) {
        Text(stringResource(R.string.session_keys_local))
    }
}

@Composable fun PasswordManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.password_manager), nav) {
        OutlinedTextField(
            vm.hotspotPassword,
            { vm.hotspotPassword = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable fun SessionManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.session_manager), nav) {
        InfoRow(stringResource(R.string.transfer), if (vm.isTransferring) stringResource(R.string.active) else stringResource(R.string.empty))
        InfoRow(stringResource(R.string.target), vm.connectionTargetIp.ifBlank { "-" })
    }
}

@Composable fun DeviceAuthentication(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.device_authentication), nav) {
        Text(vm.getDeviceInfo())
        Text(stringResource(R.string.device_identification_ready))
    }
}

@Composable fun FileHashChecker(nav: NavHostController, vm: AppViewModel) {
    val ctx = LocalContext.current
    var hash by remember { mutableStateOf("-") }

    LaunchedEffect(vm.selectedFileUri) {
        hash = vm.selectedFileUri?.let { computeUriHash(ctx, it) } ?: "-"
    }

    ScreenScaffold((R.string.file_hash_checker), nav) {
        InfoRow(stringResource(R.string.sha256), hash.take(32) + if (hash.length > 32) "..." else "")
    }
}

@Composable fun IntegrityChecker(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.integrity_checker), nav) {
        Text(vm.fileValidationMessage)
    }
}

@Composable fun SecureTransfer(nav: NavHostController) {
    ScreenScaffold((R.string.secure_transfer), nav) {
        Text(stringResource(R.string.local_tcp_transfer))
        Text(stringResource(R.string.encryption_module_soon))
    }
}

@Composable fun AccessControl(nav: NavHostController) {
    ScreenScaffold((R.string.access_control), nav) {
        Text(stringResource(R.string.local_network_only))
        Text(stringResource(R.string.port_value, NetworkUtils.TRANSFER_PORT))
    }
}

@Composable fun SecurityLogger(nav: NavHostController, vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.refreshHistory() }

    ScreenScaffold((R.string.security_logs), nav) {
        LogList(vm.securityLogs, stringResource(R.string.no_logs))
    }
}

// --- Android System ---

@Composable fun AndroidSystemManager(nav: NavHostController) {
    ScreenScaffold((R.string.android_system_manager), nav) {
        ActionButton(stringResource(R.string.device_info)) { nav.navigate(ScreenRoutes.DEVICE_INFO) }
        ActionButton(stringResource(R.string.android_version)) { nav.navigate(ScreenRoutes.ANDROID_VERSION) }
        ActionButton(stringResource(R.string.permissions)) { nav.navigate(ScreenRoutes.PERMISSION_MANAGER) }
    }
}

@Composable fun PermissionManagerScreen(nav: NavHostController) {
    ScreenScaffold((R.string.permission_manager), nav) {
        ActionButton(stringResource(R.string.camera)) { nav.navigate(ScreenRoutes.CAMERA_PERMISSION) }
        ActionButton(stringResource(R.string.storage)) { nav.navigate(ScreenRoutes.STORAGE_PERMISSION) }
        ActionButton(stringResource(R.string.wifi)) { nav.navigate(ScreenRoutes.WIFI_PERMISSION) }
        ActionButton(stringResource(R.string.bluetooth)) { nav.navigate(ScreenRoutes.BLUETOOTH_PERMISSION) }
        ActionButton(stringResource(R.string.notification)) { nav.navigate(ScreenRoutes.NOTIFICATION_PERMISSION) }
        ActionButton(stringResource(R.string.network)) { nav.navigate(ScreenRoutes.NETWORK_PERMISSION) }
        ActionButton(stringResource(R.string.location)) { nav.navigate(ScreenRoutes.LOCATION_PERMISSION) }
    }
}

@Composable fun CameraPermissionScreen(nav: NavHostController) {
    PermissionScreen(nav, R.string.camera_permission, Manifest.permission.CAMERA)
}

@Composable fun StoragePermissionScreen(nav: NavHostController) {
    val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    PermissionScreen(nav, R.string.storage_permission, perm)
}

@Composable fun WifiPermissionScreen(nav: NavHostController) {
    PermissionScreen(nav, R.string.wifi_permission, Manifest.permission.ACCESS_WIFI_STATE, requestable = false)
}

@Composable fun BluetoothPermissionScreen(nav: NavHostController) {
    val perm = if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
    PermissionScreen(nav, R.string.bluetooth_permission, perm)
}

@Composable fun NotificationPermissionScreen(nav: NavHostController) {
    if (Build.VERSION.SDK_INT >= 33) {
        PermissionScreen(
            nav,
            R.string.notification_permission,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        ScreenScaffold(R.string.notification_permission, nav) {
            Text(stringResource(R.string.android_old_notification))
        }
    }
}

@Composable fun NetworkPermissionScreen(nav: NavHostController) {
    PermissionScreen(nav, R.string.network_permission, Manifest.permission.INTERNET, requestable = false)
}

@Composable fun LocationPermissionScreen(nav: NavHostController) {
    PermissionScreen(nav, R.string.location_permission, Manifest.permission.ACCESS_FINE_LOCATION)
}

@Composable fun BatteryManagerScreen(nav: NavHostController) {
    ScreenScaffold((R.string.battery_manager), nav) {
        Text(stringResource(R.string.keep_screen_active))
        Text(stringResource(R.string.battery_normal))
    }
}

@Composable fun DeviceInfoManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.device_info), nav) {
        Text(vm.getDeviceInfo())
    }
}

@Composable fun AndroidVersionChecker(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.android_version_check), nav) {
        Text(vm.getAndroidVersion())
        Text(if (Build.VERSION.SDK_INT >= 24) stringResource(R.string.supported) else stringResource(R.string.not_supported))
    }
}

@Composable fun AppStateManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.app_state), nav) {
        InfoRow(stringResource(R.string.transfer), if (vm.isTransferring) stringResource(R.string.active) else stringResource(R.string.empty))
        InfoRow(stringResource(R.string.file), vm.selectedFileName ?: "-")
        InfoRow(stringResource(R.string.network), if (vm.isNetworkAvailable) stringResource(R.string.status_ok_short) else stringResource(R.string.error))
    }
}

@Composable fun ActivityManagerScreen(nav: NavHostController) {
    ScreenScaffold((R.string.activity_manager), nav) { 
        Text(stringResource(R.string.main_activity_active))
    }
}

@Composable fun LifecycleManagerScreen(nav: NavHostController) {
    ScreenScaffold((R.string.lifecycle_manager), nav) { 
        Text(stringResource(R.string.lifecycle_resumed))
    }
}

@Composable 
fun ClipboardManagerScreen(nav: NavHostController, vm: AppViewModel) {
    val ctx = LocalContext.current
    val copiedText = stringResource(R.string.copied)

    ScreenScaffold(R.string.clipboard_manager, nav) {
        ActionButton(stringResource(R.string.copy_ip)) {
            copyToClipboard(ctx, vm.localIp)
            showToast(ctx, copiedText)
        }

        ActionButton(stringResource(R.string.copy_qr)) {
            copyToClipboard(ctx, vm.qrContent)
            showToast(ctx, copiedText)
        }
    }
}

@Composable fun VibrationManagerScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    ScreenScaffold((R.string.vibration_manager), nav) {
        ActionButton(stringResource(R.string.test_vibration)) { vibrateShort(ctx) }
    }
}

@Composable fun SoundManagerScreen(nav: NavHostController) {
    ScreenScaffold((R.string.sound_manager), nav) { 
        Text(stringResource(R.string.transfer_sound_active))
    }
}

@Composable fun ScreenManagerScreen(nav: NavHostController) {
    ScreenScaffold((R.string.screen_manager), nav) { 
        Text(stringResource(R.string.keep_screen_active))
    }
}

@Composable fun FileAccessManager(nav: NavHostController) {
    ScreenScaffold((R.string.file_access_manager), nav) {
        ActionButton(stringResource(R.string.file_permissions)) { nav.navigate(ScreenRoutes.FILE_PERMISSION) }
        ActionButton(stringResource(R.string.select_file)) { nav.navigate(ScreenRoutes.FILE_PICKER) }
    }
}

@Composable fun SystemErrorHandler(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.system_error_manager), nav) {
        Text(vm.lastError ?: vm.transferProgress.error ?: stringResource(R.string.no_error))
    }
}

// --- Transfer Engine ---

@Composable fun RealTransferEngine(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.real_transfer_engine), nav) {
        InfoRow(stringResource(R.string.port), NetworkUtils.TRANSFER_PORT.toString())
        InfoRow(stringResource(R.string.status), vm.transferProgress.status)
        ActionButton(stringResource(R.string.transfer)) { nav.navigate(ScreenRoutes.TRANSFER) }
    }
}

@Composable fun SocketManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.socket_manager), nav) {
        InfoRow(stringResource(R.string.port), NetworkUtils.TRANSFER_PORT.toString())
        InfoRow(stringResource(R.string.status), if (vm.isTransferring) stringResource(R.string.open) else stringResource(R.string.closed))
    }
}

@Composable fun DataPacketManager(nav: NavHostController) {
    ScreenScaffold((R.string.data_packet_manager), nav) { Text(stringResource(R.string.packet_size_8192)) }
}

@Composable fun ChunkSplitter(nav: NavHostController) {
    ScreenScaffold((R.string.file_split_system), nav) { Text(stringResource(R.string.chunk_size_8kb)) }
}

@Composable fun ChunkReceiver(nav: NavHostController) {
    ScreenScaffold((R.string.chunk_receiver), nav) { Text(stringResource(R.string.chunk_receive_active)) }
}

@Composable fun ResumeTransfer(nav: NavHostController) {
    ScreenScaffold((R.string.resume_transfer), nav) { Text(stringResource(R.string.resume_soon)) }
}

@Composable fun TransferQueue(nav: NavHostController) {
    ScreenScaffold((R.string.transfer_queue), nav) { Text(stringResource(R.string.queue_empty)) }
}

@Composable fun TransferScheduler(nav: NavHostController) {
    ScreenScaffold((R.string.transfer_scheduler), nav) { Text(stringResource(R.string.no_scheduled_transfer)) }
}

@Composable fun TransferMonitor(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.transfer_monitor), nav) {
        InfoRow(stringResource(R.string.status), vm.transferProgress.status)
        InfoRow(stringResource(R.string.percent), "${vm.transferProgress.percent.toInt()}%")
    }
}

@Composable fun ConnectionRecovery(nav: NavHostController) {
    ScreenScaffold((R.string.connection_recovery), nav) { Text(stringResource(R.string.auto_recovery_three)) }
}

@Composable fun TransferOptimizer(nav: NavHostController) {
    ScreenScaffold((R.string.transfer_optimizer), nav) { Text(stringResource(R.string.buffer_tcp)) }
}

@Composable fun TransferSpeedMonitor(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.speed_monitor), nav) {
        Text(String.format("%.2f MB/s", vm.transferProgress.speedMbps))
    }
}

@Composable fun PacketValidator(nav: NavHostController) {
    ScreenScaffold((R.string.packet_validator), nav) { Text(stringResource(R.string.packet_integrity_check)) }
}

@Composable fun TransferEncryption(nav: NavHostController) {
    ScreenScaffold((R.string.transfer_encryption), nav) { Text(stringResource(R.string.aes256_planned)) }
}

@Composable fun TransferCompression(nav: NavHostController) {
    ScreenScaffold((R.string.transfer_compression), nav) { Text(stringResource(R.string.compression_disabled)) }
}

@Composable fun UploadManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.upload_manager), nav) {
        InfoRow(stringResource(R.string.file), vm.selectedFileName ?: "-")
        ActionButton(stringResource(R.string.send)) { nav.navigate(ScreenRoutes.TRANSFER) }
    }
}

@Composable fun DownloadManager(nav: NavHostController) {
    ScreenScaffold((R.string.download_manager), nav) {
        ActionButton(stringResource(R.string.receive)) { nav.navigate(ScreenRoutes.RECEIVE) }
    }
}

@Composable fun TransferLogger(nav: NavHostController, vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.refreshHistory() }
    ScreenScaffold((R.string.transfer_logs), nav) {
        LogList(vm.transferLogs, stringResource(R.string.no_logs))
    }
}

@Composable fun TransferNotification(nav: NavHostController) {
    ScreenScaffold((R.string.transfer_notification), nav) { 
        Text(stringResource(R.string.transfer_notifications_active)) 
    }
}

@Composable fun TransferCancelManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.transfer_cancel), nav) {
        ActionButton(stringResource(R.string.cancel), enabled = vm.isTransferring) { vm.cancelTransfer() }
    }
}

@Composable fun TransferRetryManager(nav: NavHostController) {
    ScreenScaffold((R.string.retry_manager), nav) {
        Text(stringResource(R.string.max_retry_three))
    }
}

// --- UI ---

@Composable fun UIManager(nav: NavHostController) {
    ScreenScaffold((R.string.ui_manager), nav) {
        ActionButton(stringResource(R.string.theme)) { nav.navigate(ScreenRoutes.THEME) }
        ActionButton(stringResource(R.string.animation)) { nav.navigate(ScreenRoutes.ANIMATION) }
    }
}

@Composable fun LoadingScreen(nav: NavHostController) {
    ScreenScaffold((R.string.loading), nav) { Text(stringResource(R.string.please_wait)) }
}

@Composable fun ErrorScreen(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.error_screen), nav) {
        Text(vm.lastError ?: stringResource(R.string.error_occurred))
        ActionButton(stringResource(R.string.home)) { nav.navigate(ScreenRoutes.HOME) { popUpTo(ScreenRoutes.HOME) } }
    }
}

@Composable
fun SuccessScreen(nav: NavHostController, vm: AppViewModel) {
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        vibrateShort(ctx)
    }

    val completedLabel =
        if (vm.isSenderMode)
            R.string.send_completed
        else
            R.string.receive_completed

    ScreenScaffold(
        title = completedLabel,
        navController = nav,
        showBack = false
    ) {

        Text(
            "✓",
            style = MaterialTheme.typography.displayMedium
        )

        if (!vm.isSenderMode) {

            Text("${stringResource(R.string.saved_location)}:\nDownloads/Swox Share/${vm.receivedFile?.name ?: ""}")

        }

        ActionButton(stringResource(R.string.close)) {

            vm.resetSession()

            nav.navigate(ScreenRoutes.HOME) {
                popUpTo(ScreenRoutes.HOME) {
                    inclusive = true
                }
            }

        }
    }
}

@Composable fun EmptyStateScreen(nav: NavHostController) {
    ScreenScaffold((R.string.empty_state), nav) { Text(stringResource(R.string.no_data)) }
}

@Composable fun ConfirmDialogScreen(nav: NavHostController) {
    ScreenScaffold((R.string.confirm_window), nav) {
        Text(stringResource(R.string.confirm_operation))
        ActionButton(stringResource(R.string.confirm)) { nav.popBackStack() }
        ActionButton(stringResource(R.string.cancel)) { nav.popBackStack() }
    }
}

@Composable fun AlertDialogManager(nav: NavHostController) {
    ScreenScaffold((R.string.alert_manager), nav) { Text(stringResource(R.string.alerts_active)) }
}

@Composable fun ToastManagerScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val testMessage = stringResource(R.string.test_message)
    ScreenScaffold((R.string.toast_manager), nav) {
        ActionButton(stringResource(R.string.test_toast)) { showToast(ctx, testMessage) }
    }
}

@Composable fun SnackbarManagerScreen(nav: NavHostController) {
    ScreenScaffold((R.string.snackbar_manager), nav) { Text(stringResource(R.string.snackbar_active)) }
}

@Composable fun AnimationManager(nav: NavHostController) {
    ScreenScaffold((R.string.animation_manager), nav) {
        ActionButton(stringResource(R.string.fade)) { nav.navigate(ScreenRoutes.FADE) }
        ActionButton(stringResource(R.string.slide)) { nav.navigate(ScreenRoutes.SLIDE) }
    }
}

@Composable fun FadeAnimation(nav: NavHostController) {
    ScreenScaffold((R.string.fade_animation), nav) { Text(stringResource(R.string.fade_active)) }
}

@Composable fun SlideAnimation(nav: NavHostController) {
    ScreenScaffold((R.string.slide_animation), nav) { Text(stringResource(R.string.slide_active)) }
}

@Composable fun ScaleAnimation(nav: NavHostController) {
    ScreenScaffold((R.string.scale_animation), nav) { Text(stringResource(R.string.scale_active)) }
}

@Composable fun ProgressAnimation(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.progress_animation), nav) { ProgressDisplay(vm.transferProgress.percent) }
}

@Composable fun ButtonAnimation(nav: NavHostController) {
    ScreenScaffold((R.string.button_animation), nav) {
        ActionButton(stringResource(R.string.animated_button)) { }
    }
}

@Composable fun ThemePreview(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.theme_preview), nav) {
        Text("${stringResource(R.string.current_theme)}: ${vm.themeMode}")
    }
}

@Composable fun DarkModeManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.dark_mode_manager), nav) {
        ActionButton(stringResource(R.string.dark_theme)) { vm.setTheme("dark") }
    }
}

@Composable fun LightModeManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.light_mode_manager), nav) {
        ActionButton(stringResource(R.string.light_theme)) { vm.setTheme("light") }
    }
}

@Composable fun SystemThemeDetector(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.system_theme_detector), nav) {
        Text("${stringResource(R.string.system_theme)}: ${vm.themeMode}")
        ActionButton(stringResource(R.string.system)) { vm.setTheme("system") }
    }
}

// --- Language ---

@Composable fun LanguageSelector(nav: NavHostController) {
    ScreenScaffold((R.string.language_selection), nav) {
        ActionButton(stringResource(R.string.language_manager)) { nav.navigate(ScreenRoutes.LANGUAGE) }
    }
}

@Composable fun TranslationManager(nav: NavHostController) {
    ScreenScaffold((R.string.translation_manager), nav) {
        Text(stringResource(R.string.multilingual_support_preparing))
    }
}

@Composable fun LanguageSystem(nav: NavHostController) {
    ScreenScaffold((R.string.language_system), nav) {
        Text("${stringResource(R.string.locale)}: ${java.util.Locale.getDefault().displayName}")
    }
}

@Composable fun LocaleDetector(nav: NavHostController) {
    ScreenScaffold((R.string.system_language_detection), nav) {
        Text("${stringResource(R.string.system_language)}: ${java.util.Locale.getDefault().language}")
    }
}

@Composable fun DefaultLanguage(nav: NavHostController) {
    ScreenScaffold((R.string.default_language), nav) {
        Text("${stringResource(R.string.default_language)}: ${stringResource(R.string.english)}")
    }
}

@Composable fun EnglishLanguage(nav: NavHostController) {
    ScreenScaffold((R.string.english_language), nav) {
        Text(stringResource(R.string.language_set_english))
    }
}

@Composable fun TurkishLanguage(nav: NavHostController) {
    ScreenScaffold((R.string.turkish_language), nav) {
        Text(stringResource(R.string.language_set_turkish))
    }
}

@Composable fun AzerbaijaniLanguage(nav: NavHostController) {
    ScreenScaffold((R.string.azerbaijani_language), nav) {
        Text(stringResource(R.string.language_set_azerbaijani))
    }
}

@Composable fun StringManager(nav: NavHostController) {
    ScreenScaffold((R.string.string_manager), nav) {
        Text(stringResource(R.string.string_resources_managed))
    }
}

@Composable fun TranslationLoader(nav: NavHostController) {
    ScreenScaffold((R.string.translation_loader), nav) {
        Text(stringResource(R.string.translations_loading))
    }
}

@Composable fun LanguageSwitcher(nav: NavHostController) {
    ScreenScaffold((R.string.language_switcher), nav) {
        ActionButton(stringResource(R.string.select_language)) { nav.navigate(ScreenRoutes.LANGUAGE) }
    }
}

@Composable fun LanguageValidator(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.language_validator), nav) {
        Text("${stringResource(R.string.selected_language)}: ${vm.language}")
        Text(if (vm.language.isNotBlank()) stringResource(R.string.ok) else stringResource(R.string.error))
    }
}

// --- Database / History ---

@Composable fun DatabaseSystem(nav: NavHostController) {
    ScreenScaffold((R.string.database_system), nav) {
        Text(stringResource(R.string.shared_preferences_storage))
    }
}

@Composable fun HistoryManager(nav: NavHostController) {
    ScreenScaffold((R.string.history_manager), nav) {
        ActionButton(stringResource(R.string.transfer_history)) { nav.navigate(ScreenRoutes.TRANSFER_HISTORY) }
    }
}

@Composable fun TransferDatabase(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.transfer_database), nav) {
        InfoRow(stringResource(R.string.records), vm.transferHistory.size.toString())
    }
}

@Composable fun FileHistory(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.file_history), nav) {
        LogList(
            vm.transferHistory.filter { it.contains("Fayl") || it.contains("Göndər") || it.contains("Qəbul") },
            stringResource(R.string.empty)
        )
    }
}

@Composable fun SearchHistory(nav: NavHostController, vm: AppViewModel) {
    var query by remember { mutableStateOf("") }

    ScreenScaffold((R.string.search_history), nav) {
        OutlinedTextField(
            query,
            { query = it },
            label = { Text(stringResource(R.string.search)) },
            modifier = Modifier.fillMaxWidth()
        )

        val filtered = vm.transferHistory.filter { it.contains(query, ignoreCase = true) }
        LogList(filtered, stringResource(R.string.no_results))
    }
}

@Composable fun DeleteHistory(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.delete_history), nav) {
        ActionButton(stringResource(R.string.delete_all_history)) { vm.clearHistory() }
    }
}

@Composable fun ExportHistory(nav: NavHostController, vm: AppViewModel) {
    val ctx = LocalContext.current
    val copied = stringResource(R.string.copied)
    // Əgər export_done resursunuz varsa, onu burada tanımlayın:
    val exportDoneText = stringResource(R.string.export_done) 

    ScreenScaffold((R.string.export_history), nav) {
        ActionButton(stringResource(R.string.copy)) {
            copyToClipboard(ctx, vm.transferHistory.joinToString("\n"))
            // Burada tanımladığınız dəyişəni istifadə edin:
            showToast(ctx, exportDoneText) 
        }
    }
}

@Composable fun ImportHistory(nav: NavHostController) {
    ScreenScaffold((R.string.import_history), nav) {
        Text(stringResource(R.string.import_soon))
    }
}

@Composable fun HistoryBackup(nav: NavHostController) {
    ScreenScaffold((R.string.history_backup), nav) {
        ActionButton(stringResource(R.string.backup)) { nav.navigate(ScreenRoutes.BACKUP_SYSTEM) }
    }
}

@Composable fun HistoryCleaner(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.history_cleaner), nav) {
        ActionButton(stringResource(R.string.clear)) { vm.clearHistory() }
    }
}

@Composable fun HistoryStorage(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.history_storage), nav) {
        InfoRow(stringResource(R.string.records), vm.transferHistory.size.toString())
    }
}

// --- Bluetooth ---

@Composable fun BluetoothSystem(nav: NavHostController) {
    ScreenScaffold((R.string.bluetooth_system), nav) {
        ActionButton(stringResource(R.string.bluetooth_manager)) { nav.navigate(ScreenRoutes.BLUETOOTH_MANAGER) }
    }
}

@Composable fun BluetoothManagerScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val adapter = getBluetoothAdapter(ctx)

    ScreenScaffold((R.string.bluetooth_manager), nav) {
        Text(
            stringResource(
                R.string.bluetooth_status,
                if (adapter?.isEnabled == true)
                    stringResource(R.string.active)
                else
                    stringResource(R.string.disabled)
            )
        )
        ActionButton(stringResource(R.string.device_list)) { nav.navigate(ScreenRoutes.BLUETOOTH_DEVICES) }
    }
}

@Composable
fun BluetoothScanner(nav: NavHostController) {
    ScreenScaffold((R.string.bluetooth_scan), nav) {
        Text(stringResource(R.string.bluetooth_scan_start))
    }
}

@Composable
fun BluetoothConnector(nav: NavHostController) {
    ScreenScaffold((R.string.bluetooth_connect), nav) {
        Text(stringResource(R.string.connect_device))
    }
}

@Composable
fun BluetoothDeviceList(nav: NavHostController) {
    val ctx = LocalContext.current
    val adapter = getBluetoothAdapter(ctx)

    ScreenScaffold((R.string.bluetooth_device_list), nav) {

        val bonded = try {
            if (
                ActivityCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                adapter?.bondedDevices
                    ?.map { it.name ?: it.address }
                    ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            emptyList()
        }

        if (bonded.isEmpty()) {
            Text(stringResource(R.string.no_devices))
        } else {
            bonded.forEach { device ->
                Text(device)
            }
        }
    }
}

@Composable fun BluetoothTransfer(nav: NavHostController) {
    ScreenScaffold((R.string.bluetooth_transfer), nav) {
        Text(stringResource(R.string.bluetooth_transfer_soon))
    }
}

@Composable fun BluetoothSecurity(nav: NavHostController) {
    ScreenScaffold((R.string.bluetooth_security), nav) {
        Text(stringResource(R.string.pairing_required))
    }
}

@Composable fun BluetoothState(nav: NavHostController) {
    val ctx = LocalContext.current
    val adapter = getBluetoothAdapter(ctx)

    ScreenScaffold((R.string.bluetooth_state), nav) {
        Text(
            if (adapter?.isEnabled == true)
                stringResource(R.string.active_check)
            else
                stringResource(R.string.disabled_check)
        )
    }
}

@Composable fun BluetoothLogger(nav: NavHostController) {
    ScreenScaffold((R.string.bluetooth_logs), nav) {
        Text(stringResource(R.string.bluetooth_logs_empty))
    }
}

@Composable fun BluetoothSettings(nav: NavHostController) {
    ScreenScaffold((R.string.bluetooth_settings), nav) {
        Text(stringResource(R.string.open_system_bluetooth))
    }
}

// --- Connection ---

@Composable fun ConnectionSystem(nav: NavHostController) {
    ScreenScaffold((R.string.connection_system), nav) {
        ActionButton(stringResource(R.string.server)) { nav.navigate(ScreenRoutes.SERVER) }
        ActionButton(stringResource(R.string.client)) { nav.navigate(ScreenRoutes.CLIENT) }
    }
}

@Composable fun ServerManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.server_manager), nav) {
        InfoRow(stringResource(R.string.port), NetworkUtils.TRANSFER_PORT.toString())
        ActionButton(stringResource(R.string.receive)) { nav.navigate(ScreenRoutes.RECEIVE) }
    }
}

@Composable fun ClientManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.client_manager), nav) {
        InfoRow(stringResource(R.string.target), vm.connectionTargetIp.ifBlank { "-" })
        ActionButton(stringResource(R.string.send)) { nav.navigate(ScreenRoutes.SEND) }
    }
}

@Composable fun SocketConnection(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.socket_connection), nav) {
        InfoRow(stringResource(R.string.port), NetworkUtils.TRANSFER_PORT.toString())
        InfoRow(stringResource(R.string.status), vm.transferProgress.status)
    }
}

@Composable fun IPScanner(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.ip_scanner), nav) {
        ActionButton(stringResource(R.string.scan)) { vm.scanDevices() }
        vm.scannedDevices.forEach { Text(it) }
    }
}

@Composable fun PortManager(nav: NavHostController) {
    ScreenScaffold((R.string.port_manager), nav) {
        InfoRow(stringResource(R.string.transfer_port), NetworkUtils.TRANSFER_PORT.toString())
    }
}

@Composable fun ConnectionValidator(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.connection_validator), nav) {
        Text(
            if (vm.isNetworkAvailable)
                stringResource(R.string.network_ok)
            else
                stringResource(R.string.network_unavailable)
        )

        Text(
            if (vm.connectionTargetIp.isNotBlank())
                stringResource(R.string.target_set)
            else
                stringResource(R.string.no_target)
        )
    }
}

@Composable fun DeviceDiscovery(nav: NavHostController) {
    ScreenScaffold((R.string.device_discovery), nav) {
        ActionButton(stringResource(R.string.search)) {
            nav.navigate(ScreenRoutes.DEVICE_SCANNER)
        }
    }
}

@Composable fun PairingManager(nav: NavHostController) {
    ScreenScaffold((R.string.pairing_manager), nav) {
        Text(stringResource(R.string.manual_ip_connection))
    }
}

@Composable fun ConnectionHistory(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.connection_history), nav) {
        LogList(vm.transferHistory, stringResource(R.string.empty))
    }
}

@Composable fun ConnectionMonitor(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.connection_monitor), nav) {
        InfoRow(
            stringResource(R.string.network),
            if (vm.isNetworkAvailable)
                stringResource(R.string.active)
            else
                stringResource(R.string.disabled)
        )

        InfoRow(
            stringResource(R.string.transfer),
            if (vm.isTransferring)
                stringResource(R.string.active)
            else
                stringResource(R.string.empty)
        )
    }
}

// --- Backup ---

@Composable fun BackupSystem(nav: NavHostController) {
    ScreenScaffold((R.string.backup_system), nav) {
        ActionButton(stringResource(R.string.local_backup)) { nav.navigate(ScreenRoutes.LOCAL_BACKUP) }
        ActionButton(stringResource(R.string.cloud_backup)) { nav.navigate(ScreenRoutes.CLOUD_BACKUP) }
    }
}

@Composable fun AutoBackup(nav: NavHostController) {
    ScreenScaffold((R.string.auto_backup), nav) {
        Text(stringResource(R.string.auto_backup_disabled))
    }
}

@Composable fun BackupExporter(nav: NavHostController, vm: AppViewModel) {
    val ctx = LocalContext.current
    val exportDone = stringResource(R.string.export_done)
    ScreenScaffold((R.string.backup_export), nav) {
        ActionButton(stringResource(R.string.export)) {
            copyToClipboard(ctx, vm.transferHistory.joinToString("\n"))
            showToast(ctx, exportDone)
        }
    }
}

@Composable fun BackupImporter(nav: NavHostController) {
    ScreenScaffold((R.string.backup_import), nav) {
        Text(stringResource(R.string.import_soon))
    }
}

@Composable fun CloudBackup(nav: NavHostController) {
    ScreenScaffold((R.string.cloud_backup), nav) {
        Text(stringResource(R.string.cloud_backup_not_supported))
    }
}

@Composable fun LocalBackup(nav: NavHostController, vm: AppViewModel) {
    val ctx = LocalContext.current
    val backupDone = stringResource(R.string.backup_done)
    ScreenScaffold((R.string.local_backup), nav) {
        ActionButton(stringResource(R.string.backup)) {
            copyToClipboard(ctx, "theme=${vm.themeMode};lang=${vm.language}")
            showToast(ctx, backupDone)
        }
    }
}

@Composable fun BackupValidator(nav: NavHostController) {
    ScreenScaffold((R.string.backup_validator), nav) {
        Text(stringResource(R.string.backup_format_ok))
    }
}


// --- Performance / Utility ---

@Composable fun PerformanceSystem(nav: NavHostController) {
    ScreenScaffold((R.string.performance_system), nav) {
        ActionButton(stringResource(R.string.memory)) { nav.navigate(ScreenRoutes.MEMORY) }
        ActionButton(stringResource(R.string.battery)) { nav.navigate(ScreenRoutes.BATTERY_OPTIMIZER) }
    }
}

@Composable fun MemoryManager(nav: NavHostController) {
    val rt = Runtime.getRuntime()

    ScreenScaffold((R.string.memory_manager), nav) {
        InfoRow(
            stringResource(R.string.free_ram),
            "${(rt.freeMemory() / 1024 / 1024)} MB"
        )
        InfoRow(
            stringResource(R.string.total_ram),
            "${(rt.totalMemory() / 1024 / 1024)} MB"
        )
    }
}

@Composable fun StorageOptimizer(nav: NavHostController) {
    ScreenScaffold((R.string.storage_optimizer), nav) {
        val stat = StatFs(Environment.getDataDirectory().path)
        InfoRow(
            stringResource(R.string.free_storage),
            "${stat.availableBytes / 1024 / 1024 / 1024} GB"
        )
    }
}

@Composable fun BatteryOptimizer(nav: NavHostController) {
    ScreenScaffold((R.string.battery_optimizer), nav) {
        Text(stringResource(R.string.power_saving_normal))
    }
}

@Composable fun SpeedOptimizer(nav: NavHostController) {
    ScreenScaffold((R.string.speed_optimizer), nav) {
        Text(stringResource(R.string.tcp_buffer))
    }
}

@Composable fun UtilitySystem(nav: NavHostController) {
    ScreenScaffold((R.string.utility_system), nav) {
        ActionButton(stringResource(R.string.datetime)) { nav.navigate(ScreenRoutes.DATETIME) }
        ActionButton(stringResource(R.string.log)) { nav.navigate(ScreenRoutes.LOGGER) }
    }
}

@Composable fun DateTimeManager(nav: NavHostController) {
    ScreenScaffold((R.string.datetime_manager), nav) {
        Text(currentDateTime())
    }
}

@Composable fun FileSizeFormatter(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.file_size_formatter), nav) {
        Text(vm.formatFileSize(vm.selectedFileSize))
    }
}

@Composable fun LoggerManager(nav: NavHostController) {
    ScreenScaffold((R.string.logger_manager), nav) {
        ActionButton(stringResource(R.string.transfer_logs)) {
            nav.navigate(ScreenRoutes.TRANSFER_LOGGER)
        }

        ActionButton(stringResource(R.string.security_logs)) {
            nav.navigate(ScreenRoutes.SECURITY_LOGGER)
        }
    }
}

@Composable fun DebugManager(nav: NavHostController, vm: AppViewModel) {
    ScreenScaffold((R.string.debug_manager), nav) {
        InfoRow(stringResource(R.string.ip), vm.localIp)
        InfoRow(stringResource(R.string.transfer), vm.transferProgress.status)
    }
}

@Composable fun UpdateChecker(nav: NavHostController) {
    ScreenScaffold((R.string.update_checker), nav) {
        Text(stringResource(R.string.version_no_update))
    }
}

@Composable fun AboutManager(nav: NavHostController) {
    ScreenScaffold((R.string.about_manager), nav) {
        ActionButton(stringResource(R.string.about)) {
            nav.navigate(ScreenRoutes.ABOUT)
        }
    }
}

// --- Helpers ---

@Composable
private fun PermissionScreen(
    nav: NavHostController,
    title: Int,
    permission: String,
    requestable: Boolean = true
) {
    val ctx = LocalContext.current
    val granted = ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    ScreenScaffold(title, nav) {
        Text(
            if (granted)
                stringResource(R.string.permission_granted)
            else
                stringResource(R.string.permission_denied)
        )

        if (!granted && requestable) {
            ActionButton(stringResource(R.string.request_permission)) {
                launcher.launch(permission)
            }
        }
    }
}

private fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
    return if (Build.VERSION.SDK_INT >= 31) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    } else {
        @Suppress("DEPRECATION")
        BluetoothAdapter.getDefaultAdapter()
    }
}

private fun formatDirSize(dir: File): String {
    val bytes = dir.walkTopDown()
        .filter { it.isFile }
        .map { it.length() }
        .sum()

    return when {
        bytes >= 1024 * 1024 ->
            String.format("%.1f MB", bytes / 1024.0 / 1024.0)

        bytes >= 1024 ->
            String.format("%.1f KB", bytes / 1024.0)

        else ->
            "$bytes B"
    }
}

private fun computeUriHash(context: Context, uri: android.net.Uri): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            var read: Int

            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }

        digest.digest().joinToString("") { "%02x".format(it) }

    } catch (_: Exception) {
        "-"
    }
}