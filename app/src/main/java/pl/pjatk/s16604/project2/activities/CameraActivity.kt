package pl.pjatk.s16604.project2.activities

import android.Manifest.permission.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_camera.*
import pl.pjatk.s16604.project2.*
import pl.pjatk.s16604.project2.utils.ImageSaver
import pl.pjatk.s16604.project2.utils.*
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.lang.Long.signum
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


var PIC_FILE_NAME_BASE = ""


class CameraActivity : AppCompatActivity() {

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height, CameraCharacteristics.LENS_FACING_BACK)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    private lateinit var cameraId: String

    private lateinit var mTextureView: AutoFitTextureView
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private lateinit var previewSize: Size

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraActivity.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraActivity.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            this@CameraActivity.finish()
        }

    }
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var imageReader: ImageReader? = null
    private lateinit var file: File
    private lateinit var address: String

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        backgroundHandler?.post(
            ImageSaver(
                it.acquireNextImage(),
                file,
                getBitmapFromString(address,60f,this),
                this
            )
        )
    }

    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest

    //start values
    private var CURRENT_CAMERA: Int = 0
    private var state =
        STATE_PREVIEW
    private val cameraOpenCloseLock = Semaphore(1)
    private var flashSupported = false
    private var sensorOrientation = 0

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                STATE_WAITING_LOCK -> capturePicture(result)
                STATE_WAITING_PRE_CAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        state =
                            STATE_WAITING_NON_PRE_CAPTURE
                    }
                }
                STATE_WAITING_NON_PRE_CAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state =
                            STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
            ) {
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state =
                        STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPrecaptureSequence()
                }
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        mTextureView = texture_view
        PIC_FILE_NAME_BASE = "dupa"
        onTakePicture()
        onChangeCamera()
        onGallery()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (mTextureView.isAvailable) {
            openCamera(
                mTextureView.width,
                mTextureView.height,
                CameraCharacteristics.LENS_FACING_BACK
            )
        } else {
            mTextureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun requestPermissions() {
        if (shouldShowRequestPermissionRationale(CAMERA)) {
            showPermissionAlert()
        } else {
            requestPermissions(
                arrayOf(
                    CAMERA, ACCESS_FINE_LOCATION, READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    private fun showPermissionAlert() {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Permission Required")
        alertBuilder.setMessage("Permission to access camera, data and GPS is needed to run this application")
        alertBuilder.setPositiveButton(android.R.string.yes) { _, _ ->
            ActivityCompat.requestPermissions(
                this@CameraActivity,
                arrayOf(
                    CAMERA,
                    ACCESS_FINE_LOCATION,
                    WRITE_EXTERNAL_STORAGE,
                    READ_EXTERNAL_STORAGE
                ),
                REQUEST_CAMERA_PERMISSION
            )
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setUpCameraOutputs(width: Int, height: Int, facing: Int) {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraDirection != null && cameraDirection != facing) {
                    continue
                }

                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue

                val largest = Collections.max(
                    listOf(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )
                imageReader = ImageReader.newInstance(
                    largest.width, largest.height,
                    ImageFormat.JPEG, /*maxImages*/ 2
                ).apply {
                    setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                }
                val displayRotation = windowManager.defaultDisplay.rotation

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                val swappedDimensions = areDimensionsSwapped(displayRotation)

                val displaySize = Point()
                windowManager.defaultDisplay.getSize(displaySize)
                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height
                var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth =
                    MAX_PREVIEW_WIDTH
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight =
                    MAX_PREVIEW_HEIGHT

                previewSize =
                    chooseOptimalSize(
                        map.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewWidth, rotatedPreviewHeight,
                        maxPreviewWidth, maxPreviewHeight,
                        largest
                    )

                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(previewSize.width, previewSize.height)
                } else {
                    mTextureView.setAspectRatio(previewSize.height, previewSize.width)
                }

                flashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                this.cameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: NullPointerException) {
            Log.e(TAG, e.toString())
            closeCamera()
        }
    }

    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    private fun openCamera(width: Int, height: Int, facing: Int) {

        val cameraPermission = ContextCompat.checkSelfPermission(this, CAMERA)
        val dataWPermission = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        val dataRPermission = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
        if (cameraPermission != PackageManager.PERMISSION_GRANTED ||
            dataRPermission != PackageManager.PERMISSION_GRANTED ||
            dataWPermission != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions()
            return
        }
        setUpCameraOutputs(width, height, facing)
        configureTransform(width, height)
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
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
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = mTextureView.surfaceTexture

            texture.setDefaultBufferSize(previewSize.width, previewSize.height)

            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )

            previewRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(
                listOf(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (cameraDevice == null) return

                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )

                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(
                                previewRequest,
                                captureCallback, backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(this@CameraActivity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = this.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        mTextureView.setTransform(matrix)
    }

    private fun lockFocus() {

        try {
            val location = getLocation(this)
            address = getAddress(
                this,
                LatLng(location!!.latitude, location.longitude)
            )
            val date = System.currentTimeMillis()
            val dateFormat: DateFormat = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
            PIC_FILE_NAME_BASE = dateFormat.format(date)
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )

            val root = getExternalFilesDir(Environment.DIRECTORY_DCIM)
            file = File(root, PIC_FILE_NAME_BASE + PIC_FORMAT)
            notifyData(
                this@CameraActivity,
                file.toURI()
            )

            Log.d(TAG, "LAT: ${location.latitude} LON: ${location.longitude}")
            Log.d(TAG, "ADDRESS: $address")

            state =
                STATE_WAITING_LOCK

            captureSession?.capture(
                previewRequestBuilder.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }


    private fun runPrecaptureSequence() {
        try {
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            state =
                STATE_WAITING_PRE_CAPTURE
            captureSession?.capture(
                previewRequestBuilder.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun captureStillPicture() {
        try {
            if (cameraDevice == null) return
            val rotation = windowManager.defaultDisplay.rotation

            val captureBuilder = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
            )?.apply {
                addTarget(imageReader?.surface!!)
                set(
                    CaptureRequest.JPEG_ORIENTATION,
                    (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360
                )
                set(
                    CaptureRequest.JPEG_GPS_LOCATION,
                    getLocation(this@CameraActivity))
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Toast.makeText(this@CameraActivity, "Saved: $file", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, file.toString())
                    unlockFocus()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder?.build()!!, captureCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            captureSession?.capture(
                previewRequestBuilder.build(), captureCallback,
                backgroundHandler
            )
            // After this, the camera will go back to the normal state of preview.
            state =
                STATE_PREVIEW
            captureSession?.setRepeatingRequest(
                previewRequest, captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun onTakePicture() {
        take_pic_btn.setOnClickListener {
            Log.d(TAG, "picture request")
            animate(this,take_pic_btn)
            lockFocus()
        }
    }


    private fun onChangeCamera() {
        change_camera_btn.setOnClickListener {
            animate(this, change_camera_btn)
            closeCamera()
            if (CURRENT_CAMERA == 0) {
                openCamera(mTextureView.width, mTextureView.height,CameraCharacteristics.LENS_FACING_FRONT)
                CURRENT_CAMERA = 1
            } else {
                openCamera(mTextureView.width, mTextureView.height,CameraCharacteristics.LENS_FACING_BACK)
                CURRENT_CAMERA = 0
            }
        }
    }

    private fun onGallery() {
        gallery_btn.setOnClickListener {
            animate(this, gallery_btn)
            val intent = Intent(this, PhotoListActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
        const val PIC_FORMAT = ".jpg"
        const val TAG = "XX_CAMERA_ACTIVITY"
        private const val REQUEST_CAMERA_PERMISSION = 1

        private const val STATE_PREVIEW = 0
        private const val STATE_WAITING_LOCK = 1
        private const val STATE_WAITING_PRE_CAPTURE = 2
        private const val STATE_WAITING_NON_PRE_CAPTURE = 3
        private const val STATE_PICTURE_TAKEN = 4
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080

        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            val bigEnough = ArrayList<Size>()
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }
            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(
                    bigEnough,
                    CompareSizesByArea()
                )
            } else if (notBigEnough.size > 0) {
                return Collections.max(
                    notBigEnough,
                    CompareSizesByArea()
                )
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

        internal class CompareSizesByArea : Comparator<Size> {
            override fun compare(a: Size, b: Size) =
                signum(a.width.toLong() * a.height - b.width.toLong() * b.height)
        }
    }
}


