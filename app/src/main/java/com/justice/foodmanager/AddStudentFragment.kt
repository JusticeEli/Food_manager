package com.justice.foodmanager

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.justice.foodmanager.databinding.FragmentAddStudentBinding
import com.justice.foodmanager.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class AddStudentFragment : Fragment(R.layout.fragment_add_student) {

    private val TAG = "AddStudentFragment"

    private lateinit var binding: FragmentAddStudentBinding
    private val viewModel: AddStudentViewModel by viewModels()
    private val navArgs: AddStudentFragmentArgs by navArgs()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddStudentBinding.bind(view)
        Log.d(TAG, "onViewCreated: date:${navArgs.date}")
        setOnClickListeners()
        setValuesForSpinner()
        subScribeToObservers()

    }


    private fun subScribeToObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.addStudentStatus.collect {
                Log.d(TAG, "subScribeToObservers: addStudentStatus:${it.status.name}")
                when (it.status) {
                    Resource.Status.LOADING -> {
                        showProgress(true)
                    }
                    Resource.Status.SUCCESS -> {
                        showProgress(false)
                        resetEdtTxt()
                    }
                    Resource.Status.ERROR -> {
                        showProgress(false)
                        Log.d(TAG, "subScribeToObservers: Error:${it.exception?.message}")

                    }
                }
            }
        }
    }

    private fun showProgress(visible: Boolean) {

    }

    private fun setOnClickListeners() {
        binding.submitBtn.setOnClickListener {

            val student = getStudentObject()
            viewModel.setEvent(AddStudentViewModel.Event.StudentAddSubmitClicked(student,navArgs.date))

        }
    }

    private fun getStudentObject(): StudentData {
        binding.apply {
            val firstName = firstNameEdtTxt.text.toString()
            val lastName = lastNameEdtTxt.text.toString()
            val classGrade = (classGradeSpinner.getSelectedItem().toString())
            val student = StudentData(firstName, lastName, classGrade)

            return student
        }

    }

    private fun resetEdtTxt() {
        binding.apply {
            firstNameEdtTxt.setText("")
            lastNameEdtTxt.setText("")
        }
    }

    private fun setValuesForSpinner() {
        val classGrade = requireActivity().resources.getStringArray(R.array.classGrade)
        val arrayAdapter1: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            classGrade
        )
        binding.classGradeSpinner.setAdapter(arrayAdapter1)


    }
}