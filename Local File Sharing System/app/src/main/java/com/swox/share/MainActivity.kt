package com.swox.share

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {

        val prefs = newBase.getSharedPreferences(
            "swox_share_prefs",
            Context.MODE_PRIVATE
        )

        val language = prefs.getString("language", "system") ?: "system"

        if (language != "system") {

            val locale = Locale(language)

            Locale.setDefault(locale)

            val config = Configuration(
                newBase.resources.configuration
            )

            config.setLocale(locale)

            super.attachBaseContext(
                newBase.createConfigurationContext(config)
            )

        } else {
            super.attachBaseContext(newBase)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SwoxShareApp()
        }
    }
}

@androidx.compose.runtime.Composable
fun SwoxShareApp() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = viewModel()
    val themeMode = viewModel.themeMode

    SwoxShareTheme(themeMode = themeMode) {
        if (!viewModel.hasRequiredPermissions) {
            PermissionRequiredScreen()
        } else {
            NavHost(navController = navController, startDestination = ScreenRoutes.SPLASH) {

                composable(ScreenRoutes.SPLASH) { SplashScreen(navController) }
            composable(ScreenRoutes.HOME) { HomeScreen(navController) }

            composable(ScreenRoutes.SEND) { SendScreen(navController, viewModel) }
            composable(ScreenRoutes.FILE_PICKER) { FilePicker(navController, viewModel) }
            composable(ScreenRoutes.FILE_SELECTOR) { FileSelector(navController, viewModel) }
            composable(ScreenRoutes.FILE_INFO) { FileInfoReader(navController, viewModel) }
            composable(ScreenRoutes.FILE_VALIDATOR) { FileValidator(navController, viewModel) }
            composable(ScreenRoutes.FILE_PERMISSION) { FilePermissionManager(navController) }
            composable(ScreenRoutes.TRANSFER) { TransferScreen(navController, viewModel) }

            composable(ScreenRoutes.RECEIVE) { ReceiveScreen(navController, viewModel) }
            composable(ScreenRoutes.DEVICE_SCANNER) { DeviceScanner(navController, viewModel) }
            composable(ScreenRoutes.CONNECTION_MANAGER) { ConnectionManager(navController, viewModel) }
            composable(ScreenRoutes.FILE_TRANSFER) { FileTransfer(navController, viewModel) }
            composable(ScreenRoutes.PROGRESS) { ProgressTracker(navController, viewModel) }
            composable(ScreenRoutes.SPEED) { SpeedCalculator(navController, viewModel) }
            composable(ScreenRoutes.TIME_REMAINING) { TimeRemainingCalculator(navController, viewModel) }
            composable(ScreenRoutes.TRANSFER_STATUS) { TransferStatus(navController, viewModel) }
            composable(ScreenRoutes.TRANSFER_HISTORY) { TransferHistory(navController, viewModel) }
            composable(ScreenRoutes.TRANSFER_ERROR) { TransferError(navController, viewModel) }

            composable(ScreenRoutes.SETTINGS) { SettingsScreen(navController) }
            composable(ScreenRoutes.THEME) { ThemeManager(navController, viewModel) }
            composable(ScreenRoutes.LANGUAGE) { LanguageManager(navController, viewModel) }
            composable(ScreenRoutes.ABOUT) { AboutScreen(navController, viewModel) }

            composable(ScreenRoutes.QR_GENERATOR) { QRGenerator(navController, viewModel) }
            composable(ScreenRoutes.QR_SCANNER) { QRScanner(navController, viewModel) }
            composable(ScreenRoutes.QR_SCANNER_CHECK) { QRScannerCheck(navController, viewModel) }
            composable(ScreenRoutes.QR_DATA_ENCODER) { QRDataEncoder(navController, viewModel) }
            composable(ScreenRoutes.QR_IMAGE_CREATOR) { QRImageCreator(navController, viewModel) }
            composable(ScreenRoutes.QR_EXPORTER) { QRExporter(navController, viewModel) }
            composable(ScreenRoutes.QR_VALIDATOR) { QRValidator(navController, viewModel) }
            composable(ScreenRoutes.QR_DECODER) { QRDecoder(navController, viewModel) }
            composable(ScreenRoutes.QR_SCANNER_ENGINE) { QRScannerEngine(navController) }
            composable(ScreenRoutes.QR_PERMISSION) { QRPermissionManager(navController) }
            composable(ScreenRoutes.QR_CONNECTION) { QRConnectionHandler(navController, viewModel) }

            composable(ScreenRoutes.HOTSPOT_INFO) { HotspotInfo(navController, viewModel) }
            composable(ScreenRoutes.HOTSPOT_MANAGER) { HotspotManager(navController) }
            composable(ScreenRoutes.HOTSPOT_CREATOR) { HotspotCreator(navController) }
            composable(ScreenRoutes.HOTSPOT_PASSWORD) { HotspotPasswordManager(navController, viewModel) }
            composable(ScreenRoutes.MANUAL_INPUT) { ManualInput(navController, viewModel) }
            composable(ScreenRoutes.MANUAL_CONNECT) { ManualConnect(navController, viewModel) }

            composable(ScreenRoutes.WIFI_CONNECTOR) { WifiConnector(navController, viewModel) }
            composable(ScreenRoutes.WIFI_SCANNER) { WifiScanner(navController, viewModel) }
        //  composable(ScreenRoutes.NETWORK_CHECKER) { NetworkChecker(navController, viewModel) }
        //  composable(ScreenRoutes.IP_MANAGER) { IPManager(navController, viewModel) }
        //  composable(ScreenRoutes.CONNECTION_SECURITY) { ConnectionSecurity(navController, viewModel) }

            composable(ScreenRoutes.PREFERENCES) { PreferencesManager(navController, viewModel) }
            composable(ScreenRoutes.THEME_STORAGE) { ThemeStorage(navController, viewModel) }
            composable(ScreenRoutes.LANGUAGE_STORAGE) { LanguageStorageScreen(navController, viewModel) }
            composable(ScreenRoutes.USER_SETTINGS) { UserSettings(navController) }
            composable(ScreenRoutes.BACKUP) { BackupManager(navController) }
            composable(ScreenRoutes.RESTORE) { RestoreManager(navController) }
            composable(ScreenRoutes.CACHE) { CacheManager(navController) }
            composable(ScreenRoutes.DATABASE) { DatabaseManager(navController, viewModel) }
            composable(ScreenRoutes.SETTINGS_VALIDATOR) { SettingsValidator(navController, viewModel) }
            composable(ScreenRoutes.SETTINGS_RESET) { SettingsResetManager(navController, viewModel) }

            composable(ScreenRoutes.SECURITY) { SecurityManager(navController) }
            composable(ScreenRoutes.ENCRYPTION) { EncryptionManager(navController) }
            composable(ScreenRoutes.KEY_MANAGER) { KeyManager(navController) }
            composable(ScreenRoutes.PASSWORD_MANAGER) { PasswordManager(navController, viewModel) }
            composable(ScreenRoutes.SESSION) { SessionManager(navController, viewModel) }
            composable(ScreenRoutes.DEVICE_AUTH) { DeviceAuthentication(navController, viewModel) }
            composable(ScreenRoutes.FILE_HASH) { FileHashChecker(navController, viewModel) }
            composable(ScreenRoutes.INTEGRITY) { IntegrityChecker(navController, viewModel) }
            composable(ScreenRoutes.SECURE_TRANSFER) { SecureTransfer(navController) }
            composable(ScreenRoutes.ACCESS_CONTROL) { AccessControl(navController) }
            composable(ScreenRoutes.SECURITY_LOGGER) { SecurityLogger(navController, viewModel) }

            composable(ScreenRoutes.ANDROID_SYSTEM) { AndroidSystemManager(navController) }
            composable(ScreenRoutes.PERMISSION_MANAGER) { PermissionManagerScreen(navController) }
            composable(ScreenRoutes.CAMERA_PERMISSION) { CameraPermissionScreen(navController) }
            composable(ScreenRoutes.STORAGE_PERMISSION) { StoragePermissionScreen(navController) }
            composable(ScreenRoutes.WIFI_PERMISSION) { WifiPermissionScreen(navController) }
            composable(ScreenRoutes.BLUETOOTH_PERMISSION) { BluetoothPermissionScreen(navController) }
            composable(ScreenRoutes.NOTIFICATION_PERMISSION) { NotificationPermissionScreen(navController) }
            composable(ScreenRoutes.BATTERY) { BatteryManagerScreen(navController) }
            composable(ScreenRoutes.DEVICE_INFO) { DeviceInfoManager(navController, viewModel) }
            composable(ScreenRoutes.ANDROID_VERSION) { AndroidVersionChecker(navController, viewModel) }
            composable(ScreenRoutes.APP_STATE) { AppStateManager(navController, viewModel) }
            composable(ScreenRoutes.ACTIVITY_MANAGER) { ActivityManagerScreen(navController) }
            composable(ScreenRoutes.LIFECYCLE) { LifecycleManagerScreen(navController) }
            composable(ScreenRoutes.CLIPBOARD) { ClipboardManagerScreen(navController, viewModel) }
            composable(ScreenRoutes.VIBRATION) { VibrationManagerScreen(navController) }
            composable(ScreenRoutes.SOUND) { SoundManagerScreen(navController) }
            composable(ScreenRoutes.SCREEN) { ScreenManagerScreen(navController) }
            composable(ScreenRoutes.NETWORK_PERMISSION) { NetworkPermissionScreen(navController) }
            composable(ScreenRoutes.LOCATION_PERMISSION) { LocationPermissionScreen(navController) }
            composable(ScreenRoutes.FILE_ACCESS) { FileAccessManager(navController) }
            composable(ScreenRoutes.SYSTEM_ERROR) { SystemErrorHandler(navController, viewModel) }

            composable(ScreenRoutes.REAL_TRANSFER) { RealTransferEngine(navController, viewModel) }
            composable(ScreenRoutes.SOCKET) { SocketManager(navController, viewModel) }
            composable(ScreenRoutes.DATA_PACKET) { DataPacketManager(navController) }
            composable(ScreenRoutes.CHUNK_SPLITTER) { ChunkSplitter(navController) }
            composable(ScreenRoutes.CHUNK_RECEIVER) { ChunkReceiver(navController) }
            composable(ScreenRoutes.RESUME_TRANSFER) { ResumeTransfer(navController) }
            composable(ScreenRoutes.TRANSFER_QUEUE) { TransferQueue(navController) }
            composable(ScreenRoutes.TRANSFER_SCHEDULER) { TransferScheduler(navController) }
            composable(ScreenRoutes.TRANSFER_MONITOR) { TransferMonitor(navController, viewModel) }
            composable(ScreenRoutes.CONNECTION_RECOVERY) { ConnectionRecovery(navController) }
            composable(ScreenRoutes.TRANSFER_OPTIMIZER) { TransferOptimizer(navController) }
            composable(ScreenRoutes.SPEED_MONITOR) { TransferSpeedMonitor(navController, viewModel) }
            composable(ScreenRoutes.PACKET_VALIDATOR) { PacketValidator(navController) }
            composable(ScreenRoutes.TRANSFER_ENCRYPTION) { TransferEncryption(navController) }
            composable(ScreenRoutes.TRANSFER_COMPRESSION) { TransferCompression(navController) }
            composable(ScreenRoutes.UPLOAD) { UploadManager(navController, viewModel) }
            composable(ScreenRoutes.DOWNLOAD) { DownloadManager(navController) }
            composable(ScreenRoutes.TRANSFER_LOGGER) { TransferLogger(navController, viewModel) }
            composable(ScreenRoutes.TRANSFER_NOTIFICATION) { TransferNotification(navController) }
            composable(ScreenRoutes.TRANSFER_CANCEL) { TransferCancelManager(navController, viewModel) }
            composable(ScreenRoutes.TRANSFER_RETRY) { TransferRetryManager(navController) }

            composable(ScreenRoutes.UI_MANAGER) { UIManager(navController) }
            composable(ScreenRoutes.LOADING) { LoadingScreen(navController) }
            composable(ScreenRoutes.ERROR) { ErrorScreen(navController, viewModel) }
            composable(ScreenRoutes.SUCCESS) { SuccessScreen(navController, viewModel) }
            composable(ScreenRoutes.EMPTY_STATE) { EmptyStateScreen(navController) }
            composable(ScreenRoutes.CONFIRM_DIALOG) { ConfirmDialogScreen(navController) }
            composable(ScreenRoutes.ALERT_DIALOG) { AlertDialogManager(navController) }
            composable(ScreenRoutes.TOAST) { ToastManagerScreen(navController) }
            composable(ScreenRoutes.SNACKBAR) { SnackbarManagerScreen(navController) }
            composable(ScreenRoutes.ANIMATION) { AnimationManager(navController) }
            composable(ScreenRoutes.FADE) { FadeAnimation(navController) }
            composable(ScreenRoutes.SLIDE) { SlideAnimation(navController) }
            composable(ScreenRoutes.SCALE) { ScaleAnimation(navController) }
            composable(ScreenRoutes.PROGRESS_ANIM) { ProgressAnimation(navController, viewModel) }
            composable(ScreenRoutes.BUTTON_ANIM) { ButtonAnimation(navController) }
            composable(ScreenRoutes.THEME_PREVIEW) { ThemePreview(navController, viewModel) }
            composable(ScreenRoutes.DARK_MODE) { DarkModeManager(navController, viewModel) }
            composable(ScreenRoutes.LIGHT_MODE) { LightModeManager(navController, viewModel) }
            composable(ScreenRoutes.SYSTEM_THEME) { SystemThemeDetector(navController, viewModel) }

            composable(ScreenRoutes.LANGUAGE_SELECTOR) { LanguageSelector(navController) }
            composable(ScreenRoutes.TRANSLATION) { TranslationManager(navController) }
            composable(ScreenRoutes.LANGUAGE_SYSTEM) { LanguageSystem(navController) }
            composable(ScreenRoutes.LOCALE) { LocaleDetector(navController) }
            composable(ScreenRoutes.DEFAULT_LANGUAGE) { DefaultLanguage(navController) }
            composable(ScreenRoutes.ENGLISH) { EnglishLanguage(navController) }
            composable(ScreenRoutes.TURKISH) { TurkishLanguage(navController) }
            composable(ScreenRoutes.AZERBAIJANI) { AzerbaijaniLanguage(navController) }
            composable(ScreenRoutes.STRING_MANAGER) { StringManager(navController) }
            composable(ScreenRoutes.TRANSLATION_LOADER) { TranslationLoader(navController) }
            composable(ScreenRoutes.LANGUAGE_SWITCHER) { LanguageSwitcher(navController) }
            composable(ScreenRoutes.LANGUAGE_VALIDATOR) { LanguageValidator(navController, viewModel) }

            composable(ScreenRoutes.DATABASE_SYSTEM) { DatabaseSystem(navController) }
            composable(ScreenRoutes.HISTORY_MANAGER) { HistoryManager(navController) }
            composable(ScreenRoutes.TRANSFER_DATABASE) { TransferDatabase(navController, viewModel) }
            composable(ScreenRoutes.FILE_HISTORY) { FileHistory(navController, viewModel) }
            composable(ScreenRoutes.SEARCH_HISTORY) { SearchHistory(navController, viewModel) }
            composable(ScreenRoutes.DELETE_HISTORY) { DeleteHistory(navController, viewModel) }
            composable(ScreenRoutes.EXPORT_HISTORY) { ExportHistory(navController, viewModel) }
            composable(ScreenRoutes.IMPORT_HISTORY) { ImportHistory(navController) }
            composable(ScreenRoutes.HISTORY_BACKUP) { HistoryBackup(navController) }
            composable(ScreenRoutes.HISTORY_CLEANER) { HistoryCleaner(navController, viewModel) }
            composable(ScreenRoutes.HISTORY_STORAGE) { HistoryStorage(navController, viewModel) }

            composable(ScreenRoutes.BLUETOOTH_SYSTEM) { BluetoothSystem(navController) }
            composable(ScreenRoutes.BLUETOOTH_MANAGER) { BluetoothManagerScreen(navController) }
            composable(ScreenRoutes.BLUETOOTH_SCANNER) { BluetoothScanner(navController) }
            composable(ScreenRoutes.BLUETOOTH_CONNECTOR) { BluetoothConnector(navController) }
            composable(ScreenRoutes.BLUETOOTH_DEVICES) { BluetoothDeviceList(navController) }
            composable(ScreenRoutes.BLUETOOTH_TRANSFER) { BluetoothTransfer(navController) }
            composable(ScreenRoutes.BLUETOOTH_SECURITY) { BluetoothSecurity(navController) }
            composable(ScreenRoutes.BLUETOOTH_STATE) { BluetoothState(navController) }
            composable(ScreenRoutes.BLUETOOTH_LOGGER) { BluetoothLogger(navController) }
            composable(ScreenRoutes.BLUETOOTH_SETTINGS) { BluetoothSettings(navController) }

            composable(ScreenRoutes.CONNECTION_SYSTEM) { ConnectionSystem(navController) }
            composable(ScreenRoutes.SERVER) { ServerManager(navController, viewModel) }
            composable(ScreenRoutes.CLIENT) { ClientManager(navController, viewModel) }
            composable(ScreenRoutes.SOCKET_CONNECTION) { SocketConnection(navController, viewModel) }
            composable(ScreenRoutes.IP_SCANNER) { IPScanner(navController, viewModel) }
            composable(ScreenRoutes.PORT_MANAGER) { PortManager(navController) }
            composable(ScreenRoutes.CONNECTION_VALIDATOR) { ConnectionValidator(navController, viewModel) }
            composable(ScreenRoutes.DEVICE_DISCOVERY) { DeviceDiscovery(navController) }
            composable(ScreenRoutes.PAIRING) { PairingManager(navController) }
            composable(ScreenRoutes.CONNECTION_HISTORY) { ConnectionHistory(navController, viewModel) }
            composable(ScreenRoutes.CONNECTION_MONITOR) { ConnectionMonitor(navController, viewModel) }

            composable(ScreenRoutes.BACKUP_SYSTEM) { BackupSystem(navController) }
            composable(ScreenRoutes.AUTO_BACKUP) { AutoBackup(navController) }
            composable(ScreenRoutes.BACKUP_EXPORT) { BackupExporter(navController, viewModel) }
            composable(ScreenRoutes.BACKUP_IMPORT) { BackupImporter(navController) }
            composable(ScreenRoutes.CLOUD_BACKUP) { CloudBackup(navController) }
            composable(ScreenRoutes.LOCAL_BACKUP) { LocalBackup(navController, viewModel) }
            composable(ScreenRoutes.BACKUP_VALIDATOR) { BackupValidator(navController) }

            composable(ScreenRoutes.PERFORMANCE) { PerformanceSystem(navController) }
            composable(ScreenRoutes.MEMORY) { MemoryManager(navController) }
            composable(ScreenRoutes.STORAGE_OPTIMIZER) { StorageOptimizer(navController) }
            composable(ScreenRoutes.BATTERY_OPTIMIZER) { BatteryOptimizer(navController) }
            composable(ScreenRoutes.SPEED_OPTIMIZER) { SpeedOptimizer(navController) }
            composable(ScreenRoutes.UTILITY) { UtilitySystem(navController) }
            composable(ScreenRoutes.DATETIME) { DateTimeManager(navController) }
            composable(ScreenRoutes.FILE_SIZE_FORMAT) { FileSizeFormatter(navController, viewModel) }
            composable(ScreenRoutes.LOGGER) { LoggerManager(navController) }
            composable(ScreenRoutes.DEBUG) { DebugManager(navController, viewModel) }
            composable(ScreenRoutes.UPDATE) { UpdateChecker(navController) }
            composable(ScreenRoutes.ABOUT_MANAGER) { AboutManager(navController) }
            }
        }
    }
}
