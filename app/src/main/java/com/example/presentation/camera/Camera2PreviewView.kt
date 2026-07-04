package com.example.presentation.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Camera2PreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textureView: TextureView = TextureView(context)
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var cameraId: String? = null
    private var previewSize: Size = Size(1920, 1080)
    private var captureSize: Size = Size(1920, 1080)

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock = Semaphore(1)

    private var onCapturedCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private var isFlashSupported: Boolean = false
    private var isTorchOn: Boolean = false

    init {
        addView(textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                setupCamera(width, height)
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                closeCamera()
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }
    }

    private fun setupCamera(width: Int, height: Int) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue // Skip front camera
                }

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue
                
                // Choose JPEG size (largest or a standard 1080p target size)
                val jpegSizes = map.getOutputSizes(ImageFormat.JPEG)
                captureSize = jpegSizes?.firstOrNull { it.width <= 2560 && it.height <= 1920 } 
                    ?: jpegSizes?.firstOrNull() 
                    ?: Size(1920, 1080)

                // Choose Preview Size
                val previewSizes = map.getOutputSizes(SurfaceTexture::class.java)
                previewSize = previewSizes?.firstOrNull { it.width <= width && it.height <= height }
                    ?: previewSizes?.firstOrNull { it.width <= 1920 && it.height <= 1080 }
                    ?: Size(1920, 1080)

                val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                isFlashSupported = flashAvailable ?: false

                cameraId = id
                break
            }
        } catch (e: CameraAccessException) {
            onErrorCallback?.invoke("Camera access error: ${e.message}")
        }
    }

    private fun openCamera() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId ?: return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            onErrorCallback?.invoke("CAMERA permission not granted")
            return
        }

        startBackgroundThread()

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraOpenCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    onErrorCallback?.invoke("Camera open error code: $error")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            onErrorCallback?.invoke("Failed to open camera: ${e.message}")
        }
    }

    private fun createCameraPreviewSession() {
        val device = cameraDevice ?: return
        val texture = textureView.surfaceTexture ?: return

        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val surface = Surface(texture)

        imageReader = ImageReader.newInstance(captureSize.width, captureSize.height, ImageFormat.JPEG, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()
                saveImage(bytes)
            }, backgroundHandler)
        }

        try {
            val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                updateFlashRequest(this)
            }

            device.createCaptureSession(
                listOf(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            onErrorCallback?.invoke("Session start repeating request failed: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onErrorCallback?.invoke("Capture session configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            onErrorCallback?.invoke("Create capture session failed: ${e.message}")
        }
    }

    private fun updateFlashRequest(requestBuilder: CaptureRequest.Builder) {
        if (isFlashSupported) {
            if (isTorchOn) {
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            } else {
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
        }
    }

    fun toggleFlash(onToggleCompleted: (Boolean) -> Unit) {
        if (!isFlashSupported) {
            onToggleCompleted(false)
            return
        }
        isTorchOn = !isTorchOn
        val session = captureSession ?: return
        val device = cameraDevice ?: return
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)

        try {
            val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                updateFlashRequest(this)
            }
            session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
            onToggleCompleted(isTorchOn)
        } catch (e: CameraAccessException) {
            isTorchOn = !isTorchOn // Revert state on fail
            onToggleCompleted(isTorchOn)
        }
    }

    fun capturePhoto(onCaptured: (String) -> Unit, onError: (String) -> Unit) {
        this.onCapturedCallback = onCaptured
        this.onErrorCallback = onError

        val device = cameraDevice ?: run {
            onError("Camera device is null")
            return
        }
        val session = captureSession ?: run {
            onError("Capture session is null")
            return
        }
        val reader = imageReader ?: run {
            onError("Image reader is null")
            return
        }

        try {
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                updateFlashRequest(this)
            }

            session.stopRepeating()
            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    // Restart preview
                    try {
                        val texture = textureView.surfaceTexture ?: return
                        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
                        val surface = Surface(texture)
                        val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(surface)
                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            updateFlashRequest(this)
                        }
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        onErrorCallback?.invoke("Failed to restart preview: ${e.message}")
                    }
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    onErrorCallback?.invoke("Photo capture failed: ${failure.reason}")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            onError("Camera capture access exception: ${e.message}")
        }
    }

    private fun saveImage(bytes: ByteArray) {
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }
        val file = File(receiptsDir, "receipt_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            // Trigger callback on main thread
            Handler(Looper.getMainLooper()).post {
                onCapturedCallback?.invoke(file.absolutePath)
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onErrorCallback?.invoke("Failed to save receipt image: ${e.message}")
            }
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread != null) return
        backgroundThread = HandlerThread("CameraBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
            stopBackgroundThread()
        }
    }
}
