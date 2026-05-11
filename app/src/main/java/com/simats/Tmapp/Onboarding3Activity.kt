package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Onboarding3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding3)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {
            // Mark onboarding as seen
            SessionManager.getInstance(this).setOnboardingSeen(true)
            
            startActivity(Intent(this, AuthWelcomeActivity::class.java))
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
            finish()
        }
    }
}