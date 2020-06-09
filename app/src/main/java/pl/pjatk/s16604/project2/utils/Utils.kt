package pl.pjatk.s16604.project2.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import pl.pjatk.s16604.project2.R
import pl.pjatk.s16604.project2.activities.CameraActivity
import java.io.File
import java.io.IOException
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

fun getLocation(context: Context): Location? {
    val locationManager = context.getSystemService(LocationManager::class.java)
    if (locationManager != null && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }
    return null
}
fun getAddress(context: Context,latLng: LatLng): String {
    val geocoder = Geocoder(context)
    val addresses: List<Address>?
//        val address: Address?
    var addressText = ""

    try {
        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val subLocality = addresses[0].subLocality
            val city= addresses[0].locality
            val country = addresses[0].countryName
            val adminArea = addresses[0].adminArea

            if (country != null){
                if (adminArea != null) {
                    if (city != null){
                        if (subLocality != null){
                            addressText = "$subLocality - $city, $country"
                        } else{
                            addressText = "$city, $country"
                        }
                    }else{
                        addressText = "$adminArea, $country"
                    }
                }else{
                    addressText = country
                }
            } else {
                addressText = "Planet Earth"
            }
        }
    } catch (e: IOException) {
        Log.e(CameraActivity.TAG, e.toString())
    }

    return addressText
}

