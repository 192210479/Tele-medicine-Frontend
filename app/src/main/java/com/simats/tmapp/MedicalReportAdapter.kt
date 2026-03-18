package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.MedicalRecordResponse

class MedicalReportAdapter(
    private val currentRole: String,
    private val onItemClick: (MedicalRecordResponse) -> Unit,
    private val onDownloadClick: (MedicalRecordResponse) -> Unit,
    private val onShareClick: (MedicalRecordResponse) -> Unit
) : ListAdapter<MedicalRecordResponse, MedicalReportAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_report, parent, false)
        return ViewHolder(view, currentRole)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = getItem(position)
        holder.bind(report)
        holder.itemView.setOnClickListener { onItemClick(report) }
        holder.ivDownload.setOnClickListener { onDownloadClick(report) }
        holder.ivShare.setOnClickListener { onShareClick(report) }
    }

    class ViewHolder(view: View, private val currentRole: String) : RecyclerView.ViewHolder(view) {
        private val ivFileIcon: ImageView = view.findViewById(R.id.ivFileIcon)
        private val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        private val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        private val tvUploadDate: TextView = view.findViewById(R.id.tvUploadDate)
        val ivDownload: ImageView = view.findViewById(R.id.ivDownload)
        val ivShare: ImageView = view.findViewById(R.id.ivShare)

        fun bind(report: MedicalRecordResponse) {
            tvFileName.text = report.fileName
            tvDescription.text = report.description ?: report.recordType ?: "General Record"
            tvDescription.visibility = View.VISIBLE
            
            val displayDate = if (report.createdAt.contains(" ")) report.createdAt.split(" ")[0] else report.createdAt
            tvUploadDate.text = "Uploaded: $displayDate"

            ivShare.setImageResource(R.drawable.ic_share)
            
            // Visibility logic
            if (currentRole == "doctor") {
                ivShare.visibility = View.GONE
            } else {
                ivShare.visibility = if (report.accessGranted) View.GONE else View.VISIBLE
            }

            val ext = report.fileName.substringAfterLast('.', "").lowercase()
            when (ext) {
                "pdf" -> ivFileIcon.setImageResource(R.drawable.ic_file_document)
                "jpg", "jpeg", "png" -> ivFileIcon.setImageResource(R.drawable.ic_camera)
                "doc", "docx" -> ivFileIcon.setImageResource(R.drawable.ic_document)
                else -> ivFileIcon.setImageResource(R.drawable.ic_document)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MedicalRecordResponse>() {
        override fun areItemsTheSame(oldItem: MedicalRecordResponse, newItem: MedicalRecordResponse) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MedicalRecordResponse, newItem: MedicalRecordResponse) = oldItem == newItem
    }
}
