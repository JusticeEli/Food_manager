package com.justice.foodmanager

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.google.firebase.firestore.DocumentSnapshot
import com.justice.foodmanager.utils.FirebaseUtil
import com.justice.foodmanager.utils.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class EditStudentViewModel @ViewModelInject constructor(
    private val repository: StudentsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {


    private val TAG = "EditStudentViewModel"


    val currentStudent =
        repository.getCurrentStudent(savedStateHandle.get<StudentData>(StudentsFragment.STUDENT_ARGS)!!.id!!)

    private val _editStudentStatus = Channel<Resource<StudentData>>()
    val editStudentStatus = _editStudentStatus.receiveAsFlow()

    private val _currentSnapshot = MutableLiveData<DocumentSnapshot>()
    val currentSnapshot = _currentSnapshot as LiveData<DocumentSnapshot>
    fun setCurrentSnapshot(snapshot: DocumentSnapshot) {
        _currentSnapshot.value = snapshot
    }

    fun setEvent(event: Event) {
        viewModelScope.launch {
            when (event) {

                is Event.StudentEditSubmitClicked -> {
                    if (fieldsAreEmpty(event.student)) {
                        _editStudentStatus.send(Resource.empty())
                    } else {
                        trimDataAndSaveIntoDatabase(event.student, event.date)
                    }
                }
            }
        }

    }

    private suspend fun trimDataAndSaveIntoDatabase(data: StudentData, date: String) {
        val student = data.copy(firstName = data.firstName.trim(), lastName = data.lastName.trim())
        updateStudent(student, date)
    }

    private suspend fun updateStudent(student: StudentData, date: String) {
        _editStudentStatus.send(Resource.loading("editing student"))
        try {
            FirebaseUtil.updateStudentToMainCollection(student.copy(present = false))
        } catch (e: Exception) {
            _editStudentStatus.send(Resource.error(e))
        }
        try {
            FirebaseUtil.updateStudentToDateCollection(student, date)
        } catch (e: Exception) {
            _editStudentStatus.send(Resource.error(e))
        }

        _editStudentStatus.send(Resource.success(student))
    }

    private fun fieldsAreEmpty(student: StudentData) =
        student.firstName.isBlank() || student.lastName.isBlank()

    sealed class Event {
        data class StudentEditSubmitClicked(val student: StudentData, val date: String) :
            Event()
    }

}