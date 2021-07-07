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
import androidx.core.view.isVisible
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

        setHasOptionsMenu(true)
        initRecyclerViewAdapter()
        setOnClickListeners()
        setSwipeListenerForItems()
        subScribeToObservers()
        setValuesForSpinners()
        initFetching()
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
                        classGradeSelected(classGrade)

                    }


                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.d(TAG, "onNothingSelected: ")
                }
            }
    }


    private fun subScribeToObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.studentsEvents.collect {
                when (it) {
                    is StudentsViewModel.Event.StudentClicked -> {
                        val student = it.parentSnapshot.toObject(StudentData::class.java)
                        Log.d(TAG, "subScribeToObservers: student:$student")
                        navController.navigate(
                            StudentsFragmentDirections.actionStudentsFragmentToEditStudentFragment(
                                student!!,
                                binding.currentDateTxtView.text.toString().cleanString
                            )
                        )
                    }
                    is StudentsViewModel.Event.StudentDelete -> {
                        deleteStudentFromDatabase(it.parentSnapshot)

                    }
                    is StudentsViewModel.Event.StudentSwiped -> {
                        deleteStudentFromDatabase(it.parentSnapshot)
                    }

                    is StudentsViewModel.Event.CorrectDateChoosen -> {
                        correctDateChoosen(it.currentInfo)
                    }
                    is StudentsViewModel.Event.FutureDateChoosen -> {
                        futureDateChoosen()
                    }

                }
            }

        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {

            viewModel.deleteStudentStatus.collect {
                when (it.status) {
                    Resource.Status.LOADING->{
                        showProgess(true)

                    }
                    Resource.Status.SUCCESS -> {
                        showToastInfo("Success deleting student")
                        showProgess(false)
                    }
                    Resource.Status.ERROR -> {
                        showProgess(false)

                        showToastInfo("Error: ${it.exception?.message}")
                    }
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.studentQueryStatus.collect {
                Log.d(TAG, "subScribeToObservers: studentQueryStatus:${it.status.name}")
                when (it.status) {
                    Resource.Status.LOADING -> {
                        showProgess(true)

                    }
                    Resource.Status.SUCCESS -> {
                        showProgess(false)
                        adapter.submitList(it.data)
                    }
                    Resource.Status.ERROR -> {
                        showProgess(false)
                        Log.d(TAG, "subScribeToObservers: Error: ${it.exception?.message}")
                    }
                    Resource.Status.EMPTY -> {
                        showProgess(false)
                        Log.d(TAG, "subScribeToObservers: empty query has been passed")
                        adapter.submitList(viewModel.currentListLiveData.value)

                    }
                }
            }
        }
    }

    private fun showProgess(visible: Boolean) {
        binding.progressBar.isVisible = visible
    }

    private fun dateClicked() {
        Log.d(TAG, "dateClicked: ")
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

    private fun initFetching() {
        val date = Calendar.getInstance().time
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

            val classGrade = binding.classGradeSpinner.selectedItem.toString()
            viewModel.setEvent(StudentsViewModel.Event.StudentQuery(query, classGrade))
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
        deleteStudentFromDatabase(it)
    }

    private fun onStudentClicked(it: DocumentSnapshot) {
        Log.d(TAG, "onStudentClicked: ")
        val student = it.toObject(StudentData::class.java)!!
        goToEditScreen(student)
    }

    private fun goToEditScreen(student: StudentData) {
        val date = binding.currentDateTxtView.text.toString().cleanString
        findNavController().navigate(
            StudentsFragmentDirections.actionStudentsFragmentToEditStudentFragment(
                student,
                date
            )
        )
    }

    private fun onEditClicked(it: DocumentSnapshot) {
        viewModel.setEvent(StudentsViewModel.Event.StudentEdit(it))

    }

    private fun setOnClickListeners() {
        binding.addStudentBtn.setOnClickListener {
            val date = binding.currentDateTxtView.text.toString().cleanString
            findNavController().navigate(
                StudentsFragmentDirections.actionStudentsFragmentToAddStudentFragment(
                    date
                )
            )
        }
        binding.dateBtn.setOnClickListener {
            dateClicked()
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
                deleteStudentFromDatabase(snapshot)
            }
        }).attachToRecyclerView(binding.recyclerView)
    }




}