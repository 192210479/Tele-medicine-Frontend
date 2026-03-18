package com.simats.tmapp

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.simats.tmapp.api.ApiClient
import com.simats.tmapp.api.MedicalRecordResponse
import io.socket.client.Socket
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream

class DoctorPatientRecordsActivity : AppCompatActivity() {

    private lateinit var rvRecords: RecyclerView
    private lateinit var adapter: DoctorMedicalRecordAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var sessionManager: SessionManager

    private var patientId: Int = -1
    private var patientName: String = ""
    private var lastAppointment: String? = null
    private var allRecords: List<MedicalRecordResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_patient_records)

        sessionManager = SessionManager.getInstance(this)
        patientId = intent.getIntExtra("patient_id", -1)
        patientName = intent.getStringExtra("patient_name") ?: "Patient"
        lastAppointment = intent.getStringExtra("last_appointment")

        if (patientId == -1) {
            Toast.makeText(this, "Invalid Patient ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupSocketListener()
        fetchRecords()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvPatientName).text = patientName
        findViewById<TextView>(R.id.tvHeaderTitle).text = "$patientName's Records"
        findViewById<TextView>(R.id.tvApptInfo).text = "Last Appointment: ${TimeUtils.formatSimpleDate(lastAppointment)}"

        val ivAvatar = findViewById<ImageView>(R.id.ivPatientAvatar)
        val avatarUrl = "${ApiClient.BASE_URL}api/profile/image/$patientId?role=patient"
        Glide.with(this)
            .load(avatarUrl)
            .placeholder(R.drawable.ic_user_placeholder)
            .error(R.drawable.ic_user_placeholder)
            .circleCrop()
            .into(ivAvatar)

        rvRecords = findViewById(R.id.rvRecords)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)

        adapter = DoctorMedicalRecordAdapter(
            onViewClick = { record -> openRecordViewer(record) },
            onDownloadClick = { record -> downloadRecord(record) }
        )
        rvRecords.layoutManager = LinearLayoutManager(this)
        rvRecords.adapter = adapter

        chipGroupFilters.setOnCheckedChangeListener { _, checkedId ->
            applyFilters()
        }
    }

    private fun setupSocketListener() {
        SocketService.socket?.on("medical_record_shared") { args ->
            runOnUiThread {
                try {
                    val data = args[0] as JSONObject
                    val sharedPatientId = data.optInt("patient_id", -1)
                    if (sharedPatientId == patientId) {
                        fetchRecords()
                        Toast.makeText(this, "New record shared!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun fetchRecords() {
        progressBar.visibility = View.VISIBLE
        val doctorId = sessionManager.getUserId()
        
        ApiClient.instance.getMedicalRecords(
            userId = doctorId, 
            role = "doctor",
            patientId = patientId
        ).enqueue(object : Callback<List<MedicalRecordResponse>> {
            override fun onResponse(call: Call<List<MedicalRecordResponse>>, response: Response<List<MedicalRecordResponse>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    allRecords = response.body() ?: emptyList()
                    updateHeaderInfo()
                    applyFilters()
                } else {
                    Toast.makeText(this@DoctorPatientRecordsActivity, "Failed to load records", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MedicalRecordResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@DoctorPatientRecordsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateHeaderInfo() {
        findViewById<TextView>(R.id.tvTotalRecords).text = "${allRecords.size} Total Records"
    }

    private fun applyFilters() {
        val filteredList = when (chipGroupFilters.checkedChipId) {
            R.id.chipLabReports -> allRecords.filter { it.recordType?.lowercase()?.contains("lab") == true || it.recordType?.lowercase()?.contains("report") == true }
            R.id.chipPrescriptions -> allRecords.filter { it.recordType?.lowercase()?.contains("prescription") == true }
            R.id.chipDocuments -> allRecords.filter { it.recordType?.lowercase()?.contains("document") == true || it.fileName.lowercase().endsWith(".pdf") || it.fileName.lowercase().endsWith(".doc") }
            else -> allRecords
        }

        if (filteredList.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            rvRecords.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvRecords.visibility = View.VISIBLE
            adapter.submitList(filteredList)
        }
    }

    private fun openRecordViewer(record: MedicalRecordResponse) {
        val doctorId = sessionManager.getUserId()
        val tempFile = File(cacheDir, record.fileName)
        
        progressBar.visibility = View.VISIBLE
        ApiClient.instance.downloadMedicalRecord(record.id, doctorId, "doctor")
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        Thread {
                            try {
                                val input = response.body()!!.byteStream()
                                val output = FileOutputStream(tempFile)
                                input.use { i -> output.use { o -> i.copyTo(o) } }
                                
                                runOnUiThread {
                                    val uri = FileProvider.getUriForFile(
                                        this@DoctorPatientRecordsActivity,
                                        "${packageName}.provider",
                                        tempFile
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val ext = record.fileName.substringAfterLast('.', "").lowercase()
                                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                                    intent.setDataAndType(uri, mimeType)
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    startActivity(intent)
                                }
                            } catch (e: Exception) {
                                runOnUiThread { Toast.makeText(this@DoctorPatientRecordsActivity, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show() }
                            }
                        }.start()
                    } else {
                        Toast.makeText(this@DoctorPatientRecordsActivity, "Failed to fetch file", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@DoctorPatientRecordsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun downloadRecord(record: MedicalRecordResponse) {
        val doctorId = sessionManager.getUserId()
        Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()
        
        ApiClient.instance.downloadMedicalRecord(record.id, doctorId, "doctor")
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        saveToDownloads(record.fileName, response.body()!!)
                    } else {
                        Toast.makeText(this@DoctorPatientRecordsActivity, "Failed to download", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@DoctorPatientRecordsActivity, "Download error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveToDownloads(fileName: String, body: ResponseBody) {
        Thread {
            try {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    val ext = fileName.substringAfterLast('.', "").lowercase()
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                } else {
                    null // Handle legacy below
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    showDownloadSuccess(uri, fileName)
                } else {
                    // Legacy for older devices
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                    FileOutputStream(file).use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    val legacyUri = FileProvider.getUriForFile(this@DoctorPatientRecordsActivity, "${packageName}.provider", file)
                    showDownloadSuccess(legacyUri, fileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun showDownloadSuccess(uri: Uri, fileName: String) {
        runOnUiThread {
            val snackbar = Snackbar.make(rvRecords, "File downloaded successfully", Snackbar.LENGTH_LONG)
            snackbar.setAction("Open") {
                val intent = Intent(Intent.ACTION_VIEW)
                val ext = fileName.substringAfterLast('.', "").lowercase()
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                intent.setDataAndType(uri, mimeType)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            }
            snackbar.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketService.socket?.off("medical_record_shared")
    }
}
