package pl.pjatk.s16604.project2.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import pl.pjatk.s16604.project2.activities.CameraActivity
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
