package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class UploadReportActivity : AppCompatActivity() {
    private var doctorId: Int = -1
    private var patientId: Int = -1
    private var appointmentId: Int = -1
    private var selectedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_report)

        findViewById<View>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        patientId = intent.getIntExtra("patient_id", SessionManager.getInstance(this).getUserId())
        appointmentId = intent.getIntExtra("appointment_id", -1)
        doctorId = intent.getIntExtra("doctor_id", -1)

        findViewById<View>(R.id.cvSelectFile).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            val mimeTypes = arrayOf(
                "application/pdf",
                "image/png",
                "image/jpeg",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(intent, 100)
        }

        findViewById<MaterialButton>(R.id.btnUpload).setOnClickListener {
            val recordType = if (appointmentId != -1) "Consultation Report" else findViewById<android.widget.EditText>(R.id.etRecordType).text.toString().ifEmpty { "Medical Report" }
            selectedFile?.let { file ->
                // Check file size (10MB = 10 * 1024 * 1024 bytes)
                if (file.length() > 10 * 1024 * 1024) {
                    Toast.makeText(this, "File size exceeds 10MB limit", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Check allowed formats
                val name = file.name.lowercase()
                val allowed = listOf(".pdf", ".png", ".jpg", ".jpeg", ".doc", ".docx")
                if (!allowed.any { name.endsWith(it) }) {
                     Toast.makeText(this, "Invalid file format. Allowed: PDF, PNG, JPG, JPEG, DOC, DOCX", Toast.LENGTH_SHORT).show()
                     return@setOnClickListener
                }

                uploadMedicalReport(file, patientId, recordType)
            } ?: run {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val file = getFileFromUri(uri)
                if (file != null) {
                    selectedFile = file
                    findViewById<TextView>(R.id.tvFileName).text = file.name
                }
            }
        }
    }

    private fun getFileFromUri(uri: android.net.Uri): File? {
        return try {
            val contentResolver = contentResolver
            val fileName = getFileName(uri)
            val file = File(cacheDir, "upload_" + System.currentTimeMillis() + "_" + fileName)
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: android.net.Uri): String {
        var name = "file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }

    fun uploadMedicalReport(file: File, patientId: Int, recordType: String) {
        val extension = file.extension.lowercase()
        val mimeType = when (extension) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val userId = patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val currentRole = SessionManager.getInstance(this).getUserRole().lowercase()
        val role = currentRole.toRequestBody("text/plain".toMediaTypeOrNull())
        val type = recordType.toRequestBody("text/plain".toMediaTypeOrNull())
        val patientIdBody = patientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val doctorIdBody = if (doctorId != -1) doctorId.toString().toRequestBody("text/plain".toMediaTypeOrNull()) else null
        val apptId = if (appointmentId != -1) appointmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull()) else null

        Toast.makeText(this, "Sharing report...", Toast.LENGTH_SHORT).show()
        ApiClient.instance.uploadMedicalRecord(
            userId = userId,
            role = role,
            recordType = type,
            patientId = patientIdBody,
            doctorId = doctorIdBody,
            appointmentId = apptId,
            file = listOf(body)
        ).enqueue(object : Callback<com.simats.Tmapp.api.GenericResponse> {
                override fun onResponse(call: Call<com.simats.Tmapp.api.GenericResponse>, response: Response<com.simats.Tmapp.api.GenericResponse>) {
                    if (response.isSuccessful || response.code() == 200) {
                        Toast.makeText(this@UploadReportActivity, "Report shared successfully", Toast.LENGTH_SHORT).show()
                        emitSocketEvent()
                        finish()
                    } else {
                        Toast.makeText(this@UploadReportActivity, "Sharing failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.simats.Tmapp.api.GenericResponse>, t: Throwable) {
                    Toast.makeText(this@UploadReportActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun emitSocketEvent() {
        try {
            val socket = SocketService.socket ?: return
            val currentRole = SessionManager.getInstance(this).getUserRole().lowercase()

            val data = org.json.JSONObject()
            data.put("appointment_id", appointmentId)
            data.put("patient_id", patientId)
            data.put("doctor_id", doctorId)
            data.put("role", currentRole)
            data.put("room", "consultation_$appointmentId")

            socket.emit("medical_record_uploaded", data)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
