package com.justice.foodmanager

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.justice.foodmanager.databinding.ItemRegisterBinding

class StudentAdapter(
    private val checkBoxClicked: (DocumentSnapshot, Boolean) -> Unit,
    private val onEditClicked: (DocumentSnapshot) -> Unit,
    private val onStudentClicked: (DocumentSnapshot) -> Unit,
    private val onStudentDelete: (DocumentSnapshot) -> Unit
) : ListAdapter<DocumentSnapshot, StudentAdapter.ViewHolder>(DIFF_UTIL) {
    private val TAG = "ParentFilterAdapter"

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<DocumentSnapshot>() {
            override fun areItemsTheSame(
                oldItem: DocumentSnapshot,
                newItem: DocumentSnapshot
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: DocumentSnapshot,
                newItem: DocumentSnapshot
            ): Boolean {
                val old = oldItem.toObject(StudentData::class.java)
                val new = newItem.toObject(StudentData::class.java)
                return old!!.equals(new)
            }

        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_register, parent, false)
        val binding: ItemRegisterBinding = ItemRegisterBinding.bind(view)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: StudentAdapter.ViewHolder, position: Int) {
        val model = getItem(position).toObject(StudentData::class.java)!!
        Log.d(TAG, "onBindViewHolder: model:$model")
        holder.bind(model)
    }


    inner class ViewHolder(val binding: ItemRegisterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: StudentData) = with(binding.root) {
            binding.studentNameTxtView.text = "${model.firstName} ${model.lastName} "
            binding.studentClassTxtView.text = "${model.gradeClass}"

            binding.checkbox.setOnCheckedChangeListener(null);
            binding.checkbox.isChecked = model.present
            setOnClickListeners(this@ViewHolder, this@ViewHolder.adapterPosition)

        }

        private fun setOnClickListeners(holder: ViewHolder, position: Int) {

            var currentSnapshot: DocumentSnapshot
            try {
                currentSnapshot = getItem(position)
            } catch (e: IndexOutOfBoundsException) {
                currentSnapshot = getItem(position - 1)

            }

            holder.itemView.setOnClickListener {
                onStudentClicked(currentSnapshot)
            }
            holder.binding.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                Log.d(TAG, "setOnClickListeners: isChecked:$isChecked")
                checkBoxClicked(currentSnapshot, isChecked)
            }
        }
    }

}