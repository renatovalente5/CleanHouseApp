package com.example.houseclean.model

import java.io.Serializable
import java.util.*

data class Transaction(
    val transactionID: Int? = null,
    val clientID: String? = null,
    var cleanerID: String? = null,
    var clientName: String? = null,
    var house: House? = null,
    var location: String? = null,
    //cleaned but not completed
    var status: String? = null,
    //completed after read qr code
    var completed: Boolean? = false
) : Serializable