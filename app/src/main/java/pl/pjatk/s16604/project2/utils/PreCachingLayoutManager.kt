package pl.pjatk.s16604.project2.utils

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PreCachingGridManager : GridLayoutManager {
    private val defaultExtraLayoutSpace = 600
    private var extraLayoutSpace = -1
    private var context: Context? = null

    constructor(context: Context?,spanCount: Int) : super(context,spanCount) {
        this.context = context
    }
    constructor(context: Context, spanCount: Int,extraLayoutSpace: Int) : super(context,spanCount) {
        this.context = context
        this.extraLayoutSpace = extraLayoutSpace
    }
    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(
        context,
        spanCount,
        orientation,
        reverseLayout
    ) {
        this.context = context
    }
    fun setExtraLayoutSpace(extraLayoutSpace: Int) {
        this.extraLayoutSpace = extraLayoutSpace
    }
    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        return if (extraLayoutSpace > 0) {
            extraLayoutSpace
        } else defaultExtraLayoutSpace
    }
}