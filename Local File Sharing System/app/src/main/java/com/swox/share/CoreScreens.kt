package com.swox.share

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.widget.Toast
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun SplashScreen(navController: NavHostController) {
    var startAnimation by remember { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation && !fadeOut) 1f else 0f,
        animationSpec = tween(800),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (startAnimation && !fadeOut) 1f else 0.85f,
        animationSpec = tween(800),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        fadeOut = true
        delay(600)
        navController.navigate(ScreenRoutes.HOME) {
            popUpTo(ScreenRoutes.SPLASH) { inclusive = true }
        }
    }

    SplashContent(visible = startAnimation, alpha = alpha, scale = scale)
}

@Composable
fun HomeScreen(navController: NavHostController) {
    ScreenScaffold(
        title = R.string.home,
        navController = navController,
        showBack = false,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                HomeFloatingIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                ) {
                    navController.navigate(ScreenRoutes.SETTINGS)
                }

                HomeFloatingIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = stringResource(R.string.about)
                ) {
                    navController.navigate(ScreenRoutes.ABOUT)
                }
            }
        }
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            HomeHeader()

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryActionButton(
                stringResource(R.string.send),
                SendBlue
            ) {
                navController.navigate(ScreenRoutes.SEND)
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryActionButton(
                stringResource(R.string.receive),
                ReceiveGreen
            ) {
                navController.navigate(ScreenRoutes.RECEIVE)
            }
        }
    }
}

@Composable
private fun HomeFloatingIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}

@Composable
fun SendScreen(navController: NavHostController, viewModel: AppViewModel) {
    val context = LocalContext.current
    var sessionStarted by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasLocationPermission = granted
        }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = readFileInfo(context, uri)
            viewModel.setSelectedFile(uri, name, size)
            viewModel.logTransfer("Fayl seçildi: $name")
            sessionStarted = false
        }
    }

    LaunchedEffect(
        viewModel.selectedFileUri,
        hasLocationPermission
    ) {
        if (viewModel.selectedFileUri != null && !sessionStarted) {

            if (!hasLocationPermission) {
                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                return@LaunchedEffect
            }

            sessionStarted = true

            viewModel.prepareSendSession(
                onReady = { },
                onTransferStart = {
                    navController.navigate(ScreenRoutes.TRANSFER)
                },
                onComplete = { success ->
                    navController.navigate(
                        if (success) ScreenRoutes.SUCCESS
                        else ScreenRoutes.TRANSFER_ERROR
                    ) {
                        popUpTo(ScreenRoutes.SEND)
                    }
                }
            )
        }
    }

    val qrBitmap = remember(viewModel.qrContent) {
        if (viewModel.qrContent.isNotBlank())
            QrUtils.generateQrBitmap(viewModel.qrContent, 480)
        else null
    }

    ScreenScaffold(
        R.string.send,
        navController
    ) {

        ActionButton(stringResource(R.string.select_file)) {
            fileLauncher.launch("*/*")
        }

        if (viewModel.selectedFileName != null) {
            Spacer(modifier = Modifier.height(8.dp))

            FileInfoCard(
                fileName = viewModel.selectedFileName ?: "-",
                fileSize = viewModel.formatFileSize(viewModel.selectedFileSize)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(R.string.qr_code),
            style = MaterialTheme.typography.titleMedium
        )

        if (qrBitmap != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.qr_code),
                modifier = Modifier.size(220.dp)
            )

        } else if (viewModel.selectedFileUri != null) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                stringResource(R.string.hotspot_preparing),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatusCard(
            message = when (viewModel.connectionStatus) {

                ConnectionStatus.STARTING_HOTSPOT ->
                    stringResource(R.string.hotspot_starting)

                ConnectionStatus.WAITING_FOR_RECEIVER ->
                    stringResource(R.string.waiting_receiver)

                ConnectionStatus.TRANSFERRING ->
                    stringResource(R.string.sending)

                ConnectionStatus.COMPLETE ->
                    stringResource(R.string.transfer_complete)

                ConnectionStatus.ERROR ->
                    viewModel.lastError ?: stringResource(R.string.error)

                else ->
                    viewModel.statusMessage.ifBlank {
                        stringResource(R.string.select_file_share_qr)
                    }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))


        // if (viewModel.connectionStatus == ConnectionStatus.WAITING_FOR_RECEIVER) { ActionButton(stringResource(R.string.start)) { viewModel.startTransfer() } }


        if (viewModel.connectionStatus == ConnectionStatus.TRANSFERRING) {

            ActionButton(stringResource(R.string.close)) {
                viewModel.cancelTransfer()
            }

        }


        if (viewModel.hotspotName.isNotBlank()) {

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(
                stringResource(R.string.ssid),
                viewModel.hotspotName
            )

            InfoRow(
                stringResource(R.string.password),
                viewModel.hotspotPassword
            )
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            if (
                viewModel.connectionStatus == ConnectionStatus.WAITING_FOR_RECEIVER ||
                viewModel.connectionStatus == ConnectionStatus.STARTING_HOTSPOT
            ) {
                viewModel.resetSession()
            }
        }
    }
}

