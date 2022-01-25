package com.example.houseclean

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.houseclean.databinding.ActivityGetLocationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import java.util.*

class GetLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var binding: ActivityGetLocationBinding
    private var granted = false
    @RequiresApi(Build.VERSION_CODES.N)
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        granted = permissions[Manifest.permission.INTERNET]!! &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION]!! &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION]!!
        if (!granted) {
            Toast.makeText(this, "Location permission needed!", Toast.LENGTH_SHORT).show()
            val intent = Intent()
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }
    }
    private var currentLocation: Location? = null
    private var currentLatLng: String? = null
    private var currentMarker: Marker? = null
    private var addressLine: String? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        getPermissions()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocation()

        binding.addLocationBtn.setOnClickListener{
            val intent = Intent()
            if (addressLine == null || currentLatLng == null){
                setResult(RESULT_CANCELED, intent)
                finish()
            }
            intent.putExtra("address", addressLine)
            intent.putExtra("location", currentLatLng)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val latLng: LatLng = if (currentLocation == null) {
            LatLng("0".toDouble(), "0".toDouble())
        } else {
            LatLng(currentLocation?.latitude!!, currentLocation?.longitude!!)
        }

        drawMarker(latLng)

        mMap.setOnMapLongClickListener { latLng ->
            currentLatLng = latLng.latitude.toString().plus(",").plus(latLng.longitude)
            if (currentMarker != null) currentMarker?.remove()
            drawMarker(latLng)
        }
    }

    private fun drawMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng).title("My house").snippet(getAddress(latLng.latitude, latLng.longitude)).draggable(true)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
        currentMarker = mMap.addMarker(markerOptions)
        currentMarker?.showInfoWindow()
    }

    private fun getAddress(lat: Double, lng: Double): String {
        val geoCoder = Geocoder(this, Locale.getDefault())
        val addresses = geoCoder.getFromLocation(lat, lng, 1)
        val adrLn = addresses[0].getAddressLine(0).toString()
        addressLine = adrLn
        return adrLn
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getPermissions() {
        requestPermission.launch(arrayOf(Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
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
                val intent = Intent()
                setResult(RESULT_CANCELED, intent)
                finish()
            }
        } else {
            if (isLocationEnabled()) {
                fusedLocationProviderClient!!.lastLocation.addOnSuccessListener {
                    if(it != null) {
                        this.currentLocation = it
                        this.currentLatLng = it.latitude.toString().plus(",").plus(it.longitude)
                        val mapFragment = supportFragmentManager.findFragmentById(R.id.getLocationMap) as SupportMapFragment
                        mapFragment.getMapAsync(this@GetLocationActivity)
                    }
                }.addOnFailureListener {
                    showMapDialog()
                }
            } else Toast.makeText(this, "Please enable location", Toast.LENGTH_SHORT).show()
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