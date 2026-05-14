package com.suplz.yadro.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AidlContact(
    val id: String,
    val name: String,
    val phoneNumber: String
) : Parcelable