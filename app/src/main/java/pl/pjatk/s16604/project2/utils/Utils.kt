package pl.pjatk.s16604.project2.utils

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.location.Location
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import pl.pjatk.s16604.project2.models.PhotoMetadata
import pl.pjatk.s16604.project2.R
import pl.pjatk.s16604.project2.activities.CameraActivity
import pl.pjatk.s16604.project2.recycler.PhotoViewHolder
import java.io.File
import java.net.URI
import java.net.URISyntaxException


fun animate(context: Context, obj: View) {
    val animationZoom = AnimationUtils.loadAnimation(
        context,
        R.anim.zoom_in
    )
    val animationZoomOut = AnimationUtils.loadAnimation(
        context,
        R.anim.zoom_out
    )

    obj.startAnimation(animationZoom)
    obj.startAnimation(animationZoomOut)

}
