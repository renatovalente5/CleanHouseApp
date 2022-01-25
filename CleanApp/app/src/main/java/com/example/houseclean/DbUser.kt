package com.example.houseclean

import com.example.houseclean.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DbUser {
    companion object {
        val database = FirebaseDatabase.getInstance("https://housecleanaveiro-default-rtdb.europe-west1.firebasedatabase.app/")
        val userInst = FirebaseAuth.getInstance().currentUser
        fun withValue(dbUser: User) = let{
            database.getReference("Users".plus("/${userInst?.uid.toString()}"))
                .addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //it = snapshot.getValue(User::class.java)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        var dbUser = let {
            database.getReference("Users".plus("/${userInst?.uid.toString()}"))
                .addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //it.dbUser = snapshot.getValue(User::class.java)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }
}