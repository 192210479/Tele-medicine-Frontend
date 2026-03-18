package com.simats.tmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.simats.tmapp.api.ApiClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startNativeDownload() {
        val doctorId = SessionManager.getInstance(this).getUserId()
        val url = "${ApiClient.BASE_URL}api/medical-record/download/$recordId?user_id=$doctorId&role=doctor"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }
}
