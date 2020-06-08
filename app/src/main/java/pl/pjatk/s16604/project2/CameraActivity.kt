package pl.pjatk.s16604.project2

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.ImageReader
import android.media.MediaActionSound
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_camera.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque


class CameraActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        const val REQUEST_GPS_PERMISSION = 101
        private val TAG = this::class.qualifiedName
    }

    private var CURRENT_CAMERA: Int = 0

    private val MAX_PREVIEW_WIDTH = 4032
    private val MAX_PREVIEW_HEIGHT = 3024


    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraDevice: CameraDevice
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private var captureImageRequestBuilder: CaptureRequest.Builder? = null
    private val captureResults: BlockingQueue<CaptureResult> = LinkedBlockingDeque()
    private val saveImageExecutor: Executor = Executors.newSingleThreadExecutor()
    private var jpegImageReader: ImageReader? = null



    private val cameraManager by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {

            Log.d(TAG, "camera device opened")
            cameraDevice = camera
            previewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "camera device disconnected")
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "camera device error")
            this@CameraActivity.finish()
        }

    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
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
            openCamera(CameraCharacteristics.LENS_FACING_BACK)
        }
    }


    private fun previewSession() {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val surface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating capture session failed")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                }

            },
            null
        )
    }

    private fun closeCamera() {
        if (this::captureSession.isInitialized) {
            captureSession.close()
        }
        if (this::cameraDevice.isInitialized) {
            cameraDevice.close()
        }

    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera2").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        onChangeCamera()
        onGallery()
        onTakePicture()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (textureView.isAvailable)
            openCamera(CameraCharacteristics.LENS_FACING_BACK)
        else
            textureView.surfaceTextureListener = surfaceListener
    }

    override fun onPause() {
        closeCamera()
        super.onPause()
        stopBackgroundThread()
    }


    private fun onChangeCamera() {
        changeCameraBtn.setOnClickListener {
            animate(this, changeCameraBtn)
            closeCamera()
            if (CURRENT_CAMERA == 0) {
                openCamera(CameraCharacteristics.LENS_FACING_FRONT)
                CURRENT_CAMERA = 1
            } else {
                openCamera(CameraCharacteristics.LENS_FACING_BACK)
                CURRENT_CAMERA = 0
            }
        }
    }

    private fun onGallery() {
        galleryBtn.setOnClickListener {

            animate(this, galleryBtn)
            val intent = Intent(this, PhotoListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun onTakePicture() {
        TakePicBtn.setOnClickListener {
            Log.d(TAG, "picture request")

            takePicture()

        }
    }

    //CAMERA SETUP PART
    lateinit var currentCameraCharacteristics: CameraCharacteristics
    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
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
        currentCameraCharacteristics = cameraManager.getCameraCharacteristics(deviceId[0])
        return deviceId[0]
    }

    @SuppressLint("MissingPermission")
    private fun connectCamera(cameraFacing: Int) {
        val deviceId = cameraId(cameraFacing)
        Log.d(TAG, "XXXXXXXXXXXXXXXdevicesId: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            Log.e(TAG, "Open camera device interrupted while opened")
        }
    }


    private fun openCamera(cameraFacing: Int) {
        checkCameraPermission(cameraFacing)
    }

    //PERMISSIONS PART
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun checkCameraPermission(cameraFacing: Int) {
        if (EasyPermissions.hasPermissions(this, CAMERA) &&
            EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION)) {
            Log.d(TAG, "APP HAS CAMERA ADN GPS PERMISSION")
            connectCamera(cameraFacing)
        } else {
            showPermissionAlert()
            if (!EasyPermissions.hasPermissions(this, CAMERA)) {
                EasyPermissions.requestPermissions(
                    this, "Camera app requires camera access",
                    REQUEST_CAMERA_PERMISSION, CAMERA
                )
                }
            if (!EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION)) {
                EasyPermissions.requestPermissions(
                    this, "This app requires GPS access",
                    REQUEST_GPS_PERMISSION, ACCESS_FINE_LOCATION
                )
            }

        }
    }

    private fun showPermissionAlert() {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission Required")
        alertBuilder.setMessage("Permission to access camera and GPS is needed to run this application")
        alertBuilder.setPositiveButton(android.R.string.yes) { _, _ ->
            ActivityCompat.requestPermissions(
                this@CameraActivity,
                arrayOf(CAMERA,ACCESS_FINE_LOCATION),
                REQUEST_CAMERA_PERMISSION
            )
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    // PICTURE TAKEN PART
    private fun takePicture() {
        val orientation =
            getJpegOrientation(currentCameraCharacteristics, resources.configuration.orientation)
        captureImageRequestBuilder?.set(CaptureRequest.JPEG_ORIENTATION, orientation)
        val location = getLocation()
        val address = getAddress(LatLng(location!!.latitude, location.longitude))
        captureImageRequestBuilder?.set(CaptureRequest.JPEG_GPS_LOCATION, location)
        captureImageRequestBuilder?.set(CaptureRequest.JPEG_QUALITY, 100)

        Log.d(TAG,"XXXxxxXXX LAT: ${location.latitude} LON: ${location.longitude}")
        Log.d(TAG,"XXXxxxXXX ADDRESS: $address")
        Log.d(TAG,"XXXxxxXXX $orientation")

        val captureImageRequest = captureImageRequestBuilder?.build()
//        if (captureImageRequest != null) {
//            captureSession.capture(
//                captureImageRequest,
//                CaptureImageStateCallback(),
//                backgroundHandler
//            )
//        }


    }

    private fun getLocation(): Location? {
        val locationManager = getSystemService(LocationManager::class.java)
        if (locationManager != null && ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        }
        return null
    }
    private fun getAddress(latLng: LatLng): String {
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
//        val address: Address?
        var addressText = ""

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val sublocality = addresses[0].subLocality
                val locality= addresses[0].locality
                addressText = "$sublocality, $locality"
            }
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }

        return addressText
    }

    private fun getJpegOrientation(
        cameraCharacteristics: CameraCharacteristics,
        deviceOrientation: Int
    ): Int {
        var myDeviceOrientation = deviceOrientation
        if (myDeviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0
        }
        val sensorOrientation =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        myDeviceOrientation = (myDeviceOrientation + 45) / 90 * 90

        val facingFront =
            cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) {
            myDeviceOrientation = -myDeviceOrientation
        }

        return (sensorOrientation + myDeviceOrientation + 360) % 360
    }

