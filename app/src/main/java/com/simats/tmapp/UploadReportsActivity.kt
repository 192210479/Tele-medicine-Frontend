package com.simats.tmapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class UploadReportsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consultation_waiting) // Reusing layout for placeholder

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }
    }
}
