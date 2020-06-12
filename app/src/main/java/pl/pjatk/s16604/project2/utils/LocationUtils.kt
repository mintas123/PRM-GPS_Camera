package pl.pjatk.s16604.project2.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import pl.pjatk.s16604.project2.activities.CameraActivity
import pl.pjatk.s16604.project2.models.PhotoMetadata
import pl.pjatk.s16604.project2.recycler.PhotoViewHolder
import java.io.IOException

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
fun getAddress(context: Context, latLng: LatLng): String {
    val geocoder = Geocoder(context)
    val addresses: List<Address>?
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

fun filterPhotos(context: Context): MutableList<String> {

    val currentLocation = getLocation(context)
    val metadataList = mutableListOf<PhotoMetadata>()
    val pathList = mutableListOf<String>()

    //get photos metadata
    val mColumnProjection = arrayOf(
        MediaStore.Images.ImageColumns.LONGITUDE,
        MediaStore.Images.ImageColumns.LATITUDE,
        MediaStore.Images.ImageColumns.DATA
    )
    val mSelection = MediaStore.Images.ImageColumns.DATA + " LIKE ?"
    val mSelectionArg = "%data/pl.pjatk.s16604.project2/files%"
    if (currentLocation !== null) {
        val contentResolver = context.contentResolver
        val cursor: Cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mColumnProjection,
            mSelection,
            arrayOf(mSelectionArg),
            null
        )
        while (cursor.moveToNext()) {
            metadataList.add(
                PhotoMetadata(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2)
                )
            )
        }
        Log.d(PhotoViewHolder.TAG, "Photos found: ${metadataList.size}")

        //filter only those photos that dont have location or are close enough
        metadataList.forEach {
            if (it.lat !== null && it.lon !== null) {
                val floatArray = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    it.lat.toDouble(), it.lon.toDouble(), floatArray
                )
                if (floatArray[0] < 1000) {
                    Log.d(PhotoViewHolder.TAG, "Photo added, dist: ${floatArray[0]}")
                    pathList.add(it.path)
                } else {
                    Log.d(
                        PhotoViewHolder.TAG,
                        "Photo further than 100, dist:  ${floatArray[0]}"
                    )
                }
            } else {
                //added null location to the list for sake of showing stuff on my phone
                pathList.add(it.path)
                Log.d(PhotoViewHolder.TAG, "NULL LOCATION ${it.path}")
            }
        }
        cursor.close()
    } else {
        Log.d(PhotoViewHolder.TAG, "You suck")
    }
    pathList.sortDescending()
    return pathList
}
