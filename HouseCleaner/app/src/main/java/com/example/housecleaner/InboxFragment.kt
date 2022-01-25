package com.example.housecleaner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housecleaner.adapter.TransactionsAdapter
import com.example.housecleaner.model.User
import com.example.housecleaner.databinding.FragmentInboxBinding
import com.example.housecleaner.model.Transaction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.ArrayList

class InboxFragment : Fragment(R.layout.fragment_inbox) {
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private var dbUser: User? = null
    private lateinit var dialog: AlertDialog
    private lateinit var adapter: TransactionsAdapter
    private var transactions: MutableList<Transaction> = arrayListOf()
    private lateinit var selectedTrans: Transaction
    private val readQr = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) updateTransStatus(1)
    }

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
            val ref1 = database.getReference("Cleaners").child(user?.uid.toString())
            val ref2 = database.getReference("Transactions")
            withContext(Dispatchers.Main) {
                dbUser = ref1.get().await().getValue(User::class.java)
                transactions.clear()
                ref2.get().await().children.forEach {
                    if (it.child("cleanerID").getValue(String::class.java).equals(user?.uid.toString())) {
                        val tr = it.getValue(Transaction::class.java)
                        print("\n print 1: \n ${tr.toString()}")
                        transactions.add(tr!!)
                    }
                }.apply {
                    binding.noTransactionsTxt.isVisible = transactions.isNullOrEmpty()
                    adapter = TransactionsAdapter(dbUser?.UID, transactions)
                    binding.inboxLst.adapter = adapter
                    binding.inboxLst.layoutManager = LinearLayoutManager(activity)
                    adapter.onItemLongClick = {
                        selectedTrans = transactions[it]
                        if (selectedTrans.completed != true && selectedTrans.status != "canceled" && selectedTrans.status == "onTheWay") {
                            dialog.show()
                            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        }
                    }
                }
            }
        }

        val builder = AlertDialog.Builder(activity)
        val view = View.inflate(activity, R.layout.arrived_trans_dialog, null)
        dialog = builder.setView(view).create()
        val btn = view.findViewById<FloatingActionButton>(R.id.arrivedBtn)
        btn.setOnClickListener {
            if (selectedTrans.status == "onTheWay")
                updateTransStatus(0)
            dialog.dismiss()
        }
        checkTransactions()
    }

    private fun checkTransactions() : MutableList<Transaction> {
        var tmpTrans = arrayListOf<Transaction>()
        database.getReference("Transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    if (it.child("cleanerID").getValue(String::class.java).equals(user?.uid.toString())) {
                        tmpTrans.add(it.getValue(Transaction::class.java)!!)
                        val tr = it.getValue(Transaction::class.java)!!
                        if (tr.status.equals("arrived")) {
                            val intent = Intent(activity, QrScannerActivity::class.java)
                            intent.putExtra("qr",
                                "${tr.status} house ${tr.house?.ID}")
                            print("\n \n lauching \n activity \n")
                            readQr.launch(intent)
                        }
                    }

                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        return tmpTrans
    }
    private fun updateTransStatus(status: Int) {
        if (status == 0) {
            selectedTrans.status = "arrived"
            database.getReference("Transactions/").child("${selectedTrans.transactionID}").setValue(selectedTrans)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Arrived", Toast.LENGTH_SHORT).show()
                }
        } else if (status == 1) {
            selectedTrans.status = "cleaning"
            database.getReference("Transactions/").child("${selectedTrans.transactionID}").setValue(selectedTrans)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Cleaning", Toast.LENGTH_SHORT).show()
                }
        }
    }


    fun updateDbUser() {
        database.getReference("Cleaners").child(user?.uid.toString())
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dbUser = snapshot.getValue(User::class.java)
                    binding.noTransactionsTxt.isVisible = dbUser?.transactions.isNullOrEmpty()
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
}