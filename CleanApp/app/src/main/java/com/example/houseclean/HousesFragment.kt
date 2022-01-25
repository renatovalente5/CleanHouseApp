package com.example.houseclean

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.houseclean.adapter.HousesAdapter
import com.example.houseclean.databinding.FragmentHousesBinding
import com.example.houseclean.model.House
import com.example.houseclean.model.Transaction
import com.example.houseclean.model.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.okhttp.Dispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HousesFragment : Fragment(R.layout.fragment_houses) {
    private var _binding: FragmentHousesBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private var dbUser: User? = null
    private lateinit var adapter: HousesAdapter
    private lateinit var addHouse: ActivityResultLauncher<Intent>
    private lateinit var dialog: AlertDialog
    private lateinit var selectedHouse: House


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHousesBinding.inflate(inflater, container, false)
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalScope.launch(Dispatchers.IO){
            val ref = database.getReference("Users").child(user?.uid.toString())
            withContext(Dispatchers.Main) {
                dbUser = ref.get().await().getValue(User::class.java)
                binding.noHousesTxt.isVisible = dbUser?.houses.isNullOrEmpty()
                adapter = HousesAdapter(dbUser?.UID, dbUser?.houses)
                binding.houseLst.adapter = adapter
                binding.houseLst.layoutManager = LinearLayoutManager(activity)
                adapter.onItemLongClick = {
                    selectedHouse = dbUser?.houses?.get(it)!!
                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                }
            }
        }

        addHouse = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) Toast.makeText(activity, "Added!", Toast.LENGTH_SHORT).show()
        }

        binding.addHouseBtn.setOnClickListener{
            val intent = Intent(activity, AddHouseActivity::class.java)
            if (dbUser?.houses?.isEmpty() == true) intent.putExtra("houseID", "0")
            else intent.putExtra("houseID", (dbUser?.houses?.size).toString())
            intent.putExtra("user", dbUser)
            addHouse.launch(intent)
        }

        val builder = AlertDialog.Builder(activity)
        val view = View.inflate(activity, R.layout.clean_house_dialog, null)
        dialog = builder.setView(view).create()
        val btn = view.findViewById<FloatingActionButton>(R.id.cleanHouseBtn)
        btn.setOnClickListener {
            addTransaction()
            dialog.dismiss()
        }


    }

    private fun addTransaction() {
        database.getReference("Transactions").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var transaction = Transaction(
                    transactionID = snapshot.childrenCount.toInt(),
                    clientID = dbUser?.UID,
                    clientName = dbUser?.name,
                    house = selectedHouse,
                    status = "waiting"
                )
                database.getReference("Transactions").child(transaction.transactionID.toString()).setValue(transaction)
                    .addOnSuccessListener{
                        mainActivity.not(1)
                        Toast.makeText(activity, "Waiting for cleaners!", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener{
                        Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show()
                    }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}