package com.simats.Tmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.MedicalRecordResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConsultationUploadBottomSheet(
    private val appointmentId: Int,
    private val onUploadClick: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var rvDocuments: RecyclerView
    private lateinit var adapter: MedicalReportAdapter
    private val records = mutableListOf<MedicalRecordResponse>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_consultation_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.ivClose).setOnClickListener {
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnUploadDocuments).setOnClickListener {
            onUploadClick()
            dismiss()
        }

        rvDocuments = view.findViewById(R.id.rvDocuments)
        val sessionManager = SessionManager.getInstance(requireContext())
        adapter = MedicalReportAdapter(
            currentRole = sessionManager.getUserRole().lowercase(),
            isSharingMode = false,
            onItemClick = { record ->
                openDocument(record)
            },
            onDownloadClick = { record ->
                openDocument(record)
            },
            onShareClick = { _ -> }
        )
        rvDocuments.layoutManager = LinearLayoutManager(requireContext())
        rvDocuments.adapter = adapter

        fetchDocuments()
    }

    private fun fetchDocuments() {
        // using userId = -1 and role = "doctor" fetches all records for the appointment without restriction
        ApiClient.instance.getMedicalRecords(
            userId = -1,
            role = "doctor",
            appointmentId = appointmentId
        ).enqueue(object : Callback<List<MedicalRecordResponse>> {
            override fun onResponse(
                call: Call<List<MedicalRecordResponse>>,
                response: Response<List<MedicalRecordResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    records.clear()
                    records.addAll(response.body()!!)
                    adapter.submitList(records.toList())
                }
            }

            override fun onFailure(call: Call<List<MedicalRecordResponse>>, t: Throwable) {
                Toast.makeText(requireContext(), "Failed to load documents", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openDocument(record: MedicalRecordResponse) {
        if (record.filePath.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "File path not available", Toast.LENGTH_SHORT).show()
            return
        }
        val url = if (record.filePath.startsWith("http")) {
            record.filePath
        } else {
            ApiClient.BASE_URL.removeSuffix("/") + record.filePath
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No application found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
}
