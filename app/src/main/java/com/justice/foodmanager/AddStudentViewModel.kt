package com.justice.foodmanager

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
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
                        _addStudentStatus.send(Resource.loading("started the uploading parent"))
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
        _addStudentStatus.send(Resource.loading("adding student"))
        try {
            FirebaseUtil.addStudentToMainCollection(student)
        } catch (e: Exception) {
            _addStudentStatus.send(Resource.error(e))
        }
        try {
            FirebaseUtil.addStudentToDateCollection(student, date)
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