//    private inner class CaptureImageStateCallback : CameraCaptureSession.CaptureCallback() {
//
//        override fun onCaptureCompleted(
//            session: CameraCaptureSession,
//            request: CaptureRequest,
//            result: TotalCaptureResult
//        ) {
//            super.onCaptureCompleted(session, request, result)
//            captureResults.put(result)
//            jpegImageReader = ImageReader.newInstance(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, ImageFormat.JPEG, 5)
//
//            val dateFormat: DateFormat =
//                SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
//            val folder: File = filesDir
//            val file = File(folder, "GPS_Camera")
//            val image = jpegImageReader.acquireLatestImage()
//            val captureResult = captureResults.take()
//            val jpegByteBuffer = image.planes[0].buffer// Jpeg image data only occupy the planes[0].
//            val jpegByteArray = ByteArray(jpegByteBuffer.remaining())
//            jpegByteBuffer.get(jpegByteArray)
//            val width = image.width
//            val height = image.height
//            saveImageExecutor.execute {
//                val date = System.currentTimeMillis()
//                val title = "IMG_${dateFormat.format(date)}"// e.g. IMG_20190211100833786
//                val displayName = "$title.jpeg"// e.g. IMG_20190211100833786.jpeg
//                val path =
//                    "${file.absolutePath}/$displayName"// e.g. /sdcard/DCIM/Camera/IMG_20190211100833786.jpeg
//                val orientation = captureResult[CaptureResult.JPEG_ORIENTATION]
//                val location = captureResult[CaptureResult.JPEG_GPS_LOCATION]
//                val longitude = location?.longitude ?: 0.0
//                val latitude = location?.latitude ?: 0.0
//
//                // Write the jpeg data into the specified file.
//                File(path).writeBytes(jpegByteArray)
//
//                // Insert the image information into the media store.
//                val values = ContentValues()
//                values.put(MediaStore.Images.ImageColumns.TITLE, title)
//                values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
//                values.put(MediaStore.Images.ImageColumns.DATA, path)
//                values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
//                values.put(MediaStore.Images.ImageColumns.WIDTH, width)
//                values.put(MediaStore.Images.ImageColumns.HEIGHT, height)
//                values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation)
//                values.put(MediaStore.Images.ImageColumns.LONGITUDE, longitude)
//                values.put(MediaStore.Images.ImageColumns.LATITUDE, latitude)
//                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//
//            }
//
//        }
//    }

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

