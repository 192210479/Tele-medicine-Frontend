package com.simats.tmapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.simats.tmapp.api.GenericResponse
import com.simats.tmapp.api.MedicalRecordResponse
import com.simats.tmapp.api.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MedicalRecordsActivity : AppCompatActivity() {

    private lateinit var rvMedicalRecords: RecyclerView
    private lateinit var adapter: MedicalReportAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var llEmptyState: LinearLayout
    private lateinit var sessionManager: SessionManager
    private lateinit var btnRequestRecord: com.google.android.material.button.MaterialButton

    private lateinit var tabAll: MaterialCardView
    private lateinit var tabLab: MaterialCardView
    private lateinit var tabPrescription: MaterialCardView
    private lateinit var tabDocument: MaterialCardView

    private lateinit var tvTabAll: TextView
    private lateinit var tvTabLab: TextView
    private lateinit var tvTabPrescription: TextView
    private lateinit var tvTabDocument: TextView

    private var allRecords = mutableListOf<MedicalRecordResponse>()
    private var currentRole: String = "patient"
    private var patientId: Int = -1

    private var appointmentId: Int = -1
    private lateinit var socket: io.socket.client.Socket
    private var isSocketInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_records)

        sessionManager = SessionManager.getInstance(this)
        currentRole = sessionManager.getUserRole().lowercase()
        // If doctor, they must have passed an appointment_id or patient_id.
        // If patient, use their own.
        patientId = intent.getIntExtra("patient_id", if (currentRole == "patient") sessionManager.getUserId() else -1)
        appointmentId = intent.getIntExtra("appointment_id", -1)

        initViews()
        setupRecyclerView()
        setupListeners()
        setupSocket()
        
        fetchMedicalRecords()
    }

    private fun setupSocket() {
        val s = SocketService.socket
        if (s != null) {
            socket = s
            isSocketInitialized = true
            
            val userId = sessionManager.getUserId()
            val roomName = if (currentRole == "doctor") "doctor_$userId" else "patient_$userId"
            socket.emit("join_room", roomName)

            socket.on("medical_record_uploaded") {
                runOnUiThread { fetchMedicalRecords() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isSocketInitialized) {
            socket.off("medical_record_uploaded")
        }
    }

    private fun initViews() {
        // ...Existing views initialization...
        rvMedicalRecords = findViewById(R.id.rvMedicalRecords)
        progressBar = findViewById(R.id.progressBar)
        llEmptyState = findViewById(R.id.llEmptyState)

        tabAll = findViewById(R.id.tabLabReports)
        tabLab = findViewById(R.id.tabPrescriptions)
        tabPrescription = findViewById(R.id.tabDiagnoses)
        tabDocument = findViewById(R.id.tabDocuments)

        tvTabAll = findViewById(R.id.tvTabLabReports)
        tvTabLab = findViewById(R.id.tvTabPrescriptions)
        tvTabPrescription = findViewById(R.id.tvTabDiagnoses)
        tvTabDocument = findViewById(R.id.tvTabDocuments)
        
        btnRequestRecord = findViewById(R.id.btnRequestRecord)
        
        if (currentRole == "doctor") {
            btnRequestRecord.visibility = View.VISIBLE
        } else {
            btnRequestRecord.visibility = View.GONE
        }
        
        // Hide upload buttons if role is doctor and no patient selected? 
        // No, doctor can upload too according to requirement (Section 6).
    }

    private fun setupRecyclerView() {
        adapter = MedicalReportAdapter(
            currentRole,
            onItemClick = { record -> downloadRecord(record) },
            onDownloadClick = { record -> downloadRecord(record) },
            onShareClick = { record -> 
                if (currentRole == "patient") {
                    shareRecord(record)
                } else {
                    downloadRecord(record)
                }
            }
        )
        rvMedicalRecords.layoutManager = LinearLayoutManager(this)
        rvMedicalRecords.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnUploadTop).setOnClickListener { openFilePicker() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpload).setOnClickListener { openFilePicker() }
        btnRequestRecord.setOnClickListener { showRequestRecordDialog() }

        tabAll.setOnClickListener { handleTabClick(0) }
        tabLab.setOnClickListener { handleTabClick(1) }
        tabPrescription.setOnClickListener { handleTabClick(2) }
        tabDocument.setOnClickListener { handleTabClick(3) }
    }

    private fun showRequestRecordDialog() {
        val editText = EditText(this)
        editText.hint = "Enter record type (e.g. ECG, Blood Test)"
        editText.setPadding(40, 20, 40, 20)
        
        AlertDialog.Builder(this)
            .setTitle("Request Medical Record")
            .setMessage("Ask patient to share a specific report for this consultation.")
            .setView(editText)
            .setPositiveButton("Send Request") { _, _ ->
                val type = editText.text.toString()
                if (type.isNotEmpty()) requestRecordFromServer(type)
                else Toast.makeText(this, "Please enter record type", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestRecordFromServer(recordType: String) {
        val doctorId = sessionManager.getUserId()
        val body = mapOf(
            "doctor_id" to doctorId,
            "patient_id" to patientId,
            "appointment_id" to appointmentId,
            "record_type" to recordType
        )
        
        ApiClient.instance.requestMedicalRecord(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MedicalRecordsActivity, "Request sent to patient", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MedicalRecordsActivity, "Failed to send request", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                 Toast.makeText(this@MedicalRecordsActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        val extraMimeTypes = arrayOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
        intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
        pickFileLauncher.launch(intent)
    }

    private fun showUploadDialog(uri: Uri) {
        val editText = EditText(this)
        editText.hint = "Record type (e.g. Blood Test)"
        
        AlertDialog.Builder(this)
            .setTitle("Upload Record")
            .setView(editText)
            .setPositiveButton("Upload") { _, _ ->
                val type = editText.text.toString().ifEmpty { "Medical Record" }
                uploadFile(uri, type)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchMedicalRecords() {
        if (!::progressBar.isInitialized) return
        progressBar.visibility = View.VISIBLE
        
        val userId = sessionManager.getUserId()
        if (userId == -1) {
            progressBar.visibility = View.GONE
            return
        }

        // Section 3 & 6: Standardized Fetch
        val call = if (currentRole == "doctor") {
            // Doctors ONLY see records shared for a specific appointment
            if (appointmentId == -1) {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Access restricted to shared appointment records.", Toast.LENGTH_SHORT).show()
                updateList(emptyList())
                return
            }
            ApiClient.instance.getMedicalRecords(userId = -1, role = currentRole, appointmentId = appointmentId)
        } else {
            // Patients see all their own records privately
            ApiClient.instance.getMedicalRecords(userId = userId, role = currentRole)
        }

        call.enqueue(object : Callback<List<MedicalRecordResponse>> {
            override fun onResponse(call: Call<List<MedicalRecordResponse>>, response: Response<List<MedicalRecordResponse>>) {
                if (!::progressBar.isInitialized) return
                progressBar.visibility = View.GONE
                try {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            allRecords = body.toMutableList()
                        } else {
                            android.util.Log.e("MEDICAL_RECORDS", "Successful response but body is null")
                            allRecords = mutableListOf()
                        }
                        updateList(allRecords)
                    } else {
                        val rawResponse = response.errorBody()?.string() ?: ""
                        android.util.Log.e("MEDICAL_RECORDS", "API Error (${response.code()}): $rawResponse")
                        updateList(emptyList())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MEDICAL_RECORDS", "Json Parsing Exception: ${e.message}")
                    updateList(emptyList())
                }
            }

            override fun onFailure(call: Call<List<MedicalRecordResponse>>, t: Throwable) {
                if (!::progressBar.isInitialized) return
                progressBar.visibility = View.GONE
                android.util.Log.e("MEDICAL_RECORDS", "Network or Parsing Failure: ${t.message}")
                updateList(emptyList())
            }
        })
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateList(records: List<MedicalRecordResponse>?) {
        // Section 8 & 9: Crash Protection and Empty State
        try {
            val finalRecords = records ?: emptyList()
            if (finalRecords.isEmpty()) {
                llEmptyState.visibility = View.VISIBLE
                rvMedicalRecords.visibility = View.GONE
                adapter.submitList(emptyList())
            } else {
                llEmptyState.visibility = View.GONE
                rvMedicalRecords.visibility = View.VISIBLE
                adapter.submitList(finalRecords)
            }
        } catch (e: Exception) {
            android.util.Log.e("MEDICAL_RECORDS", "Error updating list: ${e.message}")
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showUploadDialog(uri)
            }
        }
    }

    private fun uploadFile(uri: Uri, recordType: String) {
        val doctorOrPatientId = sessionManager.getUserId()
        if (doctorOrPatientId == -1) return

        val file = getFileFromUri(uri)
        if (file == null) {
            Toast.makeText(this, "Could not access selected file", Toast.LENGTH_SHORT).show()
            return
        }
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        // Section 2: Correct Multipart Request
        val userIdPart = doctorOrPatientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val rolePart = currentRole.toRequestBody("text/plain".toMediaTypeOrNull())
        val typePart = recordType.toRequestBody("text/plain".toMediaTypeOrNull())

        val patientPart = if (currentRole == "doctor" && patientId != -1) {
            patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        } else if (currentRole == "patient") {
            doctorOrPatientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        val doctorPart = if (currentRole == "doctor") {
            doctorOrPatientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        val appointmentPart = if (appointmentId != -1) {
            appointmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        ApiClient.instance.uploadMedicalRecord(
            userId = userIdPart,
            role = rolePart,
            recordType = typePart,
            patientId = patientPart,
            doctorId = doctorPart,
            appointmentId = appointmentPart,
            file = filePart
        )
            .enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (!::progressBar.isInitialized) return
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@MedicalRecordsActivity, "Medical record uploaded successfully", Toast.LENGTH_SHORT).show()
                        fetchMedicalRecords()
                        // Emit socket event for real-time update
                        if (isSocketInitialized) {
                            try {
                                val data = org.json.JSONObject()
                                data.put("user_id", doctorOrPatientId)
                                data.put("role", currentRole)
                                if (appointmentId != -1) data.put("appointment_id", appointmentId)
                                socket.emit("medical_record_uploaded", data)
                            } catch(e: Exception) {}
                        }
                    } else {
                        Toast.makeText(this@MedicalRecordsActivity, "Upload failed. (Code: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MedicalRecordsActivity, "Upload error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun downloadRecord(record: MedicalRecordResponse) {
        progressBar.visibility = View.VISIBLE
        val userId = sessionManager.getUserId()
        ApiClient.instance.downloadMedicalRecord(record.id, userId, currentRole).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        saveAndOpenFile(body, record.fileName)
                    }
                } else {
                    Toast.makeText(this@MedicalRecordsActivity, "Download failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@MedicalRecordsActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAndOpenFile(body: ResponseBody, fileName: String) {
        try {
            val file = File(getExternalFilesDir(null), fileName)
            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            openFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareRecord(record: MedicalRecordResponse) {
        if (appointmentId == -1) {
            Toast.makeText(this, "Record sharing requires an active appointment.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = sessionManager.getUserId()

        val body = mapOf(
            "user_id" to userId,
            "appointment_id" to appointmentId,
            "record_ids" to listOf(record.id)
        )

        ApiClient.instance.shareRecordsToAppointment(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MedicalRecordsActivity, "Report shared with doctor", Toast.LENGTH_SHORT).show()
                    emitShareSocketEvent(record.id)
                    fetchMedicalRecords()
                } else {
                    Toast.makeText(this@MedicalRecordsActivity, "Sharing failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@MedicalRecordsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun emitShareSocketEvent(recordId: Int) {
        try {
            val data = org.json.JSONObject()
            data.put("user_id", sessionManager.getUserId())
            data.put("role", "patient")
            data.put("appointment_id", appointmentId)
            data.put("record_id", recordId)
            SocketService.socket?.emit("medical_record_shared", data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val fileName = getFileName(uri) ?: "temp_file"
        val file = File(cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        } catch (e: Exception) {
            return null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }

    private fun handleTabClick(position: Int) {
        resetTabs()
        when (position) {
            0 -> {
                activateTab(tabAll, tvTabAll)
                updateList(allRecords)
            }
            1 -> {
                activateTab(tabLab, tvTabLab)
                updateList(allRecords.filter { it.fileName.contains("lab", true) || it.fileName.contains("test", true) || it.fileName.contains("report", true) })
            }
            2 -> {
                activateTab(tabPrescription, tvTabPrescription)
                updateList(allRecords.filter { it.fileName.contains("presc", true) })
            }
            3 -> {
                activateTab(tabDocument, tvTabDocument)
                updateList(allRecords.filter { it.fileName.contains("doc", true) || it.fileName.contains("pdf", true) })
            }
        }
    }

    private fun activateTab(tab: MaterialCardView, text: TextView) {
        tab.setCardBackgroundColor(Color.WHITE)
        tab.cardElevation = 4f
        text.setTextColor(Color.parseColor("#2F6FED"))
    }

    private fun resetTabs() {
        deactivateTab(tabAll, tvTabAll)
        deactivateTab(tabLab, tvTabLab)
        deactivateTab(tabPrescription, tvTabPrescription)
        deactivateTab(tabDocument, tvTabDocument)
    }

    private fun deactivateTab(tab: MaterialCardView, text: TextView) {
        tab.setCardBackgroundColor(Color.TRANSPARENT)
        tab.cardElevation = 0f
        text.setTextColor(Color.parseColor("#64748B"))
    }
}
