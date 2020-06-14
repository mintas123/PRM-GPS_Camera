package pl.pjatk.s16604.project2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import pl.pjatk.s16604.project2.models.ProjectMetadata


const val PREFS_FILENAME = "PROD"
const val WATERMARK_COLOR = "COLOR"
const val PROXIMITY = "DIST"
const val WHITE = "FFFFFFFF"
const val BLACK = "FF000000"

class StorageManager {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var color :String
    private var dist = 100

    fun loadData(context: Context): ProjectMetadata {
        sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        color = sharedPreferences.getString(WATERMARK_COLOR, WHITE)
        dist = sharedPreferences.getInt(PROXIMITY, 100)

        return ProjectMetadata(
            sharedPreferences,
            color,
            dist
        )
    }

    fun saveData(md: ProjectMetadata) {
        val editor = sharedPreferences.edit()
        editor.putInt(PROXIMITY, md.dist)
        editor.putString(WATERMARK_COLOR, md.color)
        editor.apply()
        editor.commit()
        color = md.color
        dist = md.dist

    }

}