package com.justice.foodmanager


import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentSnapshot
import com.justice.foodmanager.databinding.FragmentStudentsBinding
import com.justice.foodmanager.utils.Resource
import com.justice.foodmanager.utils.cleanString
import com.justice.foodmanager.utils.formatDate
import com.justice.foodmanager.utils.onQueryTextChanged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.util.*


@AndroidEntryPoint
class StudentsFragment : Fragment(R.layout.fragment_students) {

    private val TAG = "StudentsFragment"


    lateinit var adapter: StudentAdapter
    lateinit var binding: FragmentStudentsBinding
    lateinit var navController: NavController
    lateinit var searchView: SearchView
    private val viewModel: StudentsViewModel by viewModels()

    companion object {
        const val STUDENT_ARGS = "studentData"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStudentsBinding.bind(view)
        navController = findNavController()
        initProgressBar()
        setHasOptionsMenu(true)
        initRecyclerViewAdapter()
        setOnClickListeners()
        setSwipeListenerForItems()
        subScribeToObservers()
        setValuesForSpinners()

    }

    //checking when to call spinner onItemSelected
    var check = 0
    private fun setValuesForSpinners() {
        val classGrade = requireActivity().resources.getStringArray(R.array.classGradeR)
        val arrayAdapter1: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            classGrade
        )
        binding.classGradeSpinner.setAdapter(arrayAdapter1)

