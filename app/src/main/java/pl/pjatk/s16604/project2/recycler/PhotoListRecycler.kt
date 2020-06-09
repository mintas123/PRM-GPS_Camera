package pl.pjatk.s16604.project2.recycler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_card.view.*
import pl.pjatk.s16604.project2.R
import java.io.File


class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


    val photo: ImageView = itemView.photo

    var mView: View = itemView

    // todo get from folder
    fun bind(photo: Bitmap) {
            this.photo.setImageBitmap(photo)
    }

    class RecyclerAdapterMenu(context: Context) : RecyclerView.Adapter<PhotoViewHolder>() {

        private var photos: MutableList<String>
        private var myContext: Context = context

        init {
            photos = loadData()
        }

        private fun loadData(): MutableList<String> {

            val root=myContext.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            var photos: MutableList<File>? = null

            if (root != null && !root.listFiles().isNullOrEmpty()) {
                photos=root.listFiles().toMutableList()
            }else{
                Log.d(TAG, "INVALID MEDIA ROOT")
            }
            val photosPaths = mutableListOf<String>()
            photos?.forEach { photosPaths.add(it.path) }
            return photosPaths
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
            return photos.size
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val bitmap = decodeSampledBitmapFromFile(photos[position],128,128)
            holder.bind(bitmap)

            holder.mView.setOnClickListener {
                //todo show full picture
            }
        }
        private fun decodeSampledBitmapFromFile(
            file: String,
            reqWidth: Int,
            reqHeight: Int
        ): Bitmap {
            // First decode with inJustDecodeBounds=true to check dimensions
            return BitmapFactory.Options().run {
                inJustDecodeBounds = true
                BitmapFactory.decodeFile(file,this)
                // Calculate inSampleSize
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                // Decode bitmap with inSampleSize set
                inJustDecodeBounds = false
                BitmapFactory.decodeFile(file,this)
            }
        }
        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2

                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
    companion object {
        const val TAG = "RECYCYLER"
    }

    }


