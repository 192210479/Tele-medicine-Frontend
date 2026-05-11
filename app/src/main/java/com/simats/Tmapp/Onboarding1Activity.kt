package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Onboarding1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding1)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnNext.setOnClickListener {
            startActivity(Intent(this, Onboarding2Activity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        tvSkip.setOnClickListener {
            // Mark onboarding as seen if skipped
            SessionManager.getInstance(this).setOnboardingSeen(true)
            
            startActivity(Intent(this, AuthWelcomeActivity::class.java))
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
            finish()
        }
    }
}