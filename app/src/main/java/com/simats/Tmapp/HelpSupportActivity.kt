package com.simats.Tmapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.CreateSupportTicketRequest
import com.simats.Tmapp.api.SupportFaqResponse
import com.simats.Tmapp.api.SupportTicketCreateResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HelpSupportActivity : AppCompatActivity() {

    private lateinit var faqContainer: LinearLayout
    private lateinit var tvFaqEmpty: TextView

    private var userId: Int = -1
    private var userRole: String = "patient"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)

        val sessionManager = SessionManager(this)

        userId = intent.getIntExtra("user_id", -1)
        if (userId <= 0) {
            userId = sessionManager.getUserId()
        }

        userRole = intent.getStringExtra("userRole")
            ?: sessionManager.getUserRole()
            ?: "patient"

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val cardLiveChat = findViewById<MaterialCardView>(R.id.cardLiveChat)
        val cardEmailUs = findViewById<MaterialCardView>(R.id.cardEmailUs)
        val btnCallNow = findViewById<MaterialButton>(R.id.btnCallNow)

        if (userRole == "admin") {
            cardEmailUs.visibility = View.GONE
        }

        val btnMyTickets = findViewById<MaterialButton>(R.id.btnMyTickets)

        btnMyTickets.setOnClickListener {
            val intent = Intent(this, MyTicketsActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        faqContainer = findViewById(R.id.faqContainer)
        tvFaqEmpty = findViewById(R.id.tvFaqEmpty)

        ivBack.setOnClickListener { finish() }

        cardLiveChat.setOnClickListener {
            val intent = Intent(this, AiSupportChatActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        cardEmailUs.setOnClickListener {
            showRaiseTicketDialog()
        }

        btnCallNow.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+1234567890")
            }
            startActivity(intent)
        }
        
        loadFaqs()
    }

    private fun loadFaqs() {
        ApiClient.instance.getSupportFaqs(userRole)
            .enqueue(object : Callback<List<SupportFaqResponse>> {
                override fun onResponse(
                    call: Call<List<SupportFaqResponse>>,
                    response: Response<List<SupportFaqResponse>>
                ) {
                    faqContainer.removeAllViews()

                    val faqs = response.body().orEmpty()

                    if (faqs.isEmpty()) {
                        tvFaqEmpty.visibility = View.VISIBLE
                        return
                    }

                    tvFaqEmpty.visibility = View.GONE

                    for (faq in faqs) {
                        addFaqCard(faq)
                    }
                }

                override fun onFailure(call: Call<List<SupportFaqResponse>>, t: Throwable) {
                    tvFaqEmpty.visibility = View.VISIBLE
                    Toast.makeText(
                        this@HelpSupportActivity,
                        "Failed to load FAQs",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun addFaqCard(faq: SupportFaqResponse) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_faq_card, faqContainer, false)

        val tvQuestion = view.findViewById<TextView>(R.id.tvQuestion)
        val tvAnswer = view.findViewById<TextView>(R.id.tvAnswer)
        val ivToggle = view.findViewById<ImageView>(R.id.ivToggle)

        tvQuestion.text = faq.question
        tvAnswer.text = faq.answer
        tvAnswer.visibility = View.GONE

        view.setOnClickListener {
            val expanded = tvAnswer.visibility == View.VISIBLE
            tvAnswer.visibility = if (expanded) View.GONE else View.VISIBLE
            ivToggle.rotation = if (expanded) 0f else 180f
        }

        faqContainer.addView(view)
    }

    private fun showRaiseTicketDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_raise_ticket, null)

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etIssueType = dialogView.findViewById<TextInputEditText>(R.id.etIssueType)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)

        AlertDialog.Builder(this)
            .setTitle("Raise Support Ticket")
            .setView(dialogView)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    val submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    submitBtn.setOnClickListener {
                        val title = etTitle.text.toString().trim()
                        val issueType = etIssueType.text.toString().trim().ifEmpty { "other" }
                        val description = etDescription.text.toString().trim()

                        if (title.isEmpty() || description.isEmpty()) {
                            Toast.makeText(
                                this,
                                "Title and description required",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        createTicket(title, issueType, description)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun createTicket(title: String, issueType: String, description: String) {
        if (userId <= 0) {
            Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateSupportTicketRequest(
            user_id = userId,
            role = userRole,
            issue_type = issueType,
            title = title,
            description = description
        )

        ApiClient.instance.createSupportTicket(request)
            .enqueue(object : Callback<SupportTicketCreateResponse> {
                override fun onResponse(
                    call: Call<SupportTicketCreateResponse>,
                    response: Response<SupportTicketCreateResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(
                            this@HelpSupportActivity,
                            "Ticket created: #${response.body()!!.ticket_id}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@HelpSupportActivity,
                            "Failed to create ticket",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<SupportTicketCreateResponse>, t: Throwable) {
                    Toast.makeText(
                        this@HelpSupportActivity,
                        "Server error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
