package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.simats.Tmapp.api.ApiClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.core.content.FileProvider

class DoctorRecordViewerActivity : AppCompatActivity() {

    private lateinit var ivRecordImage: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvFileName: TextView
    private lateinit var tvError: TextView

    private var recordId: Int = -1
    private var fileName: String = ""
    private var filePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_record_viewer)

        recordId = intent.getIntExtra("record_id", -1)
        fileName = intent.getStringExtra("file_name") ?: "document"
        filePath = intent.getStringExtra("file_path") ?: ""

        initViews()
        loadFile()
    }

    private fun initViews() {
        ivRecordImage = findViewById(R.id.ivRecordImage)
        progressBar = findViewById(R.id.progressBar)
        tvFileName = findViewById(R.id.tvFileName)
        tvError = findViewById(R.id.tvError)

        tvFileName.text = fileName

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<View>(R.id.ivDownload).setOnClickListener { startNativeDownload() }
    }

    private fun loadFile() {
        val doctorId = SessionManager.getInstance(this).getUserId()
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        ApiClient.instance.downloadMedicalRecord(recordId, doctorId, "doctor")
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        handleFileResponse(response.body()!!)
                    } else {
                        showError("Failed to load file")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    showError("Error: ${t.message}")
                }
            })
    }

    private fun handleFileResponse(body: ResponseBody) {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        
        if (ext in listOf("jpg", "jpeg", "png")) {
            progressBar.visibility = View.GONE
            ivRecordImage.visibility = View.VISIBLE
            Thread {
                try {
                    val file = File(cacheDir, "temp_view_$recordId.$ext")
                    saveFile(body.byteStream(), file)
                    runOnUiThread {
                        Glide.with(this).load(file).into(ivRecordImage)
                    }
                } catch (e: Exception) {
                    runOnUiThread { showError("Could not load image") }
                }
            }.start()
        } else {
            progressBar.visibility = View.GONE
            showError("Please download to view this file type: $ext")
        }
    }

    private fun saveFile(inputStream: InputStream, file: File) {
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun getMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "*/*"
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startNativeDownload() {
        val doctorId = SessionManager.getInstance(this).getUserId()
        progressBar.visibility = View.VISIBLE

        ApiClient.instance.downloadMedicalRecord(recordId, doctorId, "doctor")
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        Thread {
                            try {
                                val ext = fileName.substringAfterLast('.', "pdf")
                                val file = File(getExternalFilesDir(null), fileName.ifBlank { "record_$recordId.$ext" })
                                saveFile(response.body()!!.byteStream(), file)

                                val uri = FileProvider.getUriForFile(
                                    this@DoctorRecordViewerActivity,
                                    "${packageName}.provider",
                                    file
                                )

                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, getMimeType(file.extension))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(intent, "Open file"))
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this@DoctorRecordViewerActivity, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.start()
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@DoctorRecordViewerActivity, "Failed to download file", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@DoctorRecordViewerActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
