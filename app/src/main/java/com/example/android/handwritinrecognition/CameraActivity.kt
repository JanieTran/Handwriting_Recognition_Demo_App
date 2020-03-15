package com.example.android.handwritinrecognition

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_camera.*
import java.util.*
import android.content.Intent
import android.graphics.*
import android.view.View
import java.io.ByteArrayOutputStream
import java.io.OutputStream


class CameraActivity : AppCompatActivity() {

    //-----------------------------------------------------------------------------------
    // PROPERTIES
    //-----------------------------------------------------------------------------------

    // Log tag
    private val TAG: String = "CAMERA_ACTIVITY"

    // Screen size
    lateinit var screenSize: Point

    // Camera
    lateinit var cameraManager: CameraManager
    lateinit var cameraDevice: CameraDevice
    lateinit var cameraId: String
    lateinit var cameraStateCallback: CameraDevice.StateCallback
    lateinit var cameraRequestBuilder: CaptureRequest.Builder
    lateinit var cameraCaptureRequest: CaptureRequest
    lateinit var cameraCaptureSession: CameraCaptureSession

    // Surface texture
    lateinit var surfaceTextureListener: TextureView.SurfaceTextureListener
    lateinit var previewSize: Size

    // Background handler
    lateinit var backgroundThread: HandlerThread
    lateinit var backgroundHandler: Handler

    //-----------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Get size of screen
        screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        Log.i(TAG, "Screen width ${screenSize.x} ${screenSize.y}")

        configureCamera()
        resizeShadowBoxesWidth()

        // Capture button
        btn_capture.setOnClickListener {
            val capturedImage = txv_camera_preview.bitmap

            val capturedHeight = screenSize.x / 4f
            val shadowHeight = ((screenSize.x - capturedHeight) / 2).toInt()

            val startX = shadowHeight + (shadowHeight / 2)
            val startY = 0
            val width = capturedHeight.toInt()
            val height = capturedImage.height

            Log.i(TAG, "Bitmap size ${capturedImage.width} ${capturedImage.height}")
            Log.i(TAG, "Crop size $startX $startY $width $height")

            val resizedBitmap = Bitmap.createBitmap(capturedImage, startX, startY, width, height)

            val byteStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 30, byteStream as OutputStream?)

            val readIntent = Intent(this, ReadActivity::class.java)
            readIntent.putExtra("CapturedImage", byteStream.toByteArray())
            readIntent.putExtra("Width", width)
            readIntent.putExtra("Height", height)
            startActivity(readIntent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        openBackgroundThread()
        if (txv_camera_preview.isAvailable) {
            setUpCamera()
            openCamera()
        } else {
            txv_camera_preview.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
        closeBackgroundThread()
    }

    //-----------------------------------------------------------------------------------
    // Activity initialisation
    //-----------------------------------------------------------------------------------

    private fun configureCamera() {
        // Camera
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Surface Texture
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
                Log.i(TAG, "Surface Texture available")
                setUpCamera()
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture?, width: Int, height: Int) {}

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {}

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
                return true
            }
        }

        // Camera state callback
        cameraStateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                createPreviewSession()
            }

            override fun onDisconnected(p0: CameraDevice) {
                cameraDevice.close()
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                cameraDevice.close()
            }
        }
    }

    //-----------------------------------------------------------------------------------
    // Camera configuration
    //-----------------------------------------------------------------------------------

    private fun setUpCamera() {
        try {
            for (id in cameraManager.cameraIdList) {
                val cameraChar = cameraManager.getCameraCharacteristics(id)
                if (cameraChar.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    val streamConfig = cameraChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    // Chooose resolution that match screen size
                    for (size in streamConfig.getOutputSizes(SurfaceTexture::class.java)) {
                        Log.i(TAG, "Resolution ${size.width} ${size.height}")
                        if (size.height == screenSize.x) {
                            previewSize = size
                            break
                        }
                    }
                    cameraId = id
                    break
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException ${e.reason}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera permission denied ${e.reason}")
        }
    }

    private fun closeCamera() {
        cameraCaptureSession.close()
        cameraDevice.close()
    }

    //-----------------------------------------------------------------------------------
    // Background thread handling
    //-----------------------------------------------------------------------------------

    private fun openBackgroundThread() {
        backgroundThread = HandlerThread("Camera_Background")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun closeBackgroundThread() {
        backgroundThread.quitSafely()
    }

    //-----------------------------------------------------------------------------------
    // Camera Preview Session
    //-----------------------------------------------------------------------------------

    private fun createPreviewSession() {
        try {
            val surfaceTexture = txv_camera_preview.surfaceTexture
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface = Surface(surfaceTexture)

            cameraRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            cameraRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            cameraRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(captureSession: CameraCaptureSession) {
                    try {
                        cameraCaptureRequest = cameraRequestBuilder.build()
                        cameraCaptureSession = captureSession
                        cameraCaptureSession.setRepeatingRequest(cameraCaptureRequest, null, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, "CameraAccessException: ${e.reason}")
                    }
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {
                    Log.e(TAG, "Camera Device failed to create Capture Session.")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException: ${e.reason}")
        }
    }

    //-----------------------------------------------------------------------------------
    // Camera Preview Canvas
    //-----------------------------------------------------------------------------------

    private fun resizeShadowBoxesHeight() {
        val capturedHeight = screenSize.x / 3.5f
        val shadowHeight = (screenSize.y - capturedHeight) / 2
        Log.i(TAG, "previewSize width ${screenSize.x} capturedHeight $capturedHeight shadowHeight $shadowHeight")

        v_first_shadow.layoutParams.height = shadowHeight.toInt()
        v_first_shadow.requestLayout()
        v_second_shadow.layoutParams.height = shadowHeight.toInt()
        v_second_shadow.requestLayout()
    }

    private fun resizeShadowBoxesWidth() {
        val capturedHeight = screenSize.x / 4f
        val shadowHeight = ((screenSize.x - capturedHeight) / 2).toInt()
        Log.i(TAG, "previewSize width ${screenSize.y} capturedHeight $capturedHeight shadowHeight $shadowHeight")

        v_first_shadow.layoutParams.width = shadowHeight + (shadowHeight / 2)
        v_first_shadow.requestLayout()
        v_second_shadow.layoutParams.width = shadowHeight - (shadowHeight / 2)
        v_second_shadow.requestLayout()
    }
}
