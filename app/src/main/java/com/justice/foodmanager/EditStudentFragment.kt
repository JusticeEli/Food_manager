package com.justice.foodmanager

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.justice.foodmanager.databinding.FragmentAddStudentBinding
import com.justice.foodmanager.databinding.FragmentEditStudentBinding
import com.justice.foodmanager.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class EditStudentFragment : Fragment(R.layout.fragment_edit_student) {

    private val TAG = "EditStudentFragment"

    private lateinit var binding: FragmentEditStudentBinding
    private val viewModel: EditStudentViewModel by viewModels()
    private val navArgs: EditStudentFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditStudentBinding.bind(view)
        Log.d(TAG, "onViewCreated: student:${navArgs.studentData}")
        Log.d(TAG, "onViewCreated: date:${navArgs.date}")

        setValuesForSpinner()
        subScribeToObservers()

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

    private fun subScribeToObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.currentStudent.collect {
                Log.d(TAG, "subScribeToObservers: currentStudent:${it.status.name}")
                when (it.status) {
                    Resource.Status.LOADING -> {

                    }
                    Resource.Status.SUCCESS -> {
                        viewModel.setCurrentSnapshot(it.data!!)
                        setDefaultValuesToEdtTxt(it.data.toObject(StudentData::class.java)!!)
                        setOnClickListeners()

                    }
                    Resource.Status.ERROR -> {
                        showToastInfo("Error:${it.exception?.message}")
                    }
                }

            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.editStudentStatus.collect {
                Log.d(TAG, "subScribeToObservers: editStudentStatus:${it.status.name}")
                when (it.status) {
                    Resource.Status.LOADING -> {
                        showProgress(true)
                    }
                    Resource.Status.SUCCESS -> {
                        showProgress(false)
                    }
                    Resource.Status.ERROR -> {
                        showProgress(false)
                        goBack()
                        Log.d(TAG, "subScribeToObservers: Error:${it.exception?.message}")

                    }
                }
            }
        }
    }

    private fun showToastInfo(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showProgress(b: Boolean) {


    }

    private fun setOnClickListeners() {
        binding.submitBtn.setOnClickListener {
            val student = getStudentObject()
            viewModel.setEvent(EditStudentViewModel.Event.StudentEditSubmitClicked(student))
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

    private fun setDefaultValuesToEdtTxt(studentData: StudentData) {
        binding.apply {
            firstNameEdtTxt.setText(studentData!!.firstName)
            lastNameEdtTxt.setText(studentData!!.lastName)
            setDefaultValueClassGradeSpinner(studentData)
        }


    }

    private fun setDefaultValueClassGradeSpinner(studentData: StudentData) {
        binding.apply {
            when (studentData!!.gradeClass) {
                "1 " -> classGradeSpinner.setSelection(0)
                "2 " -> classGradeSpinner.setSelection(1)
                "3 " -> classGradeSpinner.setSelection(2)
                "4 " -> classGradeSpinner.setSelection(3)
                "5 " -> classGradeSpinner.setSelection(4)
                "6 " -> classGradeSpinner.setSelection(5)
                "7 " -> classGradeSpinner.setSelection(6)
                "8 " -> classGradeSpinner.setSelection(7)
            }
        }

    }

    private fun goBack() {
        findNavController().popBackStack()
    }
}