package pl.pjatk.s16604.project2.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_photo_list.*
import pl.pjatk.s16604.project2.utils.GridItemDecoration
import pl.pjatk.s16604.project2.R
import pl.pjatk.s16604.project2.recycler.PhotoViewHolder
import pl.pjatk.s16604.project2.utils.animate

class PhotoListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_list)
        initRecyclerView()
        onBackButton()
    }

    private fun initRecyclerView() {
        val recyclerAdapterMenu = PhotoViewHolder.RecyclerAdapterMenu(this)
        recycler_view.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            layoutManager = GridLayoutManager(this@PhotoListActivity,2)
            addItemDecoration(
                GridItemDecoration(
                    15,
                    2
                )
            )
            adapter = recyclerAdapterMenu
        }

    }
    private fun onBackButton() {
        back_btn.setOnClickListener {
            animate(this, back_btn)
            finish()
        }

    }

}