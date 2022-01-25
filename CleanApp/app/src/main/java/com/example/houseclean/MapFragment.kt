package com.example.houseclean

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.houseclean.model.Transaction
import com.google.android.gms.maps.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions


class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private lateinit var mMap: GoogleMap
    private var currentMarker: Marker? = null
    private lateinit var transaction: Transaction
    private lateinit var mMapView: MapView
    private lateinit var googleMap: GoogleMap

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var rootView = inflater.inflate(R.layout.fragment_map, container, false)
        mMapView = rootView.findViewById<View>(R.id.google_map) as MapView
        mMapView.onCreate(savedInstanceState)
        mMapView.onResume()

        try {
            MapsInitializer.initialize(activity!!.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mMapView.getMapAsync { mMap ->
            googleMap = mMap

            // For showing a move to my location button
            if (ActivityCompat.checkSelfPermission(
                    activity!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    activity!!,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(activity, "Location permission needed!", Toast.LENGTH_SHORT).show()
            }
            googleMap.isMyLocationEnabled = true
            // For dropping a marker at a point on the Map
            //val aveiro = LatLng(40.64427, -8.64554)

            // For zooming automatically to the location of the marker
            //drawMarker(aveiro)
            //val cameraPosition = CameraPosition.Builder().target(aveiro).zoom(12f).build()
            //googleMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView!!.onLowMemory()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        print("\n \n map \n ready \n \n")
        fetchTransaction()
    }

    private fun fetchTransaction() {
        database.getReference("Transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    if (it.child("clientID").getValue(String::class.java).equals(user?.uid.toString())
                        && it.child("status").getValue(String::class.java).equals("onTheWay")) {
                            transaction = it.getValue(Transaction::class.java)!!
                            print("\n \n transaction \n complete \n \n")
                            while (mMap == null) {}
                            val lat : Double = transaction.location!!.split(",")[0].trim().toDouble()
                            val lng : Double = transaction.location!!.split(",")[1].trim().toDouble()
                            LatLng(lat, lng).apply {
                                if (currentMarker != null) currentMarker?.remove()
                                val markerOptions = MarkerOptions().position(this).title("Cleaner location").draggable(false)
                                mMap.animateCamera(CameraUpdateFactory.newLatLng(this))
                                currentMarker = mMap.addMarker(markerOptions)
                                currentMarker?.showInfoWindow()
                            }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun drawMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng).title("Cleaner location").draggable(false)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
        currentMarker = mMap.addMarker(markerOptions)
        currentMarker?.showInfoWindow()
    }
}