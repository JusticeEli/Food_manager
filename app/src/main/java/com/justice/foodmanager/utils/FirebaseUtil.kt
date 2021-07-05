package com.justice.foodmanager.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ServerTimestamp
import com.justice.foodmanager.StudentData
import kotlinx.coroutines.tasks.await
import java.util.*

object FirebaseUtil {

    private val firebaseFirestore = FirebaseFirestore.getInstance()
    val firebaseAuth = FirebaseAuth.getInstance()

    //   private val ROOT="root"
    private val COLLECTION_STUDENTS = "students"
    private val COLLECTION_DATES = "dates"


    val isUserLoggedIn: Boolean
        get() {
            return firebaseAuth.currentUser != null
        }

    val collectionReferenceMainStudents get() = firebaseFirestore.collection(COLLECTION_STUDENTS)
    val collectionReferenceDates get() = firebaseFirestore.collection(COLLECTION_DATES)
    fun getStudents(onComplete: (QuerySnapshot?, Exception?) -> Unit): ListenerRegistration {
        return collectionReferenceMainStudents.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            onComplete(querySnapshot, firebaseFirestoreException)
        }

    }

    fun getCurrentDate(onComplete: (Date?) -> Unit) {

        FirebaseFirestore.getInstance().collection("dummy").document("date").set(CurrentDate())
            .addOnSuccessListener {


                FirebaseFirestore.getInstance().collection("dummy").document("date").get()
                    .addOnSuccessListener {

                        onComplete(it.toObject(CurrentDate::class.java)?.date)

                    }
            }


    }

    suspend fun getCurrentDate2(): Date? {

        FirebaseFirestore.getInstance().collection("dummy").document("date").set(CurrentDate())
            .await()

        val date =
            FirebaseFirestore.getInstance().collection("dummy").document("date").get().await()
                .toObject(CurrentDate::class.java)?.date

        return date
    }

    fun getStudentsFromSpecificDateCollectionRef(date: String) =
        collectionReferenceDates.document(date).collection(COLLECTION_STUDENTS)

    suspend fun addStudentToMainCollection(studentData: StudentData) =
        collectionReferenceMainStudents.add(studentData).await()

    suspend fun addStudentToDateCollection(studentData: StudentData, date: String) =
        collectionReferenceDates.document(date).collection(
            COLLECTION_STUDENTS
        ).add(studentData).await()
    suspend fun updateStudentToMainCollection(studentData: StudentData) =
        collectionReferenceMainStudents.document(studentData.id).set(studentData).await()

    suspend fun updateStudentToDateCollection(studentData: StudentData, date: String) =
        collectionReferenceDates.document(date).collection(
            COLLECTION_STUDENTS
        ).document(studentData.id).set(studentData).await()
}

data class CurrentDate(@ServerTimestamp val date: Date? = null)