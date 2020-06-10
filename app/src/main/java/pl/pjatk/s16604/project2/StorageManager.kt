package pl.pjatk.s16604.project2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log


const val PREFS_FILENAME = "PROD"
const val WATERMARK_COLOR = "COLOR"
const val PROXIMITY = "DIST"
const val WHITE = "FFFFFFFF"

class StorageManager {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var color :String
    private var dist = 100

    fun loadData(context: Context): ProjectMetadata {
        Log.d("XX_", "LOADING>>>")
        sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        color = sharedPreferences.getString(WATERMARK_COLOR, WHITE)
        dist = sharedPreferences.getInt(PROXIMITY, 100)
        Log.d("XX_", "LOADED>>> $color")

        return ProjectMetadata(sharedPreferences, color, dist)
    }

    fun saveData(md: ProjectMetadata) {
        Log.d("XX_", "SAVING>>>")
        Log.d("XX_", "MD COLOR: >>> ${md.color}")

        val editor = sharedPreferences.edit()
        editor.putInt(PROXIMITY, md.dist)
        editor.putString(WATERMARK_COLOR, color)
        editor.apply()
        editor.commit()
        color = md.color
        dist = md.dist
        Log.d("XX_", "SAVED>>> $color")

    }

}