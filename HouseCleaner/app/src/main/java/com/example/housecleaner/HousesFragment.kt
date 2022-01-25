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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housecleaner.model.House
import com.example.housecleaner.model.Transaction
import com.example.housecleaner.model.User
import com.example.housecleaner.adapter.HousesAdapter
import com.example.housecleaner.databinding.FragmentHousesBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class HousesFragment : Fragment() {
    private var _binding: FragmentHousesBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainActivity: MainActivity
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private var dbUser: User? = null
    private lateinit var adapter: HousesAdapter
    private lateinit var addHouse: ActivityResultLauncher<Intent>
    private lateinit var dialog: AlertDialog
    private lateinit var selectedTrans: Transaction
    private var transactions: MutableList<Transaction> = arrayListOf()

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

        updateDbUser()
        adapter = HousesAdapter(dbUser?.UID, transactions)
        binding.houseLst.adapter = adapter
        binding.houseLst.layoutManager = LinearLayoutManager(activity)

        addHouse = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) Toast.makeText(activity, "Added!", Toast.LENGTH_SHORT).show()
        }

        val builder = AlertDialog.Builder(activity)
        val view = View.inflate(activity, R.layout.apply_trans_dialog, null)
        dialog = builder.setView(view).create()
        val btn = view.findViewById<FloatingActionButton>(R.id.cleanHouseBtn)
        btn.setOnClickListener {
            applyTransaction()
            dialog.dismiss()
        }

        adapter.onItemLongClick = {
            selectedTrans = transactions[it]
            if (selectedTrans.status == "waiting") {
                dialog.show()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }

    private fun getNearHouses() {
        val tmpTrans = arrayListOf<Transaction>()
        database.getReference("Transactions")
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach{
                        val tmp = it.getValue(Transaction::class.java)
                        if (tmp?.status == "waiting"){
                            val lat = tmp.house?.location!!.split(",")[0].trim().toDouble()
                            val lng = tmp.house?.location!!.split(",")[1].trim().toDouble()
                            if (calcDistance(lat, lng) <= dbUser?.area!!) {
                                tmpTrans += tmp
                            }
                        }
                    }
                    binding.noHousesTxt.isVisible = transactions.isNullOrEmpty()
                    transactions = tmpTrans
                }
                override fun onCancelled(error: DatabaseError) {
                    print("\n \n \n no matches \n \n \n")
                }
            })
    }

    private fun applyTransaction() {
        selectedTrans.cleanerID = user?.uid
        selectedTrans.status = "applying"
        database.getReference("Transactions/".plus(selectedTrans.transactionID)).setValue(selectedTrans)
            .addOnSuccessListener {
                Toast.makeText(activity, "Applied", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(activity, "Not applied", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calcDistance(lat: Double, lng: Double): Double{
        val radlat = dbUser?.location?.split(",")?.get(0)?.trim()?.toDouble()?.div((180/Math.PI))
        val radlng = dbUser?.location?.split(",")?.get(1)?.trim()?.toDouble()?.div((180/Math.PI))
        val radlat2 = lat / (180/Math.PI)
        val radlng2 = lng /(180/Math.PI)
        return 1.609344 * 3963.0 * acos((sin(radlat!!)*sin(radlat2))+
            cos(radlat) *cos(radlat2) *cos(radlng2- radlng!!))
    }

    fun updateDbUser() {
        database.getReference("Cleaners").child(user?.uid.toString())
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dbUser = snapshot.getValue(User::class.java)
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        getNearHouses()
    }
}