        binding.classGradeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (++check > 1) {
                        val classGrade = binding.classGradeSpinner.selectedItem.toString()
                        Log.d(TAG, "onItemSelected: spinner value changed: ${classGrade}")
                        viewModel.setEvent(StudentsViewModel.Event.ClassSelected(classGrade))

                    }


                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }
    }


    private fun subScribeToObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.getStudents.collect {
                when (it.status) {
                    Resource.Status.LOADING -> {
                        showProgress(true)
                    }
                    Resource.Status.SUCCESS -> {
                        showProgress(false)
                        viewModel.setCurrentListLiveData(it.data?.documents)
                        adapter.submitList(it.data?.documents)
                    }
                    Resource.Status.ERROR -> {
                        showProgress(false)
                    }
                    Resource.Status.EMPTY -> {
                        showProgress(false)

                    }
                }
            }

        }


        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.studentsEvents.collect {
                when (it) {
                    is StudentsViewModel.Event.StudentClicked -> {
                        val student = it.parentSnapshot.toObject(StudentData::class.java)
                        Log.d(TAG, "subScribeToObservers: student:$student")
                        navController.navigate(
                            StudentsFragmentDirections.actionStudentsFragmentToEditStudentFragment(
                                student!!
                            )
                        )
                    }
                    is StudentsViewModel.Event.StudentEdit -> {
                        val student = it.parentSnapshot.toObject(StudentData::class.java)
                    }
                    is StudentsViewModel.Event.StudentDelete -> {
                        deleteStudentFromDatabase(it.parentSnapshot)

                    }
                    is StudentsViewModel.Event.StudentSwiped -> {
                        deleteStudentFromDatabase(it.parentSnapshot)
                    }

                    StudentsViewModel.Event.AddStudent -> {
                        findNavController().navigate(R.id.action_editStudentFragment_to_addStudentFragment)
                    }
                }
            }

        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {

            viewModel.deleteStudentStatus.collect {
                when (it.status) {
                    Resource.Status.SUCCESS -> {
                        showToastInfo("Success deleting student")
                    }
                    Resource.Status.ERROR -> {
                        showToastInfo("Error: ${it.exception?.message}")
                    }
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.studentQueryStatus.collect {
                when (it.status) {
                    Resource.Status.LOADING -> {
                        showProgress(true)

                    }
                    Resource.Status.SUCCESS -> {
                        showProgress(false)
                        adapter.submitList(it.data)
                    }
                    Resource.Status.ERROR -> {
                        showProgress(false)
                        Log.d(TAG, "subScribeToObservers: Error: ${it.exception?.message}")
                    }
                    Resource.Status.EMPTY -> {
                        showProgress(false)
                        Log.d(TAG, "subScribeToObservers: empty query has been passed")
                        adapter.submitList(viewModel.currentListLiveData.value)

                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.getCurrentDate.collect {
                when (it.status) {
                    Resource.Status.LOADING -> {
                    }
                    Resource.Status.SUCCESS -> {
                        receivedCurrentDate(it.data!!)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.studentsEvents.collect {
                when (it) {
                    is StudentsViewModel.Event.DateClicked -> {
                        dateClicked()
                    }
                    is StudentsViewModel.Event.CorrectDateChoosen -> {
                        correctDateChoosen(it.currentInfo)
                    }
                    is StudentsViewModel.Event.FutureDateChoosen -> {
                        futureDateChoosen()
                    }
                    is StudentsViewModel.Event.ClassSelected -> {
                        classGradeSelected(it.classGrade)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.fetchDataStatus.collect {
                Log.d(TAG, "subScribeToObservers:fetchDataStatus ${it.status.name}")
                when (it.status) {
                    Resource.Status.LOADING -> {

                    }
                    Resource.Status.SUCCESS -> {
                        adapter.submitList(it.data)

                    }
                    Resource.Status.ERROR -> {

                    }
                }
            }
        }
    }


    private fun dateClicked() {
        val date = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

            val calenderChoosen = Calendar.getInstance()
            calenderChoosen[Calendar.YEAR] = year
            calenderChoosen[Calendar.MONTH] = monthOfYear
            calenderChoosen[Calendar.DAY_OF_MONTH] = dayOfMonth

            val currentClassGrade = binding.classGradeSpinner.selectedItem.toString()
            val currentInfo = CurrentInfo(
                calenderChoosen.time.formatDate.cleanString,
                currentClassGrade,
                calenderChoosen.time
            )

            Log.d(TAG, "dateClicked: currentInfo:$currentInfo")
            viewModel.setEvent(StudentsViewModel.Event.CorrectDateChoosen(currentInfo))
        }


        val myCalendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(), date, myCalendar[Calendar.YEAR], myCalendar[Calendar.MONTH],
            myCalendar[Calendar.DAY_OF_MONTH]
        ).show()
    }

    private fun receivedCurrentDate(date: Date) {
        val dateFormatted = date.formatDate
        Log.d(TAG, "receivedCurrentDate: currentdate:$dateFormatted")
        binding.currentDateTxtView.text = dateFormatted
        val currentInfo = CurrentInfo()
        currentInfo.currentDateString = dateFormatted.cleanString
        viewModel.setEvent(StudentsViewModel.Event.FetchData(currentInfo))
    }

    private fun correctDateChoosen(currentInfo: CurrentInfo) {
        updateLabel(currentInfo.dateChoosen!!)
        viewModel.setEvent(StudentsViewModel.Event.FetchData(currentInfo))

    }

    private fun classGradeSelected(classGrade: String) {
        val currentDateString = binding.currentDateTxtView.text.toString().cleanString
        val currentInfo = CurrentInfo(currentDateString, classGrade, null)
        viewModel.setEvent(StudentsViewModel.Event.FetchData(currentInfo))

    }

    private fun futureDateChoosen() {
        showToastInfo("Please Dont choose future dates")
    }

    private fun showToastInfo(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateLabel(date: Date) {
        binding.currentDateTxtView.text = date.formatDate
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_blog, menu)

        val searchItem = menu.findItem(R.id.searchItem)
        searchView = searchItem.actionView as SearchView


        searchView.onQueryTextChanged { query ->
            Log.d(TAG, "onCreateOptionsMenu: query:$query")
            viewModel.setEvent(StudentsViewModel.Event.StudentQuery(query))
        }



        super.onCreateOptionsMenu(menu, inflater)
    }


    private fun initRecyclerViewAdapter() {
        adapter =
            StudentAdapter(
                { snapshot, present -> onCheckBoxClicked(snapshot, present) },
                { onEditClicked(it) },
                { onStudentClicked(it) },
                { onStudentDelete(it) })
        binding.recyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        binding.recyclerView.setAdapter(adapter)
    }

    private fun onCheckBoxClicked(snapshot: DocumentSnapshot, present: Boolean) {
        viewModel.setEvent(StudentsViewModel.Event.CheckBoxClicked(snapshot, present))
    }

    private fun onStudentDelete(it: DocumentSnapshot) {
        viewModel.setEvent(StudentsViewModel.Event.StudentDelete(it))
    }

    private fun onStudentClicked(it: DocumentSnapshot) {
        Log.d(TAG, "onStudentClicked: ")
        viewModel.setEvent(StudentsViewModel.Event.StudentClicked(it))
    }

    private fun onEditClicked(it: DocumentSnapshot) {
        viewModel.setEvent(StudentsViewModel.Event.StudentEdit(it))

    }

    private fun setOnClickListeners() {
        binding.addStudentBtn.setOnClickListener {
            viewModel.setEvent(StudentsViewModel.Event.AddStudent)
        }
        binding.dateBtn.setOnClickListener {
            viewModel.setEvent(StudentsViewModel.Event.DateClicked)
        }
    }

    fun deleteStudentFromDatabase(snapshot: DocumentSnapshot) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("delete")
            .setMessage("Are you sure you want to delete ")
            .setNegativeButton("no") { dialog, which ->
                val position = adapter.currentList.indexOf(snapshot)
                adapter.notifyItemChanged(position)
            }.setPositiveButton("yes") { dialog, which ->
                viewModel.setEvent(StudentsViewModel.Event.StudentDeleteConfirmed(snapshot))
            }.show()
    }

    private fun setSwipeListenerForItems() {
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val snapshot = adapter.currentList.get(viewHolder.adapterPosition)
                viewModel.setEvent(StudentsViewModel.Event.StudentSwiped(snapshot))
            }
        }).attachToRecyclerView(binding.recyclerView)
    }


    /////////////////////PROGRESS_BAR////////////////////////////
    lateinit var dialog: AlertDialog

    fun showProgress(show: Boolean) {

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