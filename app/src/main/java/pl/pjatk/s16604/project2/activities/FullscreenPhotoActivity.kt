package pl.pjatk.s16604.project2.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_fullscreen_photo.*
import pl.pjatk.s16604.project2.R

class FullscreenPhotoActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        val picPath: String? = extras!!.getString("picPath")
        setContentView(R.layout.activity_fullscreen_photo)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        fullscreen_content.setImageBitmap(
            BitmapFactory.decodeFile(picPath))
    }



}