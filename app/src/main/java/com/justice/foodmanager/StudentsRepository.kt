package com.justice.foodmanager

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.justice.foodmanager.utils.FirebaseUtil
import com.justice.foodmanager.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.*

class StudentsRepository {

    private val TAG = "StudentsRepository"


    fun getCurrentStudent(id: String) = flow<Resource<DocumentSnapshot>> {
        Log.d(TAG, "getParent4: id:$id")
        FirebaseUtil.collectionReferenceMainStudents.document(id).get().await()?.let {
            emit(Resource.success(it!!))
        }
    }

    fun getStudents() = callbackFlow<Resource<QuerySnapshot>> {
        offer(Resource.loading(""))
        val listenerRegistration = FirebaseUtil.getStudents { querySnapshot, exception ->
            if (exception != null) {
                offer(Resource.error(exception))
            } else if (querySnapshot?.isEmpty!!) {
                offer(Resource.empty())
            } else {
                offer(Resource.success(querySnapshot))
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun deleteStudentMetaData(snapshot: DocumentSnapshot) = flow<Resource<DocumentSnapshot>> {
        try {
            snapshot.reference.delete().await()
            emit(Resource.success(snapshot))
        } catch (e: Exception) {
            emit(Resource.error(e))

        }

    }

    fun checkIsUserLoggedIn() = callbackFlow<Resource<FirebaseUser>> {
        Log.d(TAG, "checkIsUserLoggedIn: ")

        offer(Resource.loading(""))
        if (FirebaseUtil.isUserLoggedIn) {
            offer(Resource.success(FirebaseUtil.firebaseAuth.currentUser!!))
        } else {
            offer(Resource.error(Exception("User Not Logged In")))
        }

        awaitClose { }
    }

    fun getCurrentDate() = callbackFlow<Resource<Date>> {
        offer(Resource.loading(""))
        FirebaseUtil.getCurrentDate {
            if (it == null) {
                val date = Calendar.getInstance().time
                offer(Resource.success(date))
            } else {
                offer(Resource.success(it))

            }
        }
        awaitClose { }
    }

    suspend fun getCurrentDate2(): Date {
        val date = FirebaseUtil.getCurrentDate2()
        if (date == null) {
            return Calendar.getInstance().time
        } else {
            return date
        }

    }

    fun onCheckBoxClicked(snapshot: DocumentSnapshot, present: Boolean) =
        callbackFlow<Resource<DocumentSnapshot>> {

            val map = mapOf<String, Boolean>(PRESENT to present)
            snapshot.reference.set(map, SetOptions.merge()).addOnSuccessListener {
                offer(Resource.success(snapshot))
            }.addOnFailureListener {
                offer(Resource.error(it))
            }
            awaitClose { }
        }

    fun startFetchingData(currentInfo: CurrentInfo) = callbackFlow<Resource<DocumentSnapshot>> {
        FirebaseUtil.collectionReferenceDates.document(currentInfo.currentDateString).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    offer(Resource.success(it))
                } else {
                    offer(Resource.empty())
                }

            }.addOnFailureListener {
                offer(Resource.error(it))
            }
        awaitClose { }
    }

    fun documentExist(currentInfo: CurrentInfo, snapshot: DocumentSnapshot) =
        callbackFlow<Resource<List<DocumentSnapshot>>> {
            FirebaseUtil.getStudentsFromSpecificDateCollectionRef(currentInfo.currentDateString)
                .get().addOnSuccessListener {
                    offer(Resource.success(it.documents))
                }.addOnFailureListener {
                    offer(Resource.error(it))
                }
            awaitClose {

            }
        }

    fun documentDoesNotExist(currentInfo: CurrentInfo) =
        callbackFlow<Resource<List<DocumentSnapshot>>> {

            FirebaseUtil.collectionReferenceDates.document(currentInfo.currentDateString)
                .set(currentInfo).await()
            FirebaseUtil.collectionReferenceMainStudents.get().await()
                .forEach { queryDocumentSnapshot ->
                    val studentData = queryDocumentSnapshot.toObject(StudentData::class.java)
                    FirebaseUtil.addStudentToDateCollection(
                        studentData,
                        currentInfo.currentDateString
                    )
                }

            FirebaseUtil.getStudentsFromSpecificDateCollectionRef(currentInfo.currentDateString)
                .get()
                .addOnSuccessListener {

                }.addOnFailureListener {
                    offer(Resource.error(it))
                }

            awaitClose { }
        }

}
