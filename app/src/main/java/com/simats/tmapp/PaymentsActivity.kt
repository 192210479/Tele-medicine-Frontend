package com.simats.tmapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var tvWalletAmount: TextView
    private lateinit var llTransactionsContainer: LinearLayout
    private lateinit var llPaymentMethodsContainer: LinearLayout
    private var userId: Int = -1
    private var role = "patient"
    private lateinit var rvAdminPayments: androidx.recyclerview.widget.RecyclerView
    private var adminAdapter: AdminPaymentAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payments)

        sessionManager = SessionManager.getInstance(this)
        userId = sessionManager.getUserId()
        
        tvWalletAmount = findViewById(R.id.tvWalletAmount)
        llTransactionsContainer = findViewById(R.id.llTransactionsContainer)
        llPaymentMethodsContainer = findViewById(R.id.llPaymentMethodsContainer)
        rvAdminPayments = findViewById(R.id.rvAdminPayments)
        
        role = sessionManager.getUserRole().lowercase()
        if (role.contains("admin")) {
            role = "admin"
            // Hide patient specific parts
            findViewById<View>(R.id.tvWalletTitle)?.visibility = View.GONE
            findViewById<View>(R.id.btnAddMoney)?.visibility = View.GONE
            findViewById<View>(R.id.btnAddPayment)?.visibility = View.GONE
            findViewById<View>(R.id.llPaymentMethodsContainer)?.visibility = View.GONE
            findViewById<View>(R.id.llTransactionsContainer)?.visibility = View.GONE
            
            // Update titles for Admin view
            findViewById<TextView>(R.id.tvPaymentMethodsTitle)?.visibility = View.GONE
            findViewById<TextView>(R.id.tvRecentTransactionsTitle)?.text = "Platform Payments Management"
            
            rvAdminPayments.visibility = View.VISIBLE
            adminAdapter = AdminPaymentAdapter(emptyList())
            rvAdminPayments.adapter = adminAdapter
        }

        // Header Back Navigation
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        // Initial Data Load
        loadData()

        // Add Money Button
        findViewById<MaterialButton>(R.id.btnAddMoney).setOnClickListener {
            showAddMoneyDialog()
        }

        // Add Payment Method Button
        findViewById<MaterialButton>(R.id.btnAddPayment).setOnClickListener {
            showAddPaymentMethodDialog()
        }
    }

    private fun loadData() {
        fetchWalletHistory()
        fetchPaymentMethods()
        fetchProfileForBalance() // Wallet balance is stored in Patient model
    }

    private fun fetchProfileForBalance() {
        if (role == "admin") {
            tvWalletAmount.text = "N/A"
            return
        }
        ApiClient.instance.getProfile(userId, role).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful) {
                    // Update balance if needed
                }
            }
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {}
        })
    }

    private fun fetchWalletHistory() {
        if (userId == -1) return

        if (role == "admin") {
            ApiClient.instance.getAdminPayments().enqueue(object : Callback<List<AdminPaymentResponse>> {
                override fun onResponse(call: Call<List<AdminPaymentResponse>>, response: Response<List<AdminPaymentResponse>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val payments = response.body()!!
                        if (payments.isEmpty()) {
                            showEmptyState()
                        } else {
                            // Sort by latest payment date (assuming ISO format or similar string sortable)
                            val sorted = payments.sortedByDescending { it.date }
                            adminAdapter?.updateData(sorted)
                        }
                    } else {
                        showEmptyState()
                    }
                }
                override fun onFailure(call: Call<List<AdminPaymentResponse>>, t: Throwable) {
                    showEmptyState()
                }
            })
            return
        }

        ApiClient.instance.getWalletHistory(userId, role).enqueue(object : Callback<List<WalletHistoryResponse>> {
            override fun onResponse(call: Call<List<WalletHistoryResponse>>, response: Response<List<WalletHistoryResponse>>) {
                if (response.isSuccessful) {
                    val history = response.body() ?: emptyList()
                    updateTransactionsUI(history)
                    
                    // Update balance from latest if possible, but backend wallet/add returns it directly.
                    // For initial load, we might need a GET /api/wallet/balance endpoint which isn't in backend code provided.
                    // I will assume for now we just show history.
                }
            }

            override fun onFailure(call: Call<List<WalletHistoryResponse>>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Failed to load history", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPaymentMethods() {
        ApiClient.instance.listPaymentMethods(userId, role).enqueue(object : Callback<List<PaymentMethodResponse>> {
            override fun onResponse(call: Call<List<PaymentMethodResponse>>, response: Response<List<PaymentMethodResponse>>) {
                if (response.isSuccessful) {
                    updatePaymentMethodsUI(response.body() ?: emptyList())
                }
            }
            override fun onFailure(call: Call<List<PaymentMethodResponse>>, t: Throwable) {}
        })
    }

    private fun updateTransactionsUI(history: List<WalletHistoryResponse>) {
        llTransactionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        
        history.forEach { transaction ->
            val view = inflater.inflate(R.layout.item_transaction, llTransactionsContainer, false)
            view.findViewById<TextView>(R.id.tvTransactionTitle).text = transaction.status
            view.findViewById<TextView>(R.id.tvTransactionDate).text = transaction.date
            view.findViewById<TextView>(R.id.tvTransactionAmount).text = "$${transaction.amount}"
            view.findViewById<TextView>(R.id.tvTransactionStatus).text = "Success"
            llTransactionsContainer.addView(view)
        }
    }

    private fun showEmptyState() {
        if (role == "admin") {
            // Show empty text in RecyclerView or overlay
            Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePaymentMethodsUI(methods: List<PaymentMethodResponse>) {
        if (role == "admin") return
        llPaymentMethodsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        methods.forEach { method ->
            // Reusing a similar card style or simplifying for container
            val card = inflater.inflate(R.layout.item_payment_method, llPaymentMethodsContainer, false)
            card.findViewById<TextView>(R.id.tvCardInfo).text = "${method.card_type} •••• ${method.card_last4}"
            llPaymentMethodsContainer.addView(card)
        }
    }

    private fun showAddMoneyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_money, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)

        AlertDialog.Builder(this)
            .setTitle("Add Money to Wallet")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    addMoney(amount)
                } else {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addMoney(amount: Double) {
        val body = mapOf("user_id" to userId, "role" to role, "amount" to amount)
        
        ApiClient.instance.addWalletMoney(body).enqueue(object : Callback<WalletResponse> {
            override fun onResponse(call: Call<WalletResponse>, response: Response<WalletResponse>) {
                if (response.isSuccessful) {
                    val wallet = response.body()
                    tvWalletAmount.text = "$${String.format("%.2f", wallet?.wallet_balance ?: 0.0)}"
                    Toast.makeText(this@PaymentsActivity, "Successfully added $${amount}", Toast.LENGTH_SHORT).show()
                    fetchWalletHistory()
                }
            }
            override fun onFailure(call: Call<WalletResponse>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Error adding money", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddPaymentMethodDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment_method, null)
        val etCardNumber = dialogView.findViewById<EditText>(R.id.etCardNumber)
        val etCardType = dialogView.findViewById<EditText>(R.id.etCardType)

        AlertDialog.Builder(this)
            .setTitle("Add Payment Method")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val cardNum = etCardNumber.text.toString()
                val cardType = etCardType.text.toString()
                
                if (cardNum.length >= 4 && cardType.isNotEmpty()) {
                    val last4 = cardNum.takeLast(4)
                    addPaymentMethod(last4, cardType)
                } else {
                    Toast.makeText(this, "Invalid card details", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addPaymentMethod(last4: String, type: String) {
        val body = mapOf(
            "user_id" to userId,
            "role" to role,
            "card_last4" to last4,
            "card_type" to type
        )

        ApiClient.instance.addPaymentMethod(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PaymentsActivity, "Card saved", Toast.LENGTH_SHORT).show()
                    fetchPaymentMethods()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
        })
    }
}
