package com.justice.foodmanager

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.justice.foodmanager.databinding.FragmentEditStudentBinding
import com.justice.foodmanager.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.util.*

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
        initProgressBar()
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
                        showProgress(true)
                    }
                    Resource.Status.SUCCESS -> {
                        viewModel.setCurrentSnapshot(it.data!!)
                        setDefaultValuesToEdtTxt(it.data!!.toObject(StudentData::class.java)!!)
                        setOnClickListeners()
                        showProgress(false)
                    }
                    Resource.Status.ERROR -> {
                        showProgress(false)
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
                        goBack()
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


    private fun setOnClickListeners() {
        binding.submitBtn.setOnClickListener {
            val student = getStudentObject()
            viewModel.setEvent(
                EditStudentViewModel.Event.StudentEditSubmitClicked(
                    student,
                    navArgs.date
                )
            )
        }
    }

    private fun getStudentObject(): StudentData {
        binding.apply {
            val firstName = firstNameEdtTxt.text.toString()
            val lastName = lastNameEdtTxt.text.toString()
            val classGrade = (classGradeSpinner.getSelectedItem().toString())
            val student = navArgs.studentData.copy(
                firstName = firstName,
                lastName = lastName,
                gradeClass = classGrade
            )
            return student
        }

    }

    private fun resetEdtTxt() {
        binding.apply {
            firstNameEdtTxt.setText("")
            lastNameEdtTxt.setText("")
        }
    }

    private fun setDefaultValuesToEdtTxt(studentData: StudentData?) {
        Log.d(TAG, "setDefaultValuesToEdtTxt: studentData:$studentData")
        binding.apply {
            firstNameEdtTxt.setText(studentData!!.firstName)
            lastNameEdtTxt.setText(studentData!!.lastName)
            setDefaultValueClassGradeSpinner(studentData)
        }
        Log.d(TAG, "setDefaultValuesToEdtTxt: end")

    }

    private fun setDefaultValueClassGradeSpinner(studentData: StudentData) {
        Log.d(TAG, "setDefaultValueClassGradeSpinner: ")
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

    /////////////////////PROGRESS_BAR////////////////////////////
    lateinit var dialog: AlertDialog

    private fun showProgress(show: Boolean) {

        if (show) {
            dialog.show()

        } else {
            dialog.dismiss()

        }

    }

    private fun initProgressBar() {

        dialog = setProgressDialog(requireContext(), "Loading..")
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
    }

    fun setProgressDialog(context: Context, message: String): AlertDialog {
        val llPadding = 30
        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam

        val progressBar = ProgressBar(context)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam

        llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        val tvText = TextView(context)
        tvText.text = message
        tvText.setTextColor(Color.parseColor("#000000"))
        tvText.textSize = 20.toFloat()
        tvText.layoutParams = llParam

        ll.addView(progressBar)
        ll.addView(tvText)

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setView(ll)

        val dialog = builder.create()
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = layoutParams
        }
        return dialog
    }

    //end progressbar
}