@Composable
fun FilePicker(navController: NavHostController, viewModel: AppViewModel) {
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val (name, size) = readFileInfo(context, it)
                viewModel.setSelectedFile(it, name, size)
                viewModel.logTransfer(
                    context.getString(R.string.file_selected, name ?: "")
                )
                navController.popBackStack()
            }
        }

    ScreenScaffold(R.string.file_picker, navController) {
        InfoRow(
            stringResource(R.string.file),
            viewModel.selectedFileName ?: stringResource(R.string.not_selected)
        )

        InfoRow(
            stringResource(R.string.size),
            viewModel.formatFileSize(viewModel.selectedFileSize)
        )

        ActionButton(stringResource(R.string.select_file)) {
            launcher.launch("*/*")
        }
    }


    ScreenScaffold((R.string.file_picker), navController) {
        InfoRow(stringResource(R.string.file), viewModel.selectedFileName ?: stringResource(R.string.not_selected))
        InfoRow(stringResource(R.string.size), viewModel.formatFileSize(viewModel.selectedFileSize))
        ActionButton(stringResource(R.string.select_file)) { launcher.launch("*/*") }
    }
}

@Composable
fun FileSelector(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.file_selection), navController) {
        Text(stringResource(R.string.selected_file, viewModel.selectedFileName ?: stringResource(R.string.none)))
        Spacer(modifier = Modifier.height(16.dp))
        ActionButton(stringResource(R.string.go_file_picker)) { navController.navigate(ScreenRoutes.FILE_PICKER) }
    }
}

@Composable
fun FileInfoReader(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.file_information), navController) {
        InfoRow(stringResource(R.string.name), viewModel.selectedFileName ?: "-")
        InfoRow(stringResource(R.string.size), viewModel.formatFileSize(viewModel.selectedFileSize))
        InfoRow(stringResource(R.string.check), viewModel.fileValidationMessage)
    }
}

@Composable
fun FileValidator(navController: NavHostController, viewModel: AppViewModel) {
    LaunchedEffect(Unit) { viewModel.validateFile() }

    ScreenScaffold((R.string.file_validation), navController) {
        Text(viewModel.fileValidationMessage)
        InfoRow(stringResource(R.string.size), viewModel.formatFileSize(viewModel.selectedFileSize))
    }
}

@Composable
fun FilePermissionManager(navController: NavHostController) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    ScreenScaffold((R.string.file_permissions), navController) {
        Text(if (granted) stringResource(R.string.permission_granted) else stringResource(R.string.permission_required))
        if (!granted) ActionButton(stringResource(R.string.request_permission)) { launcher.launch(permission) }
    }
}

