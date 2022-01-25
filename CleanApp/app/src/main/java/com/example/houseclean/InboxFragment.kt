package com.example.houseclean

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.houseclean.adapter.HousesAdapter
import com.example.houseclean.adapter.TransactionsAdapter
import com.example.houseclean.databinding.FragmentInboxBinding
import com.example.houseclean.model.Transaction
import com.example.houseclean.model.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class InboxFragment : Fragment(R.layout.fragment_inbox) {
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private var dbUser: User? = null
    private lateinit var dialog: AlertDialog
    private var transactions: MutableList<Transaction> = arrayListOf()
    private lateinit var selectedTrans: Transaction
    private lateinit var adapter: TransactionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch(Dispatchers.IO){
            val ref1 = database.getReference("Users").child(user?.uid.toString())
            val ref2 = database.getReference("Transactions")
            withContext(Dispatchers.Main) {
                dbUser = ref1.get().await().getValue(User::class.java)
                transactions.clear()
                ref2.get().await().children.forEach {
                    if (it.child("clientID").getValue(String::class.java).equals(user?.uid.toString())) {
                        val tr = it.getValue(Transaction::class.java)
                        transactions.add(tr!!)
                    }
                }.apply {
                    binding.noTransactionsTxt.isVisible = transactions.isNullOrEmpty()
                    adapter = TransactionsAdapter(dbUser?.UID, transactions)
                    binding.inboxLst.adapter = adapter
                    binding.inboxLst.layoutManager = LinearLayoutManager(activity)
                    adapter.onItemLongClick = {
                        selectedTrans = transactions[it]
                        if (selectedTrans.completed != true && selectedTrans.status != "canceled") {
                            dialog.show()
                            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        }
                    }
                }
            }
        }

        val builder = AlertDialog.Builder(activity)
        val view = View.inflate(activity, R.layout.cancel_trans_dialog, null)
        dialog = builder.setView(view).create()
        val btn = view.findViewById<FloatingActionButton>(R.id.cancelTransBtn)
        btn.setOnClickListener {
            cancelTrans()
            dialog.dismiss()
        }
    }

    private fun cancelTrans() {
        selectedTrans.status = "canceled"
        database.getReference("Transactions").child(selectedTrans.transactionID.toString())
            .setValue(selectedTrans).addOnSuccessListener {
                Toast.makeText(activity, "Canceled!" , Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkTransactions() : MutableList<Transaction> {
        var tmpTrans = arrayListOf<Transaction>()
        database.getReference("Transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    if (it.child("clientID").getValue(String::class.java).equals(user?.uid.toString())) {
                        val tr = it.getValue(Transaction::class.java)
                        tmpTrans.add(tr!!)
                    }
                    binding.noTransactionsTxt.isVisible = transactions.isNullOrEmpty()
                    adapter = TransactionsAdapter(dbUser?.UID, transactions)
                    binding.inboxLst.adapter = adapter
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        return tmpTrans
    }
}