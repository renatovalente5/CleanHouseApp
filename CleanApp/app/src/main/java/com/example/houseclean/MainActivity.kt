package com.example.houseclean

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.example.houseclean.databinding.ActivityMainBinding
import com.example.houseclean.model.Transaction
import com.example.houseclean.model.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    private val user = FirebaseAuth.getInstance().currentUser
    private lateinit var binding: ActivityMainBinding
    private val mapFragment = MapFragment()
    private val inboxFragment = InboxFragment()
    private val housesFragment = HousesFragment()
    private val perfilFragment = PerfilFragment()
    private lateinit var dialog: AlertDialog
    private lateinit var qrDialog: AlertDialog
    private var dbUser: User? = null
    private var not_Trans: Transaction? = null
    private lateinit var ivQr: ImageView
    private val NOTIFICATION_ID = 888
    private lateinit var mNotificationManagerCompact: NotificationManagerCompat
    private var granted = false
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        granted = permissions[Manifest.permission.INTERNET]!! &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION]!! &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION]!!
        if (!granted) {
            Toast.makeText(this, "Location permission needed!", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        initDbValues()
        GlobalScope.launch(Dispatchers.IO){
            val ref = database.getReference("Users").child(user?.uid.toString())
            withContext(Dispatchers.Main) {
                dbUser = ref.get().await().getValue(User::class.java)
            }
        }
        setUpTapBar()
        checkTransactions()
        getPermissions()

        mNotificationManagerCompact = NotificationManagerCompat.from(this)

        val builder = AlertDialog.Builder(this)
        val builder2 = AlertDialog.Builder(this)
        val view = View.inflate(this, R.layout.apply_house_dialog, null)
        val view2 = View.inflate(this, R.layout.qr_dialog, null)
        dialog = builder.setView(view).setCancelable(false).create()
        qrDialog = builder2.setView(view2).setCancelable(true).create()
        val btn1 = view.findViewById<FloatingActionButton>(R.id.acceptBtn)
        val btn2 = view.findViewById<FloatingActionButton>(R.id.cancelBtn)
        ivQr = view2.findViewById(R.id.qrImg)
        btn1.setOnClickListener {
            updateTransStatus(1)
            dialog.dismiss()
        }
        btn2.setOnClickListener {
            updateTransStatus(0)
            dialog.dismiss()
        }

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
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flFragment, mapFragment)
                        commit()
                    }
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

    private fun checkTransactions() {
        database.getReference("Transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                   if (it.child("clientID").getValue(String::class.java).equals(user?.uid.toString())) {
                       val tmpTrans = it.getValue(Transaction::class.java)
                       if (tmpTrans?.status.equals("applying")) {
                           generateInboxStyleNotification(0, "Cleaner ${tmpTrans?.cleanerID} applying")
                           not_Trans = tmpTrans
                           dialog.show()
                           dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                       }
                       if (tmpTrans?.status.equals("arrived")) {
                           generateInboxStyleNotification(1, "Cleaner arrived at House ${tmpTrans?.house?.ID}")
                           generateQr("${tmpTrans?.status} house ${tmpTrans?.house?.ID}")
                       }
                   }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun generateQr(code: String? = null) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(code, BarcodeFormat.QR_CODE, 512, 512)
            val bmp = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            ivQr.setImageBitmap(bmp)
            qrDialog.show()
        }catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun updateTransStatus(status: Int) {
        if (status == 1) {
            not_Trans?.status = "onTheWay"
            database.getReference("Transactions").child("${not_Trans?.transactionID}").setValue(not_Trans)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cleaner on the way", Toast.LENGTH_SHORT).show()
                }
        } else if (status == 0) {
            not_Trans?.status = "canceled"
            database.getReference("Transactions").child("${not_Trans?.transactionID}").setValue(not_Trans)
                .addOnSuccessListener {
                    Toast.makeText(this, "Transaction canceled", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun initDbValues() {
        database.getReference("Users").child(user?.uid.toString())
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tmpUser = snapshot.getValue(User::class.java)
                    dbUser = tmpUser
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

    private fun generateInboxStyleNotification(it: Int, msg: String){
        val notificationChannelId: String = NotificationUtil().createInboxStyleNotificationChannel(this)
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(InboxStyleMockData.mBigConstentTitle)
            .setSummaryText(InboxStyleMockData.mSummaryText)
        inboxStyle.addLine(msg)
        val mainIntent = Intent(this,MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,0,mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationCompatBuilder = NotificationCompat.Builder(
            this.applicationContext, notificationChannelId
        )
        notificationCompatBuilder.setStyle(inboxStyle)
            .setContentTitle(if (it == 0) "Applying" else if (it == 1) "Arrived" else "Notification")
            .setContentText(if (it == 0) "Applying" else if (it == 1) "Arrived" else "Notification")
            .setSmallIcon(R.drawable.ic_inbox)
            .setLargeIcon(BitmapFactory.decodeResource(resources,R.drawable.ic_perfil))
            .setContentIntent(mainPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(ContextCompat.getColor(this.applicationContext,R.color.purple_500))
            .setSubText("")
            .setCategory(Notification.CATEGORY_EMAIL)
            .setPriority(InboxStyleMockData.mPriority)
            .setVisibility(InboxStyleMockData.mChannelLockscreenVisibility)
        val notification = notificationCompatBuilder.build()
        mNotificationManagerCompact.notify(NOTIFICATION_ID, notification)
    }
}