package com.example.housecleaner.model

import java.io.Serializable

data class House(
    var ID: String? = null,
    var location : String? = null,
    var address: String? = null,
    var deleted: Boolean? = false
) : Serializable