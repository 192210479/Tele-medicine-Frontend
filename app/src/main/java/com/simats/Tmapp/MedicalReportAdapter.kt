package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.MedicalRecordResponse

class MedicalReportAdapter(
    private val currentRole: String,
    private val isSharingMode: Boolean = false,
    private val onItemClick: (MedicalRecordResponse) -> Unit,
    private val onDownloadClick: (MedicalRecordResponse) -> Unit,
    private val onShareClick: (MedicalRecordResponse) -> Unit
) : ListAdapter<MedicalRecordResponse, MedicalReportAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_report, parent, false)
        return ViewHolder(view, currentRole, isSharingMode)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = getItem(position)
        holder.bind(report)
        holder.itemView.setOnClickListener { onItemClick(report) }
        holder.ivDownload.setOnClickListener { onDownloadClick(report) }
        holder.ivShare.setOnClickListener { onShareClick(report) }
    }

    class ViewHolder(view: View, private val currentRole: String, private val isSharingMode: Boolean) : RecyclerView.ViewHolder(view) {
        private val ivFileIcon: ImageView = view.findViewById(R.id.ivFileIcon)
        private val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        private val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        private val tvUploadDate: TextView = view.findViewById(R.id.tvUploadDate)
        val ivDownload: ImageView = view.findViewById(R.id.ivDownload)
        val ivShare: ImageView = view.findViewById(R.id.ivShare)

        fun bind(report: MedicalRecordResponse) {
            tvFileName.text = report.fileName
            tvDescription.text = report.recordType ?: report.description ?: "Shared Medical Record"
            tvDescription.visibility = if (tvDescription.text.isNullOrBlank()) View.GONE else View.VISIBLE

            tvUploadDate.text = if (!report.createdAt.isNullOrBlank()) {
                "Shared on: ${TimeUtils.convertUtcToLocal(report.createdAt)}"
            } else {
                "Shared on: Not available"
            }

            ivShare.setImageResource(R.drawable.ic_share)
            
            // Visibility logic
            if (currentRole == "doctor") {
                ivShare.visibility = View.GONE
            } else {
                // For patient: only show share if we are in an appointment context (sharing mode)
                ivShare.visibility = if (isSharingMode) View.VISIBLE else View.GONE
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