@Composable
fun TransferScreen(navController: NavHostController, viewModel: AppViewModel) {
    val progress = viewModel.transferProgress
    val totalSize = progress.fileSize.takeIf { it > 0 } ?: viewModel.selectedFileSize

    ScreenScaffold((R.string.transfer), navController, showBack = false) {
        TransferProgressCard(
            fileName = progress.fileName.ifBlank { viewModel.selectedFileName ?: stringResource(R.string.file) },
            fileSizeLabel = viewModel.formatFileSize(totalSize),
            transferredLabel = "${viewModel.formatFileSize(progress.bytesTransferred)} / ${viewModel.formatFileSize(totalSize)}",
            percent = progress.percent,
            speedLabel = String.format(Locale.getDefault(), "%.0f MB/s", progress.speedMbps),
            remainingTime = viewModel.getEstimatedTimeRemaining(),
            remoteDevice = progress.remoteDeviceName.ifBlank { viewModel.connectionPayload?.deviceName ?: "" }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.isTransferring) {

            ActionButton(stringResource(R.string.cancel)) {
                viewModel.cancelTransfer()
                navController.navigate(ScreenRoutes.HOME) {
                    popUpTo(ScreenRoutes.HOME) { inclusive = true }
                }
            }
        }

        if (progress.isComplete) {
            LaunchedEffect(Unit) {
                delay(500)
                navController.navigate(ScreenRoutes.SUCCESS) {
                    popUpTo(ScreenRoutes.TRANSFER) { inclusive = true }
                }
            }
        }
    }
}

@Composable
fun ReceiveScreen(navController: NavHostController, viewModel: AppViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    var connecting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ScreenScaffold((R.string.receive), navController) {
        Text(
            stringResource(R.string.scan_qr_instruction),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (hasCameraPermission && !connecting) {
            QrCameraPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                onQrScanned = { data ->
                    if (connecting) return@QrCameraPreview
                    if (viewModel.parseQrData(data)) {
                        connecting = true
                        val payload = viewModel.connectionPayload ?: return@QrCameraPreview
                        viewModel.connectAndReceive(
                            payload = payload,
                            onConnected = { },
                            onTransferStart = {
                                navController.navigate(ScreenRoutes.TRANSFER)
                            },
                            onComplete = { file ->
                                connecting = false
                                navController.navigate(
                                    if (file != null) ScreenRoutes.SUCCESS else ScreenRoutes.TRANSFER_ERROR
                                ) {
                                    popUpTo(ScreenRoutes.RECEIVE)
                                }
                            }
                        )
                    } else {
                        showToast(context, viewModel.lastError ?: context.getString(R.string.invalid_qr))
                    }
                }
            )
        } else if (!hasCameraPermission) {
            ActionButton(stringResource(R.string.grant_camera_permission)) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        StatusCard(
            message = when (viewModel.connectionStatus) {
                ConnectionStatus.CONNECTING_WIFI -> stringResource(R.string.hotspot_connecting)
                ConnectionStatus.CONNECTED -> stringResource(R.string.connected_waiting_file)
                ConnectionStatus.TRANSFERRING -> stringResource(R.string.file_receiving)
                ConnectionStatus.ERROR -> viewModel.lastError ?: stringResource(R.string.error)
                else -> if (connecting) stringResource(R.string.connecting) else stringResource(R.string.scan_qr_camera)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.manual_input),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate(ScreenRoutes.MANUAL_CONNECT)
                }
        )
    }
}

