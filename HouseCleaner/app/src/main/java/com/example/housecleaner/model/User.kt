package com.example.housecleaner.model

import java.io.Serializable

data class User(
    var UID: String? = null,
    var name: String? = null,
    val email: String? = null,
    var area: Int? = 100,
    var location: String? = "0.0, 0.0",
    var transactions: MutableList<Transaction>? = arrayListOf()
) : Serializable
