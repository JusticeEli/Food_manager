package com.justice.foodmanager.ui.add_student

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.justice.foodmanager.R
import com.justice.foodmanager.data.StudentData
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
        initProgressBar()
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



    private fun setOnClickListeners() {
        binding.submitBtn.setOnClickListener {

            val student = getStudentObject()
            Log.d(TAG, "setOnClickListeners: student:$student")
            viewModel.setEvent(
                AddStudentViewModel.Event.StudentAddSubmitClicked(
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
            val student = StudentData("",firstName, lastName, classGrade)

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
                      LinearLayout.LayoutParams.WRAP_CONTENT)
              llParam.gravity = Gravity.CENTER
              ll.layoutParams = llParam

              val progressBar = ProgressBar(context)
              progressBar.isIndeterminate = true
              progressBar.setPadding(0, 0, llPadding, 0)
              progressBar.layoutParams = llParam

              llParam = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                      ViewGroup.LayoutParams.WRAP_CONTENT)
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