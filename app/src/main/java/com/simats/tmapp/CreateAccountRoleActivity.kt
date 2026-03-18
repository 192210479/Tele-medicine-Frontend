package com.simats.tmapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CreateAccountRoleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account_role)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val clPatient = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.clPatient)
        val clDoctor = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.clDoctor)
        val ivRadioPatient = findViewById<ImageView>(R.id.ivRadioPatient)
        val ivRadioDoctor = findViewById<ImageView>(R.id.ivRadioDoctor)
        val tvRolePatient = findViewById<android.widget.TextView>(R.id.tvRolePatient)
        val tvRoleDoctor = findViewById<android.widget.TextView>(R.id.tvRoleDoctor)
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)

        var selectedRole = "doctor" // Initially doctor is selected in the layout

        fun updateUI() {
            if (selectedRole == "patient") {
                clPatient.setBackgroundResource(R.drawable.bg_card_selected)
                ivRadioPatient.setImageResource(R.drawable.ic_radio_selected)
                tvRolePatient.setTextColor(getColor(R.color.primary_blue))
                
                clDoctor.setBackgroundResource(R.drawable.bg_card_unselected)
                ivRadioDoctor.setImageResource(R.drawable.ic_radio_unselected)
                tvRoleDoctor.setTextColor(getColor(R.color.text_dark))
            } else {
                clDoctor.setBackgroundResource(R.drawable.bg_card_selected)
                ivRadioDoctor.setImageResource(R.drawable.ic_radio_selected)
                tvRoleDoctor.setTextColor(getColor(R.color.primary_blue))
                
                clPatient.setBackgroundResource(R.drawable.bg_card_unselected)
                ivRadioPatient.setImageResource(R.drawable.ic_radio_unselected)
                tvRolePatient.setTextColor(getColor(R.color.text_dark))
            }
        }

        clPatient.setOnClickListener {
            selectedRole = "patient"
            updateUI()
        }

        clDoctor.setOnClickListener {
            selectedRole = "doctor"
            updateUI()
        }

        ivBack.setOnClickListener {
            finish()
        }

        btnContinue.setOnClickListener {
            val intent = if (selectedRole == "patient") {
                Intent(this, CreateAccountDetailsActivity::class.java)
            } else {
                Intent(this, CreateDoctorDetailsActivity::class.java)
            }
            startActivity(intent)
        }
    }
}
