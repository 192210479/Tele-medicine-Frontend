package com.simats.tmapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReportViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_viewer)

        val reportTitle = intent.getStringExtra("REPORT_TITLE") ?: "Report Viewer"
        findViewById<TextView>(R.id.tvReportTitle).text = reportTitle

        findViewById<View>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<View>(R.id.ivDownload).setOnClickListener {
            Toast.makeText(this, "Downloading report...", Toast.LENGTH_SHORT).show()
        }
    }
}
