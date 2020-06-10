package pl.pjatk.s16604.project2.utils

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import pl.pjatk.s16604.project2.R
import pl.pjatk.s16604.project2.WHITE
import pl.pjatk.s16604.project2.activities.CameraActivity
import java.io.File
import java.net.URI
import java.net.URISyntaxException


fun animate(context: Context, obj: View) {
    val animationZoom = AnimationUtils.loadAnimation(context,
        R.anim.zoom_in
    )
    val animationZoomOut = AnimationUtils.loadAnimation(context,
        R.anim.zoom_out
    )

    obj.startAnimation(animationZoom)
    obj.startAnimation(animationZoomOut)

}

@Throws(URISyntaxException::class)
fun notifyData(context: Context, fileUri: URI) {
    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    val f = File(fileUri)
    val contentUri: Uri = Uri.fromFile(f)
    mediaScanIntent.data = contentUri
    context.sendBroadcast(mediaScanIntent)
    Log.d(CameraActivity.TAG,"Broadcast sent")
}


fun getBitmapFromString(
    text: String,
    fontSizeSP: Float,
    context: Context,
    color: String
): Bitmap {
    val fontSizePX: Int = convertDipToPix(context, fontSizeSP)
    val pad = fontSizePX / 9
    val paint = Paint()
    paint.isAntiAlias = true
    val parseColor = Color.parseColor("#$color")
    paint.color = parseColor
    Log.d("XX_COLOR", "Color: $color")
    paint.textSize = fontSizePX.toFloat()
    val textWidth = (paint.measureText(text) + pad * 2).toInt()
    val height = (fontSizePX / 0.75).toInt()
    val bitmap = Bitmap.createBitmap(textWidth, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val xOriginal = pad.toFloat()
    canvas.drawText(text, xOriginal, fontSizePX.toFloat(), paint)
    return bitmap
}
private fun convertDipToPix(context: Context, dip: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dip,
        context.resources.displayMetrics
    ).toInt()
}

fun overlay(original: Bitmap, overlay: Bitmap): Bitmap? {
    val bmOverlay =
        Bitmap.createBitmap(original.width, original.height, original.config)
    val canvas = Canvas(bmOverlay)
    canvas.drawBitmap(original, Matrix(), null)
    canvas.drawBitmap(overlay, Matrix(), null)
    return bmOverlay
}


fun decodeSampledBitmapFromFile(
    file: String,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(file, this)
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        BitmapFactory.decodeFile(file, this)
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun getRealPathFromURI(
    context: Context,
    contentUri: Uri?
): String? {
    var cursor: Cursor? = null
    return try {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        cursor = context.contentResolver.query(contentUri, proj, null, null, null)
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        cursor.getString(columnIndex)
    } finally {
        cursor?.close()
    }
}