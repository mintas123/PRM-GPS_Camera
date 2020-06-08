package pl.pjatk.s16604.project2

import android.Manifest
import android.Manifest.permission.CAMERA
import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_camera.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.*


class CameraActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        private val TAG = this::class.qualifiedName
    }

    private val MAX_PREVIEW_WIDTH = 4032
    private val MAX_PREVIEW_HEIGHT = 3024

    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraDevice: CameraDevice
    private val deviceStateCallback =  object: CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice) {

            Log.d(TAG,"camera device opened")
            if (camera !=null){
                cameraDevice = camera
                previewSession()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG,"camera device disconnected")
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG,"camera device error")
            this@CameraActivity.finish()
        }

    }
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private val cameraManager by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private fun previewSession(){
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture.setDefaultBufferSize(MAX_PREVIEW_WIDTH,MAX_PREVIEW_HEIGHT)
        val surface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(listOf(surface),object: CameraCaptureSession.StateCallback(){
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG,"creating capture session failed")            }

            override fun onConfigured(session: CameraCaptureSession) {
                if (session != null){
                    captureSession = session
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(),null,null)
                }
            }

        },null)
    }

    private fun closeCamera() {
        if (this::captureSession.isInitialized) {
            captureSession.close()
        }
        if (this::cameraDevice.isInitialized){
            cameraDevice.close()
        }

    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera2 Kotlin").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }
    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException){
            Log.e(TAG,e.toString())
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (textureView.isAvailable)
            openCamera()
        else
            textureView.surfaceTextureListener = surfaceListener
    }

    override fun onPause() {
        closeCamera()
        super.onPause()
        stopBackgroundThread()
    }
    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            else -> throw IllegalArgumentException("Key not recognised")
        }!!
    }
    private fun cameraId(lens: Int): String {
        var deviceId = listOf<String>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            var string = "XXXX"
            cameraIdList.forEach { string = "$string $it; " }
            Log.d(TAG, string)
            deviceId = cameraIdList.filter {
                lens == cameraCharacteristics(
                    it,
                    CameraCharacteristics.LENS_FACING
                )
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }

    private fun connectCamera() {
        val deviceId = cameraId(CameraCharacteristics.LENS_FACING_BACK)
        Log.d(TAG, "XXXXXXXXXXXXXXXdevicesId: $deviceId")
        try {
            cameraManager.openCamera(deviceId,deviceStateCallback,backgroundHandler)
        }catch (e: CameraAccessException){
            Log.e(TAG, e.toString())
        }catch (e: InterruptedException){
            Log.e(TAG, "Open camera device interrupted while opened")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun checkCameraPermission() {
        if (EasyPermissions.hasPermissions(this, CAMERA)) {
            Log.d(TAG, "APP HAS CAMERA PERMISSION")
            connectCamera()
        } else {
//            showPermissionAlert()
            EasyPermissions.requestPermissions(
                this, "Camera app requires camera access",
                REQUEST_CAMERA_PERMISSION, CAMERA
            )
        }
    }
    private fun openCamera() {
        checkCameraPermission()
        // todo
    }
    private val surfaceListener = object: TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "width: $width height: $height")
            openCamera()
        }
    }
}








//
//    fun mark(
//        src: Bitmap,
//        watermark: String,
//        location: Point,
//        color: Int,
//        alpha: Int,
//        size: Float,
//        underline: Boolean
//    ): Bitmap? {
//        val w = src.width
//        val h = src.height
//        val result = Bitmap.createBitmap(w, h, src.config)
//        val canvas = Canvas(result)
//        canvas.drawBitmap(src, 0, 0, null)
//        val paint = Paint()
//        paint.setColor(color)
//        paint.setAlpha(alpha)
//        paint.setTextSize(size)
//        paint.setAntiAlias(true)
//        paint.setUnderlineText(underline)
//        canvas.drawText(watermark, location.x.toFloat(), location.y.toFloat(), paint)
//        return result
//    }

//    private fun showPermissionAlert() {
//        val alertBuilder = AlertDialog.Builder(this)
//        alertBuilder.setCancelable(true)
//        alertBuilder.setTitle("Permission Required")
//        alertBuilder.setMessage("Permission to access camera is needed to run this application")
//        alertBuilder.setPositiveButton(android.R.string.yes) { _, _ ->
//            ActivityCompat.requestPermissions(
//                this@CameraActivity,
//                arrayOf(CAMERA),
//                REQUEST_CAMERA_PERMISSION
//            )
//        }
//        val alert = alertBuilder.create()
//        alert.show()
//    }