package com.example.housecleaner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housecleaner.model.House
import com.example.housecleaner.R
import com.example.housecleaner.model.Transaction
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class HousesAdapter(private val UID: String?, private val houses: MutableList<Transaction>? = arrayListOf()): RecyclerView.Adapter<HousesAdapter.HousesViewHolder>() {
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HousesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.house_item, parent, false)
        return HousesViewHolder(view)
    }

    override fun onBindViewHolder(holder: HousesViewHolder, position: Int) {
        holder.bind(position)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(position)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(position)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int = if (houses.isNullOrEmpty()) 0 else houses.size

    var onItemClick: ((Int) -> Unit)? = null
    var onItemLongClick: ((Int) -> Unit)? = null

    inner class HousesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val img = itemView.findViewById<CircleImageView>(R.id.houseLstImg)
        private val title = itemView.findViewById<TextView>(R.id.houseLstTitle)
        private val addr = itemView.findViewById<TextView>(R.id.houseLstAddress)

        fun bind(pos: Int) {
            with(houses?.get(pos)) {
                storage.child(this?.clientID.plus("/houses/".plus(this?.house?.ID)).plus("/housePic")).downloadUrl.addOnSuccessListener {
                    if (it != null) {
                        Glide.with(itemView).load(it).into(img)
                    }
                }.addOnFailureListener{
                    img.setImageResource(R.drawable.ic_home)
                }
                title.setText("House ".plus((pos+1).toString()))
                addr.setText(this?.house?.address)
            }
        }
    }
}

