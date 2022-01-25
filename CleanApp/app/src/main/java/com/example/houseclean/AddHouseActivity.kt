package com.example.houseclean

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.houseclean.databinding.ActivityAddHouseBinding
import com.example.houseclean.model.House
import com.example.houseclean.model.User
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import java.io.File

class AddHouseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHouseBinding
    private val user = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private val storage = FirebaseStorage.getInstance().reference
    private var granted = false
    private lateinit var locationRequest: LocationRequest
    private lateinit var builder: LocationSettingsRequest.Builder
    @RequiresApi(Build.VERSION_CODES.N)
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        granted = permissions[android.Manifest.permission.CAMERA]!! &&
                permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]!! &&
                permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE]!!
        if (!granted) Toast.makeText(this, "Camera/Gallery permission needed!", Toast.LENGTH_SHORT).show()
    }
    private lateinit var tmpImageUri: Uri
    private var tmpImageFilePath = ""
    private val selectImg = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            tmpImageUri = it
            updateImage()
        }
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            updateImage()
        }
    }
    private val getLocation = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                binding.etHouseAddress.setText(it.data?.getStringExtra("address").toString())
                binding.houseLocation.setText(it.data?.getStringExtra("location").toString())
            } else {
                Toast.makeText(this, "Fail getting address/location!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Canceled!", Toast.LENGTH_SHORT).show()
        }
    }
    private lateinit var dbUser: User
    private lateinit var houseID: String

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHouseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        dbUser = intent.getSerializableExtra("user") as User
        houseID = intent.getStringExtra("houseID").toString()

        print("\n \n \n user uid: "+dbUser.UID+" \n \n \n")
        print("\n \n \n houseID: "+houseID+" \n \n \n")

        binding.addHouseImg.setOnClickListener {
            getCameraPermissions()
            showImagePicDialog()
        }

        binding.houseLocation.setOnClickListener {
            locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 5000L
            locationRequest.fastestInterval = 2000L
            builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                .setAlwaysShow(true)
            LocationServices.getSettingsClient(applicationContext)
                .checkLocationSettings(builder.build())
                .addOnSuccessListener {
                    if (it.locationSettingsStates?.isLocationPresent == true)
                        getLocation.launch(Intent(this, GetLocationActivity::class.java))
                }.addOnFailureListener { e ->
                    if (e is ResolvableApiException) {
                        try {
                            e.startResolutionForResult(this, 1001)
                        } catch (sendEx: IntentSender.SendIntentException) {
                        }
                    }
                }
        }

        binding.cancelBtn.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.addHouseCheckBtn.setOnClickListener{
            if (TextUtils.isEmpty(binding.etHouseAddress.text.toString().trim())) {
                Toast.makeText(this,"Please enter address!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.houseLocation.text.toString() == "Add location" || binding.houseLocation.text.toString() == "null") {
                Toast.makeText(this,"Please add location!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val house = House(houseID, binding.houseLocation.text.toString(), binding.etHouseAddress.text.toString())
            with(dbUser){
                if (houses.isNullOrEmpty()) houses = arrayListOf(house)
                else dbUser.houses?.add(house)
            }
            database.getReference("Users").child(user.uid.toString())
                .setValue(dbUser).addOnSuccessListener{
                    Toast.makeText(this, "House add success!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener{
                    Toast.makeText(this, "Error adding house!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            uploadHouseImage(tmpImageUri)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showImagePicDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Image From")
        builder.setItems(arrayOf("Camera", "Gallery"), object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                when(which) {
                    0 -> {
                        if (!granted) getCameraPermissions()
                        else {
                            tmpImageUri = FileProvider.getUriForFile(this@AddHouseActivity,
                                "com.example.houseclean.provider",
                                createImageFile(houseID).also {
                                    tmpImageFilePath = it.absolutePath
                                }
                            )
                            takePicture.launch(tmpImageUri)
                        }
                    }
                    1 -> {
                        if (!granted) getCameraPermissions()
                        else selectImg.launch("images/*")
                    }
                }
            }
        })
        builder.create().setCancelable(true)
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (resultCode == Activity.RESULT_OK) {
                GlobalScope.launch(Dispatchers.IO) {
                    delay3s()
                }
            }
        }
    }

    private suspend fun delay3s() {
        delay(3000L)
        getLocation.launch(Intent(this, GetLocationActivity::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getCameraPermissions() {
        requestPermission.launch(arrayOf(android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    private fun createImageFile(id: String): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return  File.createTempFile("house".plus(id), ".png", storageDir)
    }

    private fun uploadHouseImage(uri: Uri) {
        storage.child(user.uid.toString().plus("/houses/").plus(houseID).plus("/housePic")).putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(this, "Imagede upload success!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Imaged upload failed!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateImage() {
        Glide.with(binding.addHouseImg).load(tmpImageUri).into(binding.addHouseImg)
    }
}
