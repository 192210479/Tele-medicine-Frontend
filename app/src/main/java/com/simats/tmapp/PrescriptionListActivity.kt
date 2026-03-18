package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.ApiClient
import com.simats.tmapp.api.PrescriptionHistoryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrescriptionListActivity : AppCompatActivity() {

    private lateinit var rvPrescriptions: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var adapter: PrescriptionAdapter
    private var prescriptionList = mutableListOf<PrescriptionHistoryResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescription_list)

        initViews()
        setupRecyclerView()
        setupListeners()
        fetchPrescriptions()
    }

    private fun initViews() {
        rvPrescriptions = findViewById(R.id.rvPrescriptions)
        llEmptyState = findViewById(R.id.llEmptyState)
        etSearch = findViewById(R.id.etSearch)
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        rvPrescriptions.layoutManager = LinearLayoutManager(this)
        adapter = PrescriptionAdapter(prescriptionList) { prescription ->
            val intent = Intent(this, ViewPrescriptionActivity::class.java)
            intent.putExtra("appointment_id", prescription.appointmentId)
            intent.putExtra("doctor_id", prescription.doctorId)
            intent.putExtra("doctor_name", prescription.doctor_name)
            intent.putExtra("doctor_specialization", prescription.specialization)
            startActivity(intent)
        }
        rvPrescriptions.adapter = adapter
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPrescriptions(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<ImageView>(R.id.ivFilter).setOnClickListener {
            // Implementation of filter dialog could go here
            Toast.makeText(this, "Filter feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchPrescriptions() {
        val sessionManager = SessionManager.getInstance(this)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        // Use the requested API endpoint
        val call = ApiClient.instance.getPatientPrescriptions(userId, role)
        call.enqueue(object : Callback<List<PrescriptionHistoryResponse>> {
            override fun onResponse(
                call: Call<List<PrescriptionHistoryResponse>>,
                response: Response<List<PrescriptionHistoryResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    prescriptionList.clear()
                    prescriptionList.addAll(response.body()!!)
                    updateUI()
                } else {
                    showErrorDialog()
                }
            }

            override fun onFailure(call: Call<List<PrescriptionHistoryResponse>>, t: Throwable) {
                showErrorDialog()
            }
        })
    }

    private fun showErrorDialog() {
        showEmptyState(true)
        android.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Unable to load prescription. Please try again.")
            .setPositiveButton("Retry") { _, _ -> fetchPrescriptions() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun filterPrescriptions(query: String) {
        val filteredList = prescriptionList.filter {
            (it.doctor_name?.contains(query, ignoreCase = true) == true) || 
            (it.diagnosis?.contains(query, ignoreCase = true) == true) ||
            (it.date?.contains(query, ignoreCase = true) == true)
        }
        adapter.updateList(filteredList)
        showEmptyState(filteredList.isEmpty())
    }

    private fun updateUI() {
        adapter.notifyDataSetChanged()
        showEmptyState(prescriptionList.isEmpty())
    }

    private fun showEmptyState(isEmpty: Boolean) {
        llEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvPrescriptions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
