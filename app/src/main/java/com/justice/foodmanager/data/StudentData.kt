package com.justice.foodmanager.data

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.android.parcel.Parcelize

const val PRESENT = "present"

@Parcelize
data class StudentData(
    @DocumentId val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gradeClass: String = "1",
    var present: Boolean = false
) : Parcelable
