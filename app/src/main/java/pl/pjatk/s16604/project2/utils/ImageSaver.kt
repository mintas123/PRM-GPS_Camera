package pl.pjatk.s16604.project2.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors


internal class ImageSaver(
    private val image: Image,
    private val file: File,
    private val watermark: Bitmap,
    private val mContext: Context
) : Runnable {

    private val saveImageExecutor: Executor = Executors.newSingleThreadExecutor()

    override fun run() {


        val jpegByteBuffer = image.planes[0].buffer
        val jpegByteArray = ByteArray(jpegByteBuffer.remaining())
        jpegByteBuffer.get(jpegByteArray)

        val width = image.width
        val height = image.height
        saveImageExecutor.execute {
            val date = System.currentTimeMillis()
            val location = getLocation(mContext)
            val longitude = location?.longitude ?: 0.0
            val latitude = location?.latitude ?: 0.0

            // watermark
            val options = BitmapFactory.Options()
            options.inMutable = true
            val original =
                BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size, options)
            val overlayed = overlay(original, watermark)
            val watermarkedByteArrayOS = ByteArrayOutputStream()
            overlayed!!.compress(Bitmap.CompressFormat.JPEG, 100, watermarkedByteArrayOS)

            val watermarkedByteArray = watermarkedByteArrayOS.toByteArray()
            Log.d(TAG, "saving pic meta-data")
            val values = ContentValues()
            values.put(MediaStore.Images.ImageColumns.TITLE, file.name)
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, file.name)
            values.put(MediaStore.Images.ImageColumns.DATA, file.path)
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
            values.put(MediaStore.Images.ImageColumns.WIDTH, width)
            values.put(MediaStore.Images.ImageColumns.HEIGHT, height)
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, longitude)
            values.put(MediaStore.Images.ImageColumns.LATITUDE, latitude)
            Log.d(TAG, "PATH: ${values.get(MediaStore.Images.ImageColumns.DATA)}")

            Log.d(TAG, "LON: ${values.get(MediaStore.Images.ImageColumns.LATITUDE)}")
            Log.d(TAG, "LAT: ${values.get(MediaStore.Images.ImageColumns.LONGITUDE)}")
            var output: FileOutputStream? = null
            try {
                output = FileOutputStream(file).apply {
                    write(watermarkedByteArray)
                }
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            } finally {
                image.close()
                output?.let {
                    try {
                        it.close()
                    } catch (e: IOException) {
                        Log.e(TAG, e.toString())
                    }
                }
            }
            mContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        }



    }

    companion object {
        private const val TAG = "XX_IMAGE_SAVER"
    }


}
