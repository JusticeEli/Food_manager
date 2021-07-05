package com.justice.foodmanager


import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.justice.foodmanager.utils.Resource
import com.justice.foodmanager.utils.exhaustive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*


class StudentsViewModel @ViewModelInject constructor(
    private val repository: StudentsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = "StudentsViewModel"
    val getStudents = repository.getStudents()

    val getCurrentDate = repository.getCurrentDate()
    private val _registerEvents = Channel<Event>()

    private val _studentsEvents = Channel<Event>()
    val studentsEvents = _studentsEvents.receiveAsFlow()


    fun setEvent(event: Event) {
        viewModelScope.launch {
            when (event) {
                is Event.StudentClicked -> {
                    _studentsEvents.send(Event.StudentClicked(event.parentSnapshot))
                }
                is Event.StudentEdit -> {
                    _studentsEvents.send(Event.StudentEdit(event.parentSnapshot))

                }
                is Event.StudentDelete -> {
                    _studentsEvents.send(Event.StudentDelete(event.parentSnapshot))

                }
                is Event.StudentDeleteConfirmed -> {
                    deleteStudent(event.parentSnapshot)
                }
                is Event.StudentSwiped -> {
                    _studentsEvents.send(Event.StudentSwiped(event.parentSnapshot))

                }
                is Event.StudentQuery -> {
                    startQuery(event.query)
                }
                Event.AddStudent -> {
                    _studentsEvents.send(Event.AddStudent)

                }
                is Event.DateClicked -> {
                    _registerEvents.send(Event.DateClicked)

                }
                is Event.CorrectDateChoosen -> {
                    checkIfWeHaveChoosenCorrectDate(event.currentInfo)
                }
                is Event.ClassSelected -> {
                    _registerEvents.send(Event.ClassSelected(event.classGrade))
                }
                is Event.TabSelected -> {
                    _registerEvents.send(Event.TabSelected(event.tab))
                }
                is Event.FetchData -> {

                    startFetchingData(event.currentInfo)
                }
                is Event.CheckBoxClicked -> {

                    onCheckBoxClicked(event.snapshot, event.present)
                }
            }
        }


    }

    private val _deleteStudentStatus = Channel<Resource<DocumentSnapshot>>()
    val deleteStudentStatus = _deleteStudentStatus.receiveAsFlow()
    private suspend fun deleteStudent(snapshot: DocumentSnapshot) {
        deleteStudentMetaData(snapshot)
    }

    private suspend fun deleteStudentMetaData(parentSnapshot: DocumentSnapshot) {
        _deleteStudentStatus.send(Resource.loading("deleting student..."))
        repository.deleteStudentMetaData(parentSnapshot).collect {
            _deleteStudentStatus.send(it)
        }
    }


    private val _currentListLiveData = MutableLiveData<List<DocumentSnapshot>>()
    val currentListLiveData get() = _currentListLiveData
    fun setCurrentListLiveData(documents: List<DocumentSnapshot>?) {
        currentListLiveData.value = documents
    }

    private val _studentQueryStatus = Channel<Resource<List<DocumentSnapshot>>>()
    val studentQueryStatus = _studentQueryStatus.receiveAsFlow()

    private suspend fun startQuery(query: String) {
        if (query.isBlank()) {
            _studentQueryStatus.send(Resource.empty())
        } else {
            val filteredList = mutableListOf<DocumentSnapshot>()
            currentListLiveData.value?.forEach {
                val student = it.toObject(StudentData::class.java)!!
                if (student.firstName.toLowerCase()
                        .contains(query.toLowerCase()) || student.lastName.toLowerCase()
                        .contains(query.toLowerCase())
                ) {
                    filteredList.add(it)
                }
            }

            _studentQueryStatus.send(Resource.success(filteredList))

        }
    }

    private suspend fun onCheckBoxClicked(snapshot: DocumentSnapshot, present: Boolean) {

        repository.onCheckBoxClicked(snapshot, present).collect {
            Log.d(TAG, "onCheckBoxClicked: ${it.status.name}")
            when (it.status) {
                Resource.Status.LOADING -> {

                }
                Resource.Status.SUCCESS -> {

                }
                Resource.Status.ERROR -> {

                }
            }
        }
    }

    private suspend fun startFetchingData(currentInfo: CurrentInfo) {


        repository.startFetchingData(currentInfo).collect {
            Log.d(TAG, "startFetchingData: ${it.status.name}")
            when (it.status) {
                Resource.Status.LOADING -> {

                }
                Resource.Status.SUCCESS -> {
                    documentExists(currentInfo, it.data!!)
                }
                Resource.Status.EMPTY -> {
                    //document does not exist
                    documentDoesNotExist(currentInfo)

                }
                Resource.Status.ERROR -> {

                }
            }
        }

    }

    private suspend fun documentDoesNotExist(currentInfo: CurrentInfo) {
        repository.documentDoesNotExist(currentInfo).collect {
            when (it.status) {
                Resource.Status.LOADING -> {

                }
                Resource.Status.SUCCESS -> {
                    _fetchDataStatus.send(it)

                }
                Resource.Status.ERROR -> {
                    _fetchDataStatus.send(it)
                }
            }
        }
    }

    private suspend fun documentExists(currentInfo: CurrentInfo, snapshot: DocumentSnapshot) {
        repository.documentExist(currentInfo, snapshot).collect {
            when (it.status) {
                Resource.Status.LOADING -> {

                }
                Resource.Status.SUCCESS -> {
                    //
                    successFilterResult(it)

                }
                Resource.Status.ERROR -> {
                    _fetchDataStatus.send(it)
                }
            }

        }
    }

    private suspend fun successFilterResult(it: Resource<List<DocumentSnapshot>>) {
        _fetchDataStatus.send(it)
    }

    private suspend fun checkIfWeHaveChoosenCorrectDate(currentInfo: CurrentInfo) {
        //check if we have choosen a future date and reject it if its future date
///checks if we are on same day
        val choosenDate = currentInfo.dateChoosen!!
        val currentDateServer = repository.getCurrentDate2()
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = choosenDate
        cal2.time = currentDateServer

        val sameDay = cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR] &&
                cal1[Calendar.YEAR] == cal2[Calendar.YEAR]

        if (choosenDate.after(currentDateServer)) {
            _studentsEvents.send(Event.FutureDateChoosen)

        } else {
            _studentsEvents.send(Event.CorrectDateChoosen(currentInfo))

        }


    }


    private val _fetchDataStatus = Channel<Resource<List<DocumentSnapshot>>>()
    val fetchDataStatus = _fetchDataStatus.receiveAsFlow()

    sealed class Event {
        data class StudentClicked(val parentSnapshot: DocumentSnapshot) : Event()
        data class StudentEdit(val parentSnapshot: DocumentSnapshot) : Event()
        data class StudentDelete(val parentSnapshot: DocumentSnapshot) : Event()
        data class StudentDeleteConfirmed(val parentSnapshot: DocumentSnapshot) : Event()
        data class StudentSwiped(val parentSnapshot: DocumentSnapshot) : Event()
        data class StudentQuery(val query: String) : Event()
        object AddStudent : Event()

        ////////
        data class ClassSelected(val classGrade: String) : Event()
        data class TabSelected(val tab: Int) : Event()
        data class CorrectDateChoosen(val currentInfo: CurrentInfo) : Event()
        data class FetchCurrentRegister(val string: String) : Event()
        data class FetchData(val currentInfo: CurrentInfo) : Event()
        data class CheckBoxClicked(val snapshot: DocumentSnapshot, val present: Boolean) : Event()
        object DateClicked : Event()
        object FutureDateChoosen : Event()

    }

}