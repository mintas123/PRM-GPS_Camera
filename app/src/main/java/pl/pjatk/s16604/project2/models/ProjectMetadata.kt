package pl.pjatk.s16604.project2.models

import android.content.SharedPreferences

data class ProjectMetadata(
    val sharedPreferences: SharedPreferences,
    var color: String,
    var dist: Int
)