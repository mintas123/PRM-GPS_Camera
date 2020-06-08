package pl.pjatk.s16604.project2.recycler

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_card.view.*
import pl.pjatk.s16604.project2.R

class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imageView: ImageView = itemView.photo

    //    private val upgradeTitle: TextView = itemView.upgrade_title
//    private val upgradeAmount: TextView = itemView.upgrade_amount
    var mView: View = itemView

    // todo get from folder
    fun bind(photo: String) {
        imageView.setImageResource(getImageId(itemView.context, photo)) //temp
    }

    class RecyclerAdapterMenu(context: Context) : RecyclerView.Adapter<PhotoViewHolder>() {

        private var photos: MutableList<String> = ArrayList()
        private var myContext: Context = context

        init {
            photos = loadData(myContext)
        }

        fun loadData(context: Context): MutableList<String> {
            //todo
            return mutableListOf("TO", "DO")
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
            holder.bind(photos[position])

            holder.mView.setOnClickListener {
                //todo show full picture
            }
        }
    }

    private fun getImageId(context: Context, imageName: String): Int {
        return context.resources
            .getIdentifier("drawable/$imageName", null, context.packageName)
    }
}


