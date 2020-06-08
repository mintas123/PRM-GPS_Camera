package pl.pjatk.s16604.project2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_photo_list.*
import pl.pjatk.s16604.project2.recycler.PhotoViewHolder

class PhotoListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_list)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        val recyclerAdapterMenu = PhotoViewHolder.RecyclerAdapterMenu(this)
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@PhotoListActivity)
            adapter = recyclerAdapterMenu
        }

    }
}