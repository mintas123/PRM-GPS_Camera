package pl.pjatk.s16604.project2

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

class CameraStream @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : TextureView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width / 3 * 4)
    }
}