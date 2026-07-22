package com.swox.share

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket

data class TransferProgress(
    val percent: Float = 0f,
    val speedMbps: Double = 0.0,
    val status: String = "",
    val error: String? = null,
    val isComplete: Boolean = false,
    val fileName: String = "",
    val fileSize: Long = 0L,
    val bytesTransferred: Long = 0L,
    val remoteDeviceName: String = "",
    val remainingSeconds: Int = -1
)

class TransferEngine(private val context: Context) {

    companion object {
        const val PROTOCOL_MAGIC = "SWOX"
        const val PROTOCOL_VERSION = 1
        const val APP_VERSION = "1.0"
    }

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var activeSocket: Socket? = null

    @Volatile
    private var cancelled = false

    @Volatile
    private var paused = false 

    var progress = TransferProgress(status = context.getString(R.string.status_waiting))
        private set

    private var onProgress: ((TransferProgress) -> Unit)? = null

    fun setProgressCallback(callback: (TransferProgress) -> Unit) {
        onProgress = callback
    }

    private fun updateProgress(update: TransferProgress) {
        progress = update

        Handler(Looper.getMainLooper()).post {
            onProgress?.invoke(update)
        }
    }

    suspend fun startSenderServer(
        localDeviceName: String,
        onClientConnected: () -> Unit,
        onReady: () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            stopInternal()
            cancelled = false
            paused = false
            updateProgress(TransferProgress(status = context.getString(R.string.status_starting_server)))
            serverSocket = ServerSocket(NetworkUtils.TRANSFER_PORT)
            Log.d("SWOX", "Server başladı, client gözlənilir...")
            updateProgress(TransferProgress(status = context.getString(R.string.status_waiting_device)))
            onReady()

            val client = serverSocket!!.accept()
            Log.d("SWOX", "Client qoşuldu!")
            if (cancelled) {
                client.close()
                return@withContext
            }
            activeSocket = client
            onClientConnected()
            Log.d("SWOX", "onClientConnected çağırıldı")
            updateProgress(TransferProgress(status = context.getString(R.string.connected_sending_file)))
        } catch (e: Exception) {
            if (!cancelled) {
                updateProgress(TransferProgress(status = context.getString(R.string.error), error = e.message))
            }
            throw e
        }
    }
    suspend fun sendFileAfterConnect(
        uri: Uri,
        localDeviceName: String,
        onComplete: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        Log.d("SWOX", "sendFileAfterConnect başladı")
        val socket = activeSocket
        if (socket == null || socket.isClosed) {
            updateProgress(TransferProgress(status = context.getString(R.string.error), error = context.getString(R.string.status_no_active_connection)))
            onComplete(false)
            return@withContext
        }
        try {
            val fileName = getFileName(uri) ?: "file"
            val fileSize = getFileSize(uri)
            updateProgress(
                TransferProgress(
                    fileName = fileName,
                    fileSize = fileSize,
                    status = context.getString(R.string.sending)
                )
            )

            val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            Log.d("SWOX", "Header göndərilir...")
            output.writeUTF(PROTOCOL_MAGIC)
            output.writeInt(PROTOCOL_VERSION)
            output.writeUTF(APP_VERSION)

            output.writeUTF(localDeviceName)
            output.writeUTF(fileName)
            output.writeLong(fileSize)
            output.flush()
            Log.d("SWOX", "Header göndərildi")

            context.contentResolver.openInputStream(uri)?.use { input ->
                streamWithProgress(input, output, fileSize, fileName, context.getString(R.string.sending))
                Log.d("SWOX", "Fayl göndərilməsi bitdi")
            } ?: throw IllegalStateException(context.getString(R.string.file_read_failed))

            output.flush()
            updateProgress(
                TransferProgress(
                    percent = 100f,
                    fileName = fileName,
                    fileSize = fileSize,
                    bytesTransferred = fileSize,
                    status = context.getString(R.string.status_completed),
                    isComplete = true
                )
            )
            onComplete(true)
        } catch (e: Exception) {
            if (!cancelled) {
                updateProgress(TransferProgress(status = context.getString(R.string.error), error = e.message))
                Log.e("SWOX", "Transfer xətası", e)
            }
            onComplete(false)
        } finally {
            stopInternal()
        }
    }
    suspend fun receiveFromSender(
        host: String,
        onComplete: (File?) -> Unit
    ) = withContext(Dispatchers.IO) {
        Log.d("SWOX", "Receiver başladı")
        try {
            cancelled = false
            updateProgress(TransferProgress(status = context.getString(R.string.connecting)))
            val realHost =  host

            val socket = Socket()

            socket.connect(
                java.net.InetSocketAddress(
                    realHost,
                    NetworkUtils.TRANSFER_PORT
                ),
                8000
            )
            Log.d("SWOX", "Socket qoşuldu")

            socket.soTimeout = 15000
            activeSocket = socket

            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            Log.d("SWOX", "Header gözlənilir...")
            val magic = input.readUTF()

            if (magic != PROTOCOL_MAGIC) {
                throw IllegalStateException("Unknown Swox protocol")
            }

            val protocolVersion = input.readInt()
            val remoteVersion = input.readUTF()

            val remoteDevice = input.readUTF()
            val fileName = input.readUTF()
            val fileSize = input.readLong()
            Log.d("SWOX","Protocol=$protocolVersion Version=$remoteVersion Device=$remoteDevice File=$fileName Size=$fileSize")

            updateProgress(
                TransferProgress(
                    fileName = fileName,
                    fileSize = fileSize,
                    remoteDeviceName = remoteDevice,
                    status = context.getString(R.string.receiving)
                )
            )

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/Swox Share"
                )
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw Exception(context.getString(R.string.file_create_failed))

            val destFile = File(fileName)
            Log.d("SWOX", "Fayl yazılmağa başlayır")

            context.contentResolver.openOutputStream(uri)?.use { fos ->
                val buffer = ByteArray(8192)
                var totalRead = 0L
                var lastTime = System.currentTimeMillis()
                var lastBytes = 0L

                while (totalRead < fileSize && !cancelled) {
                    val toRead = minOf(buffer.size.toLong(), fileSize - totalRead).toInt()
                    val read = input.read(buffer, 0, toRead)
                    if (read == -1) break
                    fos.write(buffer, 0, read)
                    totalRead += read

                    val now = System.currentTimeMillis()
                    if (now - lastTime >= 400) {
                        val elapsed = (now - lastTime) / 1000.0
                        val bytesDelta = totalRead - lastBytes
                        val speed = if (elapsed > 0) (bytesDelta / elapsed) / (1024 * 1024) else 0.0
                        val remaining = if (speed > 0) {
                            ((fileSize - totalRead) / (speed * 1024 * 1024)).toInt()
                        } else -1
                        updateProgress(
                            TransferProgress(
                                percent = if (fileSize > 0) (totalRead.toFloat() / fileSize) * 100f else 0f,
                                speedMbps = speed,
                                fileName = fileName,
                                fileSize = fileSize,
                                bytesTransferred = totalRead,
                                remoteDeviceName = remoteDevice,
                                remainingSeconds = remaining,
                                status = context.getString(R.string.receiving)
                            )
                        )
                        lastTime = now
                        lastBytes = totalRead
                    }
                }
            }
            Log.d("SWOX", "Fayl uğurla qəbul edildi")

            if (cancelled) {
                onComplete(null)
                return@withContext
            }

            updateProgress(
                TransferProgress(
                    percent = 100f,
                    fileName = fileName,
                    fileSize = fileSize,
                    bytesTransferred = fileSize,
                    remoteDeviceName = remoteDevice,
                    status = context.getString(R.string.status_completed),
                    isComplete = true
                )
            )
            onComplete(destFile)
        } catch (e: Exception) {
            if (!cancelled) {
                updateProgress(TransferProgress(status = context.getString(R.string.error), error = e.message))
                Log.e("SWOX", "Transfer xətası", e)
            }
            onComplete(null)
        } finally {
            stopInternal()
        }
    }

    private fun streamWithProgress(
        input: java.io.InputStream,
        output: DataOutputStream,
        fileSize: Long,
        fileName: String,
        status: String
    ) {
        val buffer = ByteArray(8192)
        var totalSent = 0L
        var lastTime = System.currentTimeMillis()
        var lastBytes = 0L

        while (!cancelled) {
            while (paused && !cancelled) {
                Thread.sleep(150)
            }
            if (cancelled) break

            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            totalSent += read

            val now = System.currentTimeMillis()
            if (now - lastTime >= 400) {
                val elapsed = (now - lastTime) / 1000.0
                val bytesDelta = totalSent - lastBytes
                val speed = if (elapsed > 0) (bytesDelta / elapsed) / (1024 * 1024) else 0.0
                val remaining = if (speed > 0) {
                    ((fileSize - totalSent) / (speed * 1024 * 1024)).toInt()
                } else -1
                updateProgress(
                    TransferProgress(
                        percent = if (fileSize > 0) (totalSent.toFloat() / fileSize) * 100f else 0f,
                        speedMbps = speed,
                        fileName = fileName,
                        fileSize = fileSize,
                        bytesTransferred = totalSent,
                        remainingSeconds = remaining,
                        status = status
                    )
                )
                lastTime = now
                lastBytes = totalSent
            }
        }

    }

    fun stop() {
        cancelled = true
        stopInternal()
        updateProgress(TransferProgress(status = context.getString(R.string.status_stopped)))
    }

    private fun stopInternal() {
        try {
            activeSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {
        }
        activeSocket = null
        serverSocket = null
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment
    }

    private fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                val size = cursor.getLong(sizeIndex)
                if (size > 0) return size
            }
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            var total = 0L
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                total += read
            }
            total
        } ?: 0L
    }

    fun pause() {
        paused = true
        updateProgress(progress.copy(status = context.getString(R.string.status_stopped)))
    }

    fun resume() {
        paused = false
        updateProgress(progress.copy(status = context.getString(R.string.sending)))
    }

}
