package com.simats.tmapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.GenericResponse
import com.simats.tmapp.api.MedicalRecordResponse
import com.simats.tmapp.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ShareMedicalRecordsActivity : AppCompatActivity() {

    private lateinit var rvRecords: RecyclerView
    private lateinit var adapter: ShareRecordsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var sessionManager: SessionManager

    private var appointmentId: Int = -1
    private var doctorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_medical_records)

        sessionManager = SessionManager.getInstance(this)
        appointmentId = intent.getIntExtra("appointment_id", -1)
        doctorId = intent.getIntExtra("doctor_id", -1)

        initViews()
        fetchRecords()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        rvRecords = findViewById(R.id.rvRecords)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)

        adapter = ShareRecordsAdapter()
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = adapter

        findViewById<View>(R.id.btnShareWithDoctor).setOnClickListener {
            shareSelectedRecords()
        }
    }

    private fun fetchRecords() {
        progressBar.visibility = View.VISIBLE
        ApiClient.instance.getMedicalRecords(sessionManager.getUserId(), "patient")
            .enqueue(object : Callback<List<MedicalRecordResponse>> {
                override fun onResponse(call: Call<List<MedicalRecordResponse>>, response: Response<List<MedicalRecordResponse>>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val records = response.body() ?: emptyList()
                        if (records.isEmpty()) {
                            llEmptyState.visibility = View.VISIBLE
                            rvRecords.visibility = View.GONE
                        } else {
                            llEmptyState.visibility = View.GONE
                            rvRecords.visibility = View.VISIBLE
                            adapter.submitList(records)
                        }
                    }
                }

                override fun onFailure(call: Call<List<MedicalRecordResponse>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ShareMedicalRecordsActivity, "Failed to load records", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun shareSelectedRecords() {
        val selectedIds = adapter.getSelectedIds()
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one record", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mapOf(
            "user_id" to sessionManager.getUserId(),
            "role" to "patient",
            "appointment_id" to appointmentId,
            "record_ids" to selectedIds
        )

        progressBar.visibility = View.VISIBLE
        ApiClient.instance.shareRecordsToAppointment(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    android.app.AlertDialog.Builder(this@ShareMedicalRecordsActivity)
                        .setTitle("Success")
                        .setMessage("Reports shared with doctor.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .show()
                } else {
                    Toast.makeText(this@ShareMedicalRecordsActivity, "Sharing failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ShareMedicalRecordsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