@Composable
fun QrCameraPreview(
    modifier: Modifier = Modifier,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var alreadyScanned by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        if (alreadyScanned) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        @androidx.camera.core.ExperimentalGetImage
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { value ->
                                        if (!alreadyScanned && value.isNotBlank()) {
                                            alreadyScanned = true
                                            onQrScanned(value)
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}

@Composable
fun DeviceScanner(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.device_search), navController) {
        Text(if (viewModel.isScanning) stringResource(R.string.searching) else stringResource(R.string.use_hotspot_qr))
        ActionButton(stringResource(R.string.search), enabled = !viewModel.isScanning) { viewModel.scanDevices() }

        viewModel.scannedDevices.forEach { ip ->
            ActionButton(stringResource(R.string.connect_ip, ip)) {
                viewModel.setConnectionTarget(ip)
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavHostController) {
    ScreenScaffold((R.string.settings), navController) {
        ActionButton(stringResource(R.string.theme)) { navController.navigate(ScreenRoutes.THEME) }
        ActionButton(stringResource(R.string.language)) { navController.navigate(ScreenRoutes.LANGUAGE) }
        ActionButton(stringResource(R.string.permissions)) { navController.navigate(ScreenRoutes.PERMISSION_MANAGER) }
        ActionButton(stringResource(R.string.transfer_history)) { navController.navigate(ScreenRoutes.TRANSFER_HISTORY) }
        ActionButton(stringResource(R.string.reset_settings)) { navController.navigate(ScreenRoutes.SETTINGS_RESET) }
    }
}

@Composable
fun ThemeManager(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.theme_selection), navController) {
        Text(stringResource(R.string.current, viewModel.themeMode))
        ActionButton(stringResource(R.string.system)) { viewModel.setTheme("system") }
        ActionButton(stringResource(R.string.light)) { viewModel.setTheme("light") }
        ActionButton(stringResource(R.string.dark)) { viewModel.setTheme("dark") }
    }
}

@Composable
fun LanguageManager(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.language_selection), navController) {
        Text(stringResource(R.string.current, viewModel.language))
        ActionButton(stringResource(R.string.system)) { viewModel.changeLanguage("system") }
        ActionButton(stringResource(R.string.turkish)) { viewModel.changeLanguage("tr") }
        ActionButton(stringResource(R.string.english)) { viewModel.changeLanguage("en") }
        ActionButton(stringResource(R.string.azerbaijani)) { viewModel.changeLanguage("az") }
    }
}

@Composable
fun AboutScreen(navController: NavHostController, viewModel: AppViewModel) {
    val context = LocalContext.current

    ScreenScaffold((R.string.about), navController) {
        val context = LocalContext.current
        Text("Swox Share", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.local_p2p_description))
        Spacer(modifier = Modifier.height(16.dp))
        InfoRow(stringResource(R.string.version), "1.0")
        InfoRow(stringResource(R.string.device), viewModel.getDeviceInfo())
        InfoRow(stringResource(R.string.protocol), "WiFi Hotspot + TCP")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/swox_studios")))
        }) {
            Text(stringResource(R.string.support_kofi))
        }
    }
}

@Composable
fun QRGenerator(navController: NavHostController, viewModel: AppViewModel) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.qrContent) {
        if (viewModel.qrContent.isBlank()) {
            bitmap = null
            return@LaunchedEffect
        }

        bitmap = withContext(Dispatchers.Default) {
            try {
                QrUtils.generateQrBitmap(viewModel.qrContent)
            } catch (_: Exception) {
                null
            }
        }

        error = bitmap == null
    }

    ScreenScaffold((R.string.qr_code), navController) {
        bitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = stringResource(R.string.qr_code), modifier = Modifier.size(200.dp))
        } ?: Text(if (error) stringResource(R.string.qr_failed) else stringResource(R.string.loading))

        Text(viewModel.qrContent, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun HotspotInfo(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.hotspot_info), navController) {
        InfoRow(stringResource(R.string.hotspot), viewModel.hotspotName.ifBlank { "-" })
        InfoRow(stringResource(R.string.ip), viewModel.localIp)
        InfoRow(stringResource(R.string.port), NetworkUtils.TRANSFER_PORT.toString())
    }
}

@Composable
fun ManualInput(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.manual_input), navController) {
        OutlinedTextField(
            value = viewModel.hotspotName,
            onValueChange = { viewModel.hotspotName = it },
            label = { Text(stringResource(R.string.hotspot_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = viewModel.hotspotPassword,
            onValueChange = { viewModel.hotspotPassword = it },
            label = { Text(stringResource(R.string.hotspot_password)) },
            modifier = Modifier.fillMaxWidth()
        )

        ActionButton(stringResource(R.string.save)) { navController.popBackStack() }
    }
}

@Composable
fun QRScanner(navController: NavHostController, viewModel: AppViewModel) {
    ReceiveScreen(navController, viewModel)
}

@Composable
fun QRScannerCheck(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.qr_check), navController) {
        Text(
            if (viewModel.connectionTargetIp.isNotBlank())
                stringResource(R.string.qr_success)
            else
                stringResource(R.string.qr_failed_read)
        )

        InfoRow(stringResource(R.string.ip), viewModel.connectionTargetIp.ifBlank { "-" })
        InfoRow(stringResource(R.string.device), viewModel.connectionPayload?.deviceName ?: "-")
    }
}

