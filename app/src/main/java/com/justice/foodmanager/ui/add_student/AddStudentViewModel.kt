package com.justice.foodmanager.ui.add_student

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentReference
import com.justice.foodmanager.data.StudentData
import com.justice.foodmanager.data.StudentsRepository
import com.justice.foodmanager.utils.FirebaseUtil
import com.justice.foodmanager.utils.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AddStudentViewModel @ViewModelInject constructor(private val repository: StudentsRepository) :
    ViewModel() {
    private val TAG = "AddStudentViewModel"


    private val _addStudentEvents = Channel<Event>()
    val addStudentEvents = _addStudentEvents.receiveAsFlow()
    private val _addStudentStatus = Channel<Resource<StudentData>>()
    val addStudentStatus = _addStudentStatus.receiveAsFlow()

    fun setEvent(event: Event) {
        viewModelScope.launch {
            when (event) {

                is Event.StudentAddSubmitClicked -> {
                    if (fieldsAreEmpty(event.student)) {
                        _addStudentStatus.send(Resource.empty())
                    } else {
                        trimDataAndSaveIntoDatabase(event.student, event.date)
                    }

                }
            }
        }

    }

    private suspend fun trimDataAndSaveIntoDatabase(data: StudentData, date: String) {
        val student = data.copy(firstName = data.firstName.trim(), lastName = data.lastName.trim())
        uploadStudent(student, date)
    }

    private suspend fun uploadStudent(student: StudentData, date: String) {
        Log.d(TAG, "uploadStudent: student:$student : :date:$date")
        _addStudentStatus.send(Resource.loading("adding student"))
        val documentReference: DocumentReference =
            try {
                FirebaseUtil.addStudentToMainCollection(student)
            } catch (e: Exception) {
                _addStudentStatus.send(Resource.error(e))
                throw Exception("shit")
            }
        val studentDate = student.copy(id = documentReference.id)
        try {
            FirebaseUtil.addStudentToDateCollection(studentDate, date)
            _addStudentStatus.send(Resource.success(student))
        } catch (e: Exception) {
            _addStudentStatus.send(Resource.error(e))
        }

    }

    private fun fieldsAreEmpty(student: StudentData) =
        student.firstName.isBlank() || student.lastName.isBlank()

    sealed class Event {
        data class StudentAddSubmitClicked(val student: StudentData, val date: String) : Event()
    }

}