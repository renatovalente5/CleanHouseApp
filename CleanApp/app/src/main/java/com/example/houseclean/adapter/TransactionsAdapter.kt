package com.example.houseclean.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.houseclean.R
import com.example.houseclean.model.Transaction
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class TransactionsAdapter(private val UID: String?, private val transactions: MutableList<Transaction>? = arrayListOf()): RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = if (transactions.isNullOrEmpty()) 0 else transactions.size

    var onItemLongClick: ((Int) -> Unit)? = null

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val houseImg = itemView.findViewById<CircleImageView>(R.id.transactionHouseImg)
        private val addr = itemView.findViewById<TextView>(R.id.transactionLstAddress)
        private val status = itemView.findViewById<TextView>(R.id.transactionLstStatus)
        private val clientN = itemView.findViewById<TextView>(R.id.clientName)
        private val waiting = itemView.findViewById<TextView>(R.id.waiting)

        init {
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(absoluteAdapterPosition)
                return@setOnLongClickListener true
            }
        }

        fun bind(pos: Int) {
            with(transactions?.get(pos)) {
                storage.child(UID.plus("/houses/".plus(this?.house?.ID)).plus("/housePic")).downloadUrl.addOnSuccessListener {
                    if (it != null) {
                        com.bumptech.glide.Glide.with(itemView).load(it).into(houseImg)
                    }
                }.addOnFailureListener{
                    houseImg.setImageResource(R.drawable.ic_home)
                }
                status.text = this?.status
                if (this?.status == "canceled" || this?.status == "finished") {
                } else if (this?.status == "waiting") {
                    clientN.isVisible = false
                    addr.isVisible = false
                    waiting.isVisible = true
                } else {
                    clientN.text = this?.clientName
                    addr.text = this?.house?.address
                }
            }
        }
    }
}