@Composable
fun ManualConnect(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold((R.string.manual_input), navController) {

        OutlinedTextField(
            value = viewModel.hotspotName,
            onValueChange = { viewModel.hotspotName = it },
            label = { Text(stringResource(R.string.ssid)) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = viewModel.hotspotPassword,
            onValueChange = { viewModel.hotspotPassword = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.fillMaxWidth()
        )

        ActionButton(stringResource(R.string.ok)) {
            viewModel.connectManual(
                ssid = viewModel.hotspotName,
                password = viewModel.hotspotPassword,
                onTransferStart = {
                    navController.navigate(ScreenRoutes.TRANSFER)
                },
                onComplete = { file ->
                    navController.navigate(
                        if (file != null) ScreenRoutes.SUCCESS
                        else ScreenRoutes.TRANSFER_ERROR
                    )
                }
            )
        }
    }
}

@Composable
fun ConnectionManager(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold(R.string.connection_manager, navController) {
        InfoRow(
            stringResource(R.string.target),
            viewModel.connectionTargetIp.ifBlank {
                stringResource(R.string.not_set)
            }
        )
        InfoRow("IP", viewModel.localIp)
    }
}

@Composable
fun FileTransfer(navController: NavHostController, viewModel: AppViewModel) {
    TransferScreen(navController, viewModel)
}

@Composable
fun ProgressTracker(navController: NavHostController, viewModel: AppViewModel) {
    TransferScreen(navController, viewModel)
}

@Composable
fun SpeedCalculator(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold(R.string.speed, navController) {
        Text(
            String.format(
                Locale.getDefault(),
                "%.2f MB/s",
                viewModel.transferProgress.speedMbps
            )
        )
    }
}

@Composable
fun TimeRemainingCalculator(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold(R.string.remaining_time, navController) {
        Text(viewModel.getEstimatedTimeRemaining())
    }
}

@Composable
fun TransferStatus(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold(R.string.transfer_status, navController) {
        InfoRow(
            stringResource(R.string.status),
            viewModel.transferProgress.status
        )

        InfoRow(
            stringResource(R.string.percent),
            "${viewModel.transferProgress.percent.toInt()}%"
        )
    }
}

@Composable
fun TransferHistory(navController: NavHostController, viewModel: AppViewModel) {
    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }

    ScreenScaffold(R.string.transfer_history, navController) {

        LogList(
            viewModel.transferHistory,
            stringResource(R.string.no_transfers)
        )

        ActionButton(stringResource(R.string.clear)) {
            viewModel.clearHistory()
        }
    }
}

@Composable
fun TransferError(navController: NavHostController, viewModel: AppViewModel) {
    ScreenScaffold(R.string.transfer_error, navController) {

        Text(
            viewModel.lastError
                ?: viewModel.transferProgress.error
                ?: stringResource(R.string.unknown_error)
        )

        ActionButton(stringResource(R.string.retry)) {
            viewModel.resetSession()
            navController.popBackStack()
        }

        ActionButton(stringResource(R.string.home)) {
            viewModel.resetSession()
            navController.navigate(ScreenRoutes.HOME) {
                popUpTo(ScreenRoutes.HOME)
            }
        }
    }
}

@Composable
fun SwoxShareTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}

private fun readFileInfo(context: Context, uri: Uri): Pair<String?, Long> {
    var name: String? = null
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex >= 0) name = cursor.getString(nameIndex)
            if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
    }
    return name to size
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun vibrateShort(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(100)
        }
    } catch (_: Exception) {
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("swox", text))
}

fun currentDateTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}
