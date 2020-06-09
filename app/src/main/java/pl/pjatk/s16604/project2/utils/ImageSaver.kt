package pl.pjatk.s16604.project2.utils

import android.graphics.*
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


internal class ImageSaver(
    private val image: Image,
    private val file: File,
    private val watermark: Bitmap
) : Runnable {

    override fun run() {

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val options = BitmapFactory.Options()
        options.inMutable = true
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val overlayed = overlay(original, watermark)
        val byteArrayOutputStream = ByteArrayOutputStream()

        overlayed!!.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream)

        var output: FileOutputStream? = null
        val watermarkBytes = byteArrayOutputStream.toByteArray()

        try {
            output = FileOutputStream(file).apply {
                write(watermarkBytes)
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
    }

    companion object {
        private const val TAG = "XX_IMAGE_SAVER"
    }


}
