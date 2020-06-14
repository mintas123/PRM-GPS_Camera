package pl.pjatk.s16604.project2.activities

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_app_settings.*
import pl.pjatk.s16604.project2.models.ProjectMetadata
import pl.pjatk.s16604.project2.R
import pl.pjatk.s16604.project2.StorageManager
import pl.pjatk.s16604.project2.WHITE
import pl.pjatk.s16604.project2.utils.animate

private val STORAGE = StorageManager()

class AppSettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences:SharedPreferences
    private var color = WHITE
    private var distance = 100



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        onColorPicker()
        loadData()
        onBackBtn()
        onEditDistance()
    }

    override fun onStop() {
        saveData()
        super.onStop()
    }

    private fun onEditDistance(){
        save_btn.setOnClickListener {
            animate(this,save_btn)
            val dist = distance_value.text.toString()
            val distValue = dist.toIntOrNull()
            if (distValue != null && distValue >= 0){
                distance_value.hint = null
                distance = distValue
            }
            saveData()
            finish()
        }
    }

    private fun onColorPicker(){
        colorPickerView.setColorListener(object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                val hexCode = envelope?.hexCode
                color_label_current.text = "#$hexCode"
                color_label_current.setTextColor(Color.parseColor("#$hexCode"))
                if (hexCode != null) {
                    color= hexCode
                }
            }
        })
    }

    private fun onBackBtn(){
        back_btn.setOnClickListener {
            animate(this,back_btn)
            finish()
        }
    }

    private fun loadData() {
        val metadata = STORAGE.loadData(this)
        sharedPreferences = metadata.sharedPreferences
        color = metadata.color
        distance = metadata.dist

        color_label_current.text = color
        color_label_current.setTextColor(Color.parseColor("#${color}"))
        distance_value.setText(distance.toString())
    }
    private fun saveData() {
        STORAGE.saveData(
            ProjectMetadata(
                sharedPreferences, color, distance
            )
        )
    }
}