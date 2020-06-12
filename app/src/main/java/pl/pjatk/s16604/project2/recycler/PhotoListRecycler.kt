package pl.pjatk.s16604.project2.recycler

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_card.view.*
import pl.pjatk.s16604.project2.R
import pl.pjatk.s16604.project2.activities.FullscreenPhotoActivity
import pl.pjatk.s16604.project2.utils.decodeSampledBitmapFromFile
import pl.pjatk.s16604.project2.utils.filterPhotos
import pl.pjatk.s16604.project2.utils.getLocation
import java.io.File


class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


    private val photo: ImageView = itemView.photo

    var mView: View = itemView

    fun bind(photo: Bitmap) {
        this.photo.setImageBitmap(photo)
    }

    class RecyclerAdapter(context: Context) : RecyclerView.Adapter<PhotoViewHolder>() {

        private var photosPaths: MutableList<String>
        private var myContext: Context = context

        init {
            photosPaths = loadData()
        }

        private fun loadData(): MutableList<String> {
            return filterPhotos(myContext)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val inflater = LayoutInflater.from(parent.context) as LayoutInflater
            return PhotoViewHolder(
                inflater.inflate(
                    R.layout.recycler_card,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return photosPaths.size
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val bitmap = decodeSampledBitmapFromFile(photosPaths[position], 256, 256)
            holder.bind(bitmap)

            holder.mView.setOnClickListener {
                val intent = Intent(myContext, FullscreenPhotoActivity::class.java)
                intent.putExtra("picPath", photosPaths[position])
                myContext.startActivity(intent)

            }
        }
    }

    companion object {
        const val TAG = "XX_RECYCYLER"
    }

}


