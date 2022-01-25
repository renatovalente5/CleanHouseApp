package com.example.houseclean.model

import java.io.Serializable


data class User(
    var UID: String? = null,
    var name: String? = null,
    val email: String? = null,
    var houses: MutableList<House>? = arrayListOf(),
    //var transactions: MutableList<Transaction>? = arrayListOf()
) : Serializable