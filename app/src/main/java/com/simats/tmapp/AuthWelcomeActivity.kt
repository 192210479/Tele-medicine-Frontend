package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AuthWelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initial theme application
        ThemeManager.applyTheme(SessionManager.getInstance(this))
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth_welcome)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnCreateAccount = findViewById<Button>(R.id.btnCreateAccount)
        val btnGuest = findViewById<TextView>(R.id.btnGuest)

        btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnCreateAccount.setOnClickListener {
            val intent = Intent(this, CreateAccountRoleActivity::class.java)
            startActivity(intent)
        }

        btnGuest.setOnClickListener {
            Toast.makeText(this, "Continue as Guest Clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
