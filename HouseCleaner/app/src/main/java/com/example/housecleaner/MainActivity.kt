package com.example.housecleaner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.example.housecleaner.model.User
import com.example.housecleaner.databinding.ActivityMainBinding
import com.example.housecleaner.model.Transaction
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File

class MainActivity : AppCompatActivity() {
    val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private val user = FirebaseAuth.getInstance().currentUser
    private lateinit var binding: ActivityMainBinding
    private val mapFragment = MapFragment()
    private val inboxFragment = InboxFragment()
    private val housesFragment = HousesFragment()
    private val perfilFragment = PerfilFragment()
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var builder: LocationSettingsRequest.Builder
    var not_Trans: Transaction? = null
    lateinit var dbUser: User
    private var granted = false
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        granted = permissions[Manifest.permission.INTERNET]!! &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION]!! &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION]!!
        if (!granted) {
            Toast.makeText(this, "Location permission needed!", Toast.LENGTH_SHORT).show()
        }
    }
    private val readQr = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        initDbValues()
        setUpTapBar()

        getPermissions()
        enableLocation()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        checkTransactions()

        binding.bottomNavBar.setItemSelected(R.id.nav_inbox)
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, inboxFragment)
            commit()
        }
    }

    private fun setUpTapBar() {
        binding.bottomNavBar.setOnItemSelectedListener {
            when(it) {
                R.id.nav_map -> {
                    LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
                        .addOnSuccessListener { res ->
                            if (res.locationSettingsStates?.isLocationPresent == true && granted)
                                supportFragmentManager.beginTransaction().apply {
                                    replace(R.id.flFragment, mapFragment)
                                    commit()
                                }
                            else
                                Toast.makeText(this,
                                    "Location permissions and/or Location settings disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                    binding.bottomNavBar.dismissBadge(R.id.nav_map)
                }
                R.id.nav_inbox -> {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flFragment, inboxFragment)
                        commit()
                    }
                    binding.bottomNavBar.dismissBadge(R.id.nav_inbox)
                }
                R.id.nav_homes -> {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flFragment, housesFragment)
                        commit()
                    }
                }
                R.id.nav_perfil -> {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flFragment, perfilFragment)
                        commit()
                    }
                }
            }
        }
    }

    private fun initDbValues() {
        database.getReference("Cleaners").child(user?.uid.toString())
            .addValueEventListener(object: ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.N)
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tmpUser = snapshot.getValue(User::class.java)
                    dbUser = tmpUser!!
                    fetchLocation()
                    database.getReference("Cleaners").child(user?.uid.toString())
                        .setValue(dbUser)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkTransactions() {
        database.getReference("Transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val status = it.child("cleanerID").getValue(String::class.java)
                    print("\n STATUS: \n $status")
                    if (status.equals(user?.uid.toString())) {
                        val tmpTrans = it.getValue(Transaction::class.java)
                        print(tmpTrans.toString())
                        if (it.child("status").getValue(String::class.java).equals("onTheWay")) {
                            not_Trans = it.getValue(Transaction::class.java)
                            not_Trans?.location = dbUser.location
                            database.getReference("Transactions").child("${not_Trans?.transactionID}").setValue(not_Trans)
                        }
                        if (status == "arrived") {
                            val intent = Intent(this@MainActivity, QrScannerActivity::class.java)
                            intent.putExtra("qr", "${tmpTrans?.status} house ${tmpTrans?.house?.ID}")
                            print("\n \n lauching \n activity \n")
                            readQr.launch(intent)
                        }
                        if (status.equals("canceled")) {
                            if (not_Trans?.transactionID == tmpTrans?.transactionID)
                                not_Trans = null
                            Toast.makeText(this@MainActivity, "Apply canceled", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun not(i: Int) {
        when(i) {
            0 -> binding.bottomNavBar.showBadge(R.id.nav_map)
            1 -> binding.bottomNavBar.showBadge(R.id.nav_inbox)
        }
    }

    fun createImageFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return  File.createTempFile("profilePic", ".png", storageDir)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getPermissions() {
        requestPermission.launch(arrayOf(Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun enableLocation() {
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000L
        locationRequest.fastestInterval = 2000L
        builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            .setAlwaysShow(true)
        LocationServices.getSettingsClient(applicationContext)
            .checkLocationSettings(builder.build())
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        e.startResolutionForResult(this, 1001)
                    } catch (sendEx: IntentSender.SendIntentException) {
                    }
                }
            }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!granted) {
                getPermissions()
            }
        } else {
            if (isLocationEnabled()) {
                fusedLocationProviderClient!!.lastLocation.addOnSuccessListener {
                    if(it != null) {
                        dbUser.location = it.latitude.toString().plus(",").plus(it.longitude)
                    }
                }.addOnFailureListener {
                    showMapDialog()
                }
            } else enableLocation()
        }
    }

    private fun showMapDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Error getting location")
        builder.setNeutralButton("ok") {_, _ ->
            finish()
        }
        builder.create().setCancelable(true)
        builder.show()
    }
}