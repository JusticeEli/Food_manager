package com.justice.foodmanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.justice.foodmanager.databinding.FragmentSplashScreenBinding
import com.justice.foodmanager.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.util.*

@AndroidEntryPoint
class SplashScreenFragment : Fragment(R.layout.fragment_splash_screen) {

    private val TAG = "SplashScreenFragment"

    private lateinit var binding: FragmentSplashScreenBinding
    private val viewModel: SplashScreenViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSplashScreenBinding.bind(view)
        subScribeToObservers()


    }

    private fun subScribeToObservers() {
        lifecycleScope.launchWhenResumed {
            viewModel.checkIsUserLoggedInStatus.collect {
                Log.d(TAG, "subScribeToObservers: checkIsUserLoggedInStatus:${it.status.name}")
                when (it.status) {
                    Resource.Status.LOADING -> {
                        showProgress(true)
                    }
                    Resource.Status.SUCCESS -> {
                        showProgress(false)
                        goToHomeScreen()
                    }
                    Resource.Status.ERROR -> {
                        showProgress(false)
                        startLogginProcess()
                    }
                }

            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.splashScreenEvents.collect {
                when (it) {

                    is SplashScreenViewModel.Event.GoToHomeScreen -> {
                        goToHomeScreen()
                    }


                }
            }
        }
    }


    private fun goToHomeScreen() {
        findNavController().navigate(R.id.action_splashScreenFragment_to_studentsFragment)
    }

    private val RC_SIGN_IN = 9
    private fun startLogginProcess() {
        Log.d(TAG, "startLogginProcess: ")
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()

                .setAvailableProviders(
                    Arrays.asList(
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.PhoneBuilder().build(),
                        AuthUI.IdpConfig.AnonymousBuilder().build()
                    )
                )
                .build(),
            RC_SIGN_IN
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {
                //is sign in is success we want to recreate the activity
                Log.d(TAG, "onActivityResult: success sign in")
                goToHomeScreen()

            } else {
                // Sign in failed
                Log.d(TAG, "onActivityResult: sign in failed")
                if (response == null) {
                    // User pressed back button
                    showToast("sign in cancelled")
                    return
                }
                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    showToast("not internet connection")
                    return
                }
                showToast("unknown error")
                Log.e(TAG, "Sign-in error: ", response.error)
            }
        }
    }

    private fun showProgress(visible: Boolean) {
        binding.progressBar.isVisible = visible
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


}

