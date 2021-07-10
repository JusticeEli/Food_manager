package com.justice.foodmanager.ui.splash

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.justice.foodmanager.data.StudentsRepository
import com.justice.foodmanager.utils.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class SplashScreenViewModel @ViewModelInject constructor(private val repository: StudentsRepository) :
    ViewModel() {

    private val TAG = "SplashScreenViewModel"

    private val _userSignUpProcessStatus = Channel<Resource<FirebaseUser>>()
    val userSignUpProcessStatus = _userSignUpProcessStatus.receiveAsFlow()
    val checkIsUserLoggedInStatus = repository.checkIsUserLoggedIn()
    private val _userSetupStatus = Channel<Resource<DocumentSnapshot>>()
    val userSetupStatus = _userSetupStatus.receiveAsFlow()
    private val _splashScreenEvents = Channel<Event>()
    val splashScreenEvents = _splashScreenEvents.receiveAsFlow()


    fun setEvent(event: Event) {
        viewModelScope.launch {
            when (event) {

                is Event.GoToHomeScreen -> {
                    _splashScreenEvents.send(Event.GoToHomeScreen)
                }


            }
        }
    }



    sealed class Event {
          object GoToHomeScreen : Event()
    }
}