package pl.pjatk.s16604.project2

import android.Manifest.permission.CAMERA
import android.content.Context
import android.graphics.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class CameraActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        private val TAG = this::class.qualifiedName
        @JvmStatic
        fun newInstance() = CameraActivity()
    }

    private lateinit var myTextureView: TextureView

    private val cameraManager by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }


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
        Log.d(TAG, "devicesId: $deviceId")
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
            openCamera()
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

    override fun onStart() {
        super.onStart()
        myTextureView = findViewById(R.id.textureView)
        myTextureView.surfaceTextureListener = surfaceListener
    }

    override fun onResume() {
        super.onResume()
        if (myTextureView.isAvailable) {
            openCamera()
        } else {
            myTextureView.surfaceTextureListener = surfaceListener
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