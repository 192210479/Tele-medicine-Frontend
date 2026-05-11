package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.MedicalRecordResponse

class ShareRecordsAdapter : ListAdapter<MedicalRecordResponse, ShareRecordsAdapter.ViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<Int>()

    fun getSelectedIds(): List<Int> = selectedIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_share_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        holder.bind(record)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        private val tvUploadDate: TextView = view.findViewById(R.id.tvUploadDate)
        private val ivFileIcon: ImageView = view.findViewById(R.id.ivFileIcon)
        private val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)

        fun bind(record: MedicalRecordResponse) {
            tvFileName.text = record.fileName
            val displayDate = if (record.createdAt.contains(" ")) record.createdAt.split(" ")[0] else record.createdAt
            tvUploadDate.text = "Uploaded: $displayDate"

            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = selectedIds.contains(record.id)
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIds.add(record.id)
                else selectedIds.remove(record.id)
            }

            itemView.setOnClickListener {
                cbSelect.isChecked = !cbSelect.isChecked
            }

            val ext = record.fileName.substringAfterLast('.', "").lowercase()
            when (ext) {
                "pdf" -> ivFileIcon.setImageResource(R.drawable.ic_file_document)
                "jpg", "jpeg", "png" -> ivFileIcon.setImageResource(R.drawable.ic_camera)
                else -> ivFileIcon.setImageResource(R.drawable.ic_file_document)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MedicalRecordResponse>() {
        override fun areItemsTheSame(oldItem: MedicalRecordResponse, newItem: MedicalRecordResponse) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MedicalRecordResponse, newItem: MedicalRecordResponse) = oldItem == newItem
    }
}
