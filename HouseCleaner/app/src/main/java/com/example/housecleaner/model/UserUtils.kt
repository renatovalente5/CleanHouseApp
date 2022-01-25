package com.example.housecleaner.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserUtils {
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
    companion object {
        lateinit var dbUser: User
    }

    init {
        database.getReference("Cleaners/${user?.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dbUser = snapshot.getValue(User::class.java)!!
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}