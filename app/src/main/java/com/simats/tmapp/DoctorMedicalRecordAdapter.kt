package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.MedicalRecordResponse

class DoctorMedicalRecordAdapter(
    private val onViewClick: (MedicalRecordResponse) -> Unit,
    private val onDownloadClick: (MedicalRecordResponse) -> Unit
) : ListAdapter<MedicalRecordResponse, DoctorMedicalRecordAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doctor_medical_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivFileIcon: ImageView = view.findViewById(R.id.ivFileIcon)
        private val tvRecordTitle: TextView = view.findViewById(R.id.tvRecordTitle)
        private val tvUploadDate: TextView = view.findViewById(R.id.tvUploadDate)
        private val tvPatientDesc: TextView = view.findViewById(R.id.tvPatientDesc)
        private val btnView: MaterialButton = view.findViewById(R.id.btnView)
        private val btnDownload: MaterialButton = view.findViewById(R.id.btnDownload)

        fun bind(report: MedicalRecordResponse) {
            tvRecordTitle.text = report.description ?: report.recordType ?: report.fileName
            
            val localDate = TimeUtils.convertUtcToLocal(report.createdAt)
            tvUploadDate.text = "Uploaded: $localDate"
            
            tvPatientDesc.text = "Shared by Patient"

            val ext = report.fileName.substringAfterLast('.', "").lowercase()
            val recordTypeLower = report.recordType?.lowercase() ?: ""
            
            when {
                recordTypeLower.contains("lab") || recordTypeLower.contains("report") -> {
                    ivFileIcon.setImageResource(R.drawable.ic_xray_document_purple)
                }
                recordTypeLower.contains("prescription") -> {
                    ivFileIcon.setImageResource(R.drawable.ic_prescription)
                }
                ext in listOf("png", "jpg", "jpeg") -> {
                    ivFileIcon.setImageResource(R.drawable.ic_camera)
                }
                else -> {
                    ivFileIcon.setImageResource(R.drawable.ic_document)
                }
            }

            btnView.setOnClickListener { onViewClick(report) }
            btnDownload.setOnClickListener { onDownloadClick(report) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MedicalRecordResponse>() {
        override fun areItemsTheSame(oldItem: MedicalRecordResponse, newItem: MedicalRecordResponse) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MedicalRecordResponse, newItem: MedicalRecordResponse) = oldItem == newItem
    }
}
