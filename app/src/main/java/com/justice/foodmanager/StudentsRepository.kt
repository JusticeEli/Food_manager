package com.justice.foodmanager

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
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
    private lateinit var listener: ListenerRegistration

    fun getCurrentStudent2(id: String) = flow<Resource<DocumentSnapshot>> {
        Log.d(TAG, "getParent4: id:$id")
        FirebaseUtil.collectionReferenceMainStudents.document(id).get().await()?.let {
            Log.d(TAG, "getCurrentStudent:id:${it.id} ")
            emit(Resource.success(it!!))
        }
        Log.d(TAG, "getCurrentStudent: end")
    }

    fun getCurrentStudent3(id: String) = flow<Resource<DocumentSnapshot>> {
        Log.d(TAG, "getParent4: id:$id")
        val document = FirebaseUtil.collectionReferenceMainStudents.document(id).get().await()
        Log.d(TAG, "getCurrentStudent:id:${document?.id} ")
        emit(Resource.success(document))
        Log.d(TAG, "getCurrentStudent: end")
    }

    fun getCurrentStudent(id: String) = callbackFlow<Resource<DocumentSnapshot>> {
        resetListener()
        Log.d(TAG, "getParent4: id:$id")
        listener = FirebaseUtil.collectionReferenceMainStudents.document(id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    offer(Resource.error(error))
                } else {
                    offer(Resource.success(value!!))
                }
            }
        awaitClose { resetListener() }
    }

    private fun resetListener() {
        if (::listener.isInitialized) {

            try {
                listener.remove()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getStudents() = callbackFlow<Resource<QuerySnapshot>> {
        resetListener()
        offer(Resource.loading(""))
        listener = FirebaseUtil.getStudents { querySnapshot, exception ->
            if (exception != null) {
                offer(Resource.error(exception))
            } else if (querySnapshot?.isEmpty!!) {
                offer(Resource.empty())
            } else {
                offer(Resource.success(querySnapshot))
            }
        }

        awaitClose {
            resetListener()
        }
    }

    fun deleteStudentMetaData(snapshot: DocumentSnapshot) = flow<Resource<DocumentSnapshot>> {
        try {
            snapshot.reference.delete().await()
            FirebaseUtil.collectionReferenceMainStudents.document(snapshot.id).delete().await()
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

    fun getCurrentDate() = Calendar.getInstance()

    fun getCurrentDate5() = Calendar.getInstance().time
    suspend fun getCurrentDate2(): Date {
        val date = FirebaseUtil.getCurrentDate2()
        if (date == null) {
            return Calendar.getInstance().time
        } else {
            return date
        }

    }

    fun onCheckBoxClicked2(snapshot: DocumentSnapshot, present: Boolean) =
        callbackFlow<Resource<DocumentSnapshot>> {

            val map = mapOf<String, Boolean>(PRESENT to present)
            Log.d(TAG, "onCheckBoxClicked: map:$map")
            snapshot.reference.set(map, SetOptions.merge()).addOnSuccessListener {
                Log.d(TAG, "onCheckBoxClicked:success ")
                offer(Resource.success(snapshot))
            }.addOnFailureListener {
                offer(Resource.error(it))
            }
            awaitClose { }
        }

    fun onCheckBoxClicked(snapshot: DocumentSnapshot, present: Boolean) =
        flow<Resource<DocumentSnapshot>> {
            val map = mapOf<String, Boolean>(PRESENT to present)
            Log.d(TAG, "onCheckBoxClicked: map:$map")
            snapshot.reference.set(map, SetOptions.merge()).await()
            emit(Resource.success(snapshot))
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

    fun documentExist(currentInfo: CurrentInfo) =
        callbackFlow<Resource<List<DocumentSnapshot>>> {
            resetListener()
            Log.d(TAG, "documentExist: currentInfo:$currentInfo")
            listener =
                FirebaseUtil.getStudentsFromSpecificDateCollectionRef(currentInfo.currentDateString)
                    .addSnapshotListener { value, error ->

                        if (error != null) {
                            offer(Resource.error(error))

                        } else {
                            Log.d(TAG, "documentExist: size:${value!!.size()}")
                            offer(Resource.success(value.documents))

                        }
                    }

            awaitClose {
                resetListener()
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

            ////
            resetListener()
            listener =
                FirebaseUtil.getStudentsFromSpecificDateCollectionRef(currentInfo.currentDateString)
                    .addSnapshotListener { value, error ->

                        if (error != null) {
                            offer(Resource.error(error))

                        } else {
                            Log.d(TAG, "documentExist: size:${value!!.size()}")
                            offer(Resource.success(value.documents))

                        }
                    }

            awaitClose {
                resetListener()
            }
        }

}
