package com.justice.foodmanager.ui.student


import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.justice.foodmanager.data.CurrentInfo
import com.justice.foodmanager.data.StudentData
import com.justice.foodmanager.data.StudentsRepository
import com.justice.foodmanager.utils.FirebaseUtil.getCurrentDate
import com.justice.foodmanager.utils.Resource
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
    private val _getCurrentDateStatus = Channel<Resource<Date>>()
    val getCurrentDateStatus = _getCurrentDateStatus.receiveAsFlow()

    private val _studentsEvents = Channel<Event>()
    val studentsEvents = _studentsEvents.receiveAsFlow()

    private val currentInfo=CurrentInfo()
    fun setEvent(event: Event) {
        viewModelScope.launch {
            when (event) {

                is Event.StudentDeleteConfirmed -> {
                    deleteStudent(event.parentSnapshot)
                }
                is Event.GetCurrentDate -> {
                    getCurrentDate()
                }
                is Event.StudentQuery -> {
                    startQuery(event.query, event.classGrade)
                }
                is Event.CorrectDateChoosen -> {
                    checkIfWeHaveChoosenCorrectDate(event.currentInfo)
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


    private val _studentQueryStatus = Channel<Resource<List<DocumentSnapshot>>>()
    val studentQueryStatus = _studentQueryStatus.receiveAsFlow()

    private suspend fun startQuery(query: String, classGrade: String) {
        Log.d(TAG, "startQuery: query:$query ::classGrade:$classGrade")
        _studentQueryStatus.send(Resource.loading("querying..."))
        if (classGrade.equals("all")) {

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
        } else {
            val filteredList = mutableListOf<DocumentSnapshot>()
            getQueryGradeClassList(classGrade).forEach {
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

    private suspend fun startQueryGradeClass(classGrade: String) {
        Log.d(TAG, "startQueryGradeClass: classGrade:$classGrade")
        if (classGrade.equals("all")) {
            _studentQueryStatus.send(Resource.success(currentListLiveData.value!!))
        } else {
            val filteredList = mutableListOf<DocumentSnapshot>()
            currentListLiveData.value?.forEach {
                val student = it.toObject(StudentData::class.java)!!
                if (student.gradeClass.equals(classGrade)) {
                    filteredList.add(it)
                }
            }
            Log.d(TAG, "startQueryGradeClass: filteredList size:${filteredList.size}")
            _studentQueryStatus.send(Resource.success(filteredList))

        }
    }

    private suspend fun getQueryGradeClassList(classGrade: String): List<DocumentSnapshot> {
        Log.d(TAG, "startQueryGradeClass: classGrade:$classGrade")
        val filteredList = mutableListOf<DocumentSnapshot>()
        currentListLiveData.value?.forEach {
            val student = it.toObject(StudentData::class.java)!!
            if (student.gradeClass.equals(classGrade)) {
                filteredList.add(it)
            }
        }
        Log.d(TAG, "startQueryGradeClass: filteredList size:${filteredList.size}")

        return filteredList

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
        Log.d(TAG, "startFetchingData: currentInfo:$currentInfo")
        _studentQueryStatus.send(Resource.loading("Fetching data..."))
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

    private suspend fun documentExists(currentInfo: CurrentInfo, snapshot: DocumentSnapshot) {
        Log.d(TAG, "documentExists: ")
        repository.documentExist(currentInfo).collect {
            Log.d(TAG, "documentExists: status:${it.status.name}")
            when (it.status) {
                Resource.Status.LOADING -> {

                }
                Resource.Status.SUCCESS -> {
                    // successfully fetched list of students
                    successFilterResult(it, currentInfo)

                }
                Resource.Status.ERROR -> {
                    _fetchDataStatus.send(it)
                }
            }

        }
    }

    private suspend fun documentDoesNotExist(currentInfo: CurrentInfo) {
        Log.d(TAG, "documentDoesNotExist: ")
        repository.documentDoesNotExist(currentInfo).collect {
            when (it.status) {
                Resource.Status.LOADING -> {

                }
                Resource.Status.SUCCESS -> {
                    // successfully fetched list of students
                    successFilterResult(it, currentInfo)

                }
                Resource.Status.ERROR -> {
                    _fetchDataStatus.send(it)
                }
            }
        }
    }


    private suspend fun successFilterResult(
        it: Resource<List<DocumentSnapshot>>,
        currentInfo: CurrentInfo
    ) {
        Log.d(TAG, "successFilterResult: size:${it.data!!.size}")
        currentListLiveData.value = it.data
        startQueryGradeClass(currentInfo.currentClassGrade)
        Log.d(TAG, "successFilterResult: end")
    }

    private suspend fun checkIfWeHaveChoosenCorrectDate(currentInfo: CurrentInfo) {
        //check if we have choosen a future date and reject it if its future date
///checks if we are on same day
        Log.d(TAG, "checkIfWeHaveChoosenCorrectDate: currentInfo:$currentInfo")
        val choosenDate = currentInfo.dateChoosen!!
        val currentDateServer = repository.getCurrentDate2()
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = choosenDate
        cal2.time = currentDateServer

        val sameDay = cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR] &&
                cal1[Calendar.YEAR] == cal2[Calendar.YEAR]
        if (sameDay) {
            _studentsEvents.send(Event.CorrectDateChoosen(currentInfo))

        } else if (choosenDate.after(currentDateServer)) {
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
        data class StudentQuery(val query: String, val classGrade: String) : Event()
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
        object GetCurrentDate : Event()

    }

}