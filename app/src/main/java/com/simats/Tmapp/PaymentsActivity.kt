package com.simats.Tmapp

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.razorpay.Checkout
import com.simats.Tmapp.api.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class PaymentsActivity : AppCompatActivity(), com.razorpay.PaymentResultWithDataListener {

    // ── Core views (from existing activity_payments.xml) ─────────────
    private lateinit var sessionManager: SessionManager
    private lateinit var tvWalletAmount: TextView
    private lateinit var tvWalletTitle: TextView
    private lateinit var llTransactionsContainer: LinearLayout
    private lateinit var llPaymentMethodsContainer: LinearLayout
    private lateinit var tvPaymentMethodsTitle: TextView
    private lateinit var tvRecentTransactionsTitle: TextView
    private lateinit var btnAddMoney: MaterialButton
    private lateinit var btnAddPayment: MaterialButton
    private lateinit var rvAdminPayments: androidx.recyclerview.widget.RecyclerView

    // ── Session ────────────────────────────────────────────────────────
    private var userId: Int = -1
    private var role = "patient"

    // ── Data ───────────────────────────────────────────────────────────
    private var adminAdapter: AdminPaymentAdapter? = null
    private var doctorBankAccounts: List<DoctorBankAccount> = emptyList()
    private var currentAvailableBalance: Double = 0.0
    private var billingHistory: List<BillingHistoryItem> = emptyList()

    // ── Razorpay pending state ─────────────────────────────────────────
    private var pendingOrderId: String? = null
    private var pendingPaymentDbId: Int? = null
    private var pendingAmount: Double? = null
    private var pendingDoctorId: Int? = null
    private var pendingAppointmentId: Int? = null
    private var pendingDate: String? = null
    private var pendingTime: String? = null
    private var pendingDoctorName: String? = null

    // ── Triggered from booking flow extras ───────────────────────────
    private var triggerPaymentOnOpen = false

    private val api = ApiClient.instance
    private val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // ── Real-time socket listener for payment/wallet events ────────────
    private val paymentEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "PAYMENT_SUCCESS_RECEIVED" -> {
                    loadData()
                    Toast.makeText(this@PaymentsActivity, "✅ Payment confirmed!", Toast.LENGTH_SHORT).show()
                }
                "WALLET_UPDATE_RECEIVED" -> {
                    if (role == "doctor") fetchDoctorData()
                }
                "REFUND_UPDATE_RECEIVED" -> {
                    loadData()
                    Toast.makeText(this@PaymentsActivity, "💸 Refund processed!", Toast.LENGTH_SHORT).show()
                }
                "NEW_NOTIFICATION_RECEIVED" -> {
                    // badge refresh handled globally by SocketService
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payments)

        sessionManager = SessionManager.getInstance(this)
        userId = sessionManager.getUserId()
        role = sessionManager.getUserRole().lowercase()

        // ── Check if triggered from booking flow ───────────────────────
        triggerPaymentOnOpen = intent.getBooleanExtra("trigger_payment", false)

        bindViews()
        setupBackButton()

        // Pre-fetch Razorpay checkout assets for faster payment sheet
        Checkout.preload(applicationContext)

        // Role-based UI customisation
        when {
            role.contains("admin")  -> setupAdminUI()
            role.contains("doctor") -> setupDoctorUI()
            else                    -> setupPatientUI()
        }

        loadData()

        // Register local broadcast for real-time socket events
        val filter = IntentFilter().apply {
            addAction("PAYMENT_SUCCESS_RECEIVED")
            addAction("WALLET_UPDATE_RECEIVED")
            addAction("REFUND_UPDATE_RECEIVED")
            addAction("NEW_NOTIFICATION_RECEIVED")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(paymentEventReceiver, filter)

        // If opened from booking flow with payment trigger, auto-open Razorpay
        if (triggerPaymentOnOpen) {
            val doctorId      = intent.getIntExtra("doctor_id", -1)
            val appointmentId = intent.getIntExtra("appointment_id", -1)
            pendingDate = intent.getStringExtra("DATE")
            pendingTime = intent.getStringExtra("TIME")
            pendingDoctorName = intent.getStringExtra("doctor_name")
            val amount        = intent.getDoubleExtra("amount", 0.0)
            val doctorName    = intent.getStringExtra("doctor_name") ?: ""
            val patientEmail  = sessionManager.getUserEmail() ?: ""
            val patientPhone  = sessionManager.getUserPhone() ?: ""

            if (doctorId != -1 && appointmentId != -1 && amount > 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showPaymentConfirmationDialog(
                        doctorId,
                        appointmentId,
                        amount,
                        doctorName,
                        patientEmail,
                        patientPhone
                    )
                }, 400)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(paymentEventReceiver)
    }

    // ── Bind existing views ────────────────────────────────────────────
    private fun bindViews() {
        tvWalletAmount             = findViewById(R.id.tvWalletAmount)
        tvWalletTitle              = findViewById(R.id.tvWalletTitle)
        llTransactionsContainer    = findViewById(R.id.llTransactionsContainer)
        llPaymentMethodsContainer  = findViewById(R.id.llPaymentMethodsContainer)
        tvPaymentMethodsTitle      = findViewById(R.id.tvPaymentMethodsTitle)
        tvRecentTransactionsTitle  = findViewById(R.id.tvRecentTransactionsTitle)
        btnAddMoney                = findViewById(R.id.btnAddMoney)
        btnAddPayment              = findViewById(R.id.btnAddPayment)
        rvAdminPayments            = findViewById(R.id.rvAdminPayments)
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    // ── Role-based UI setup (modifies existing layout, no new views) ───
    private fun setupAdminUI() {
        role = "admin"
        tvWalletTitle.text             = "Platform Net Earnings"
        tvRecentTransactionsTitle.text = "Payment Records"
        btnAddMoney.visibility         = View.GONE
        btnAddPayment.visibility       = View.GONE
        llPaymentMethodsContainer.visibility = View.GONE
        tvPaymentMethodsTitle.visibility     = View.GONE
        llTransactionsContainer.visibility   = View.GONE
        rvAdminPayments.visibility     = View.VISIBLE
        rvAdminPayments.layoutManager  = LinearLayoutManager(this)
        adminAdapter = AdminPaymentAdapter(emptyList())
        rvAdminPayments.adapter = adminAdapter
    }

    private fun showPaymentConfirmationDialog(
        doctorId: Int,
        appointmentId: Int,
        amount: Double,
        doctorName: String,
        patientEmail: String,
        patientPhone: String
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Complete Payment")
            .setMessage("Pay ${fmt.format(amount)} to confirm your appointment with Dr. $doctorName?")
            .setPositiveButton("Pay Now") { _, _ ->
                startRazorpayPayment(
                    doctorId,
                    appointmentId,
                    amount,
                    doctorName,
                    patientEmail,
                    patientPhone
                )
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun verifyPaymentOnServer(
        razorpayPaymentId: String,
        razorpayOrderId: String,
        razorpaySignature: String
    ) {
        val request = VerifyPaymentRequest(
            patientId = userId,
            doctorId = pendingDoctorId ?: -1,
            appointmentId = pendingAppointmentId ?: -1,
            orderId = razorpayOrderId,
            razorpayPaymentId = razorpayPaymentId,
            razorpaySignature = razorpaySignature,
            amount = pendingAmount ?: 0.0,
            timezone = java.util.TimeZone.getDefault().id
        )

        api.verifyPaymentFull(request).enqueue(object : Callback<VerifyPaymentResponse> {
            override fun onResponse(
                call: Call<VerifyPaymentResponse>,
                response: Response<VerifyPaymentResponse>
            ) {
                val body = response.body()

                if (response.isSuccessful && body?.success == true) {
                    val confirmedAppointmentId = pendingAppointmentId ?: body.appointmentId ?: -1

                    Toast.makeText(this@PaymentsActivity, "Payment successful", Toast.LENGTH_SHORT).show()

                    val tempDate = pendingDate
                    val tempTime = pendingTime
                    val tempDoctorName = pendingDoctorName

                    pendingOrderId = null
                    pendingPaymentDbId = null
                    pendingAmount = null
                    pendingDoctorId = null
                    pendingAppointmentId = null
                    pendingDate = null
                    pendingTime = null
                    pendingDoctorName = null

                    loadData()

                    val intent = Intent(this@PaymentsActivity, BookingConfirmedActivity::class.java)
                    intent.putExtra("appointment_id", confirmedAppointmentId)
                    intent.putExtra("payment_success", true)
                    intent.putExtra("DATE", tempDate ?: "")
                    intent.putExtra("TIME", tempTime ?: "")
                    intent.putExtra("DOCTOR_NAME", tempDoctorName ?: "")
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@PaymentsActivity,
                        body?.error ?: body?.message ?: "Payment verification failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<VerifyPaymentResponse>, t: Throwable) {
                Toast.makeText(
                    this@PaymentsActivity,
                    "Verification failed: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun setupDoctorUI() {
        role = "doctor"
        tvWalletTitle.text             = "Available Balance"
        tvRecentTransactionsTitle.text = "Earnings History"

        llPaymentMethodsContainer.visibility = View.GONE
        tvPaymentMethodsTitle.visibility     = View.GONE

        // Reuse existing buttons without changing XML
        btnAddMoney.visibility = View.VISIBLE
        btnAddPayment.visibility = View.VISIBLE

        btnAddMoney.text = "Manage Accounts"
        btnAddMoney.setOnClickListener { showBankAccountsMenu() }

        btnAddPayment.text = "Withdraw Earnings"
        btnAddPayment.setOnClickListener { showWithdrawDialog() }
    }

    private fun setupPatientUI() {
        role = "patient"
        tvWalletTitle.text = "Total Consultation Payments"

        btnAddMoney.setOnClickListener { showAddMoneyInfo() }
        btnAddPayment.setOnClickListener { showAddPaymentMethodMenu() }
    }

    // ── Load data by role ─────────────────────────────────────────────
    private fun loadData() {
        when {
            role == "admin"  -> fetchAdminData()
            role == "doctor" -> fetchDoctorData()
            else             -> fetchPatientData()
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // PATIENT DATA
    // ──────────────────────────────────────────────────────────────────

    private fun fetchPatientData() {
        // Billing history (invoices with full enriched data)
        api.getBillingHistory(userId).enqueue(object : Callback<List<BillingHistoryItem>> {
            override fun onResponse(call: Call<List<BillingHistoryItem>>, response: Response<List<BillingHistoryItem>>) {
                if (response.isSuccessful) {
                    billingHistory = response.body() ?: emptyList()
                    val totalPaid = billingHistory
                        .filter { it.paymentStatus?.lowercase() == "success" }
                        .sumOf { it.totalAmount }
                    tvWalletAmount.text = fmt.format(totalPaid)
                    updatePatientTransactionsUI(billingHistory)
                }
            }
            override fun onFailure(call: Call<List<BillingHistoryItem>>, t: Throwable) {
                // Fallback to legacy endpoint
                fetchPatientPaymentsFallback()
            }
        })
        fetchSavedPaymentMethods()
    }

    private fun fetchPatientPaymentsFallback() {
        api.getPatientPayments(userId).enqueue(object : Callback<List<TransactionResponse>> {
            override fun onResponse(call: Call<List<TransactionResponse>>, response: Response<List<TransactionResponse>>) {
                if (response.isSuccessful) updateTransactionsUI(response.body() ?: emptyList())
            }
            override fun onFailure(call: Call<List<TransactionResponse>>, t: Throwable) {}
        })
    }

    private fun fetchSavedPaymentMethods() {
        api.getSavedPaymentMethods(userId).enqueue(object : Callback<List<SavedPaymentMethod>> {
            override fun onResponse(call: Call<List<SavedPaymentMethod>>, response: Response<List<SavedPaymentMethod>>) {
                if (response.isSuccessful) updateSavedMethodsUI(response.body() ?: emptyList())
            }
            override fun onFailure(call: Call<List<SavedPaymentMethod>>, t: Throwable) {
                // Fallback to legacy payment methods
                fetchLegacyPaymentMethods()
            }
        })
    }

    private fun fetchLegacyPaymentMethods() {
        api.listPaymentMethods(userId, role).enqueue(object : Callback<List<PaymentMethodResponse>> {
            override fun onResponse(call: Call<List<PaymentMethodResponse>>, response: Response<List<PaymentMethodResponse>>) {
                if (response.isSuccessful) updatePaymentMethodsUI(response.body() ?: emptyList())
            }
            override fun onFailure(call: Call<List<PaymentMethodResponse>>, t: Throwable) {}
        })
    }

    // ──────────────────────────────────────────────────────────────────
    // DOCTOR DATA
    // ──────────────────────────────────────────────────────────────────

    private fun fetchDoctorData() {
        api.getDoctorWallet(userId).enqueue(object : Callback<DoctorWalletResponse> {
            override fun onResponse(call: Call<DoctorWalletResponse>, response: Response<DoctorWalletResponse>) {
                if (response.isSuccessful) {
                    val w = response.body()
                    currentAvailableBalance = w?.availableBalance ?: 0.0
                    // Show available balance prominently; show summary below
                    tvWalletAmount.text = fmt.format(currentAvailableBalance)
                    tvWalletTitle.text  = "Available Balance"

                    // Append summary info to the title section
                    val summary = buildString {
                        append("Available Balance")
                        val pending   = w?.pendingBalance ?: 0.0
                        val total     = w?.totalEarned ?: 0.0
                        val paidOut   = w?.paidOutBalance ?: 0.0
                        if (pending > 0) append("\nPending: ${fmt.format(pending)}")
                        if (total > 0)   append(" | Earned: ${fmt.format(total)}")
                        if (paidOut > 0) append(" | Paid Out: ${fmt.format(paidOut)}")
                    }
                    tvWalletTitle.text = summary
                    btnAddPayment.isEnabled = currentAvailableBalance > 0
                }
            }
            override fun onFailure(call: Call<DoctorWalletResponse>, t: Throwable) {}
        })

        api.getDoctorTransactions(userId).enqueue(object : Callback<List<DoctorTransactionItem>> {
            override fun onResponse(call: Call<List<DoctorTransactionItem>>, response: Response<List<DoctorTransactionItem>>) {
                if (response.isSuccessful) updateDoctorTransactionsUI(response.body() ?: emptyList())
            }
            override fun onFailure(call: Call<List<DoctorTransactionItem>>, t: Throwable) {
                // Fallback to legacy
                api.getDoctorTransactionsV2(userId).enqueue(object : Callback<List<TransactionResponse>> {
                    override fun onResponse(call: Call<List<TransactionResponse>>, response: Response<List<TransactionResponse>>) {
                        if (response.isSuccessful) updateTransactionsUI(response.body() ?: emptyList())
                    }
                    override fun onFailure(call: Call<List<TransactionResponse>>, t: Throwable) {}
                })
            }
        })

        fetchDoctorBankAccounts()
    }

    private fun fetchDoctorBankAccounts() {
        api.getDoctorBankAccounts(userId).enqueue(object : Callback<List<DoctorBankAccount>> {
            override fun onResponse(call: Call<List<DoctorBankAccount>>, response: Response<List<DoctorBankAccount>>) {
                if (response.isSuccessful) doctorBankAccounts = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<DoctorBankAccount>>, t: Throwable) {}
        })
    }

    // ──────────────────────────────────────────────────────────────────
    // ADMIN DATA
    // ──────────────────────────────────────────────────────────────────

    private fun fetchAdminData() {
        ApiClient.instance.getAdminFinanceSummary().enqueue(object : Callback<AdminFinanceSummaryResponse> {
            override fun onResponse(call: Call<AdminFinanceSummaryResponse>, response: Response<AdminFinanceSummaryResponse>) {
                if (response.isSuccessful) {
                    val s = response.body()
                    // Show net platform earnings (from new backend field if available, else legacy)
                    val netEarnings = s?.netPlatformEarnings ?: s?.netProfit ?: 0.0
                    tvWalletAmount.text = fmt.format(netEarnings)
                    val summary = buildString {
                        val revenue    = s?.totalRevenueFull ?: s?.totalRevenue ?: 0.0
                        val commission = s?.totalCommission ?: s?.totalCommissions ?: 0.0
                        val refunded   = s?.totalRefundedAmount ?: 0.0
                        val pendingWd  = s?.pendingWithdrawalsCount ?: 0
                        append("Net Platform Earnings")
                        append("\nRevenue: ${fmt.format(revenue)} | Commission: ${fmt.format(commission)}")
                        if (refunded > 0) append("\nRefunded: ${fmt.format(refunded)}")
                        if (pendingWd > 0) append(" | Pending Withdrawals: $pendingWd")
                    }
                    tvWalletTitle.text = summary
                }
            }
            override fun onFailure(call: Call<AdminFinanceSummaryResponse>, t: Throwable) {}
        })

        // Try enriched payment list first
        api.getAdminFinancePayments("admin").enqueue(object : Callback<List<AdminPaymentItem>> {
            override fun onResponse(call: Call<List<AdminPaymentItem>>, response: Response<List<AdminPaymentItem>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    updateAdminPaymentsRich(response.body()!!)
                } else {
                    fetchAdminPaymentsLegacy()
                }
            }
            override fun onFailure(call: Call<List<AdminPaymentItem>>, t: Throwable) { fetchAdminPaymentsLegacy() }
        })
    }

    private fun fetchAdminPaymentsLegacy() {
        api.getAdminPaymentsV2().enqueue(object : Callback<List<TransactionResponse>> {
            override fun onResponse(call: Call<List<TransactionResponse>>, response: Response<List<TransactionResponse>>) {
                if (response.isSuccessful) adminAdapter?.updateData(response.body() ?: emptyList())
            }
            override fun onFailure(call: Call<List<TransactionResponse>>, t: Throwable) {}
        })
    }

    // ── UI UPDATE HELPERS ──────────────────────────────────────────────

    /** Patient: enriched billing history with doctor name, date (location-aware), status badge */
    private fun updatePatientTransactionsUI(items: List<BillingHistoryItem>) {
        llTransactionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        if (items.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No transactions yet"
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            tv.textSize = 14f
            tv.setPadding(0, 24, 0, 24)
            llTransactionsContainer.addView(tv)
            return
        }
        items.forEach { item ->
            val view = inflater.inflate(R.layout.item_transaction, llTransactionsContainer, false)

            // Doctor name as title
            view.findViewById<TextView>(R.id.tvTransactionTitle).text =
                "Dr. ${item.doctorName ?: "Doctor"}"

            // Location-based local time display
            val rawDate = item.paidAt ?: item.createdAt ?: item.appointmentDate ?: ""
            view.findViewById<TextView>(R.id.tvTransactionDate).text =
                TimeUtils.convertUtcToLocal(rawDate, outputPattern = "dd MMM yyyy, hh:mm a")
                    .ifEmpty { TimeUtils.formatSimpleDate(rawDate) }

            // Amount
            view.findViewById<TextView>(R.id.tvTransactionAmount).text =
                fmt.format(item.totalAmount)

            // Status badge
            val statusView = view.findViewById<TextView>(R.id.tvTransactionStatus)
            val status = item.paymentStatus?.lowercase() ?: "pending"
            statusView.text = when (status) {
                "success"  -> "Paid"
                "refunded" -> "Refunded"
                "failed"   -> "Failed"
                else       -> "Pending"
            }
            val (bgTint, textColor) = when (status) {
                "success"  -> "#D1FAE5" to "#10B981"
                "refunded" -> "#FEF3C7" to "#F59E0B"
                "failed"   -> "#FEE2E2" to "#EF4444"
                else       -> "#F1F5F9" to "#64748B"
            }
            statusView.setBackgroundColor(Color.parseColor(bgTint))
            statusView.setTextColor(Color.parseColor(textColor))

            // Invoice download → shows invoice detail dialog
            val llDownload = view.findViewById<LinearLayout>(R.id.llDownloadInvoice)
            llDownload.setOnClickListener {
                item.paymentId?.let { pid -> showInvoiceDetailDialog(pid) }
            }

            llTransactionsContainer.addView(view)
        }
    }

    /** Doctor: enriched transaction list with type, patient, amounts */
    private fun updateDoctorTransactionsUI(items: List<DoctorTransactionItem>) {
        llTransactionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        if (items.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No transactions yet"
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
            tv.textSize = 14f
            tv.setPadding(0, 24, 0, 24)
            llTransactionsContainer.addView(tv)
            return
        }
        items.forEach { item ->
            val view = inflater.inflate(R.layout.item_transaction, llTransactionsContainer, false)

            val typeLabel = when (item.transactionType) {
                "consultation_credit" -> "💰 Consultation"
                "refund_debit"        -> "🔄 Refund Debit"
                "withdrawal"          -> "🏦 Withdrawal"
                else -> item.transactionType ?: "Transaction"
            }
            view.findViewById<TextView>(R.id.tvTransactionTitle).text = typeLabel

            val rawDate = item.createdAt ?: item.appointmentDate ?: ""
            view.findViewById<TextView>(R.id.tvTransactionDate).text =
                TimeUtils.convertUtcToLocal(rawDate, outputPattern = "dd MMM yyyy, hh:mm a")
                    .ifEmpty { item.patientName ?: "" }

            view.findViewById<TextView>(R.id.tvTransactionAmount).text =
                fmt.format(item.netAmount ?: 0.0)

            val statusView = view.findViewById<TextView>(R.id.tvTransactionStatus)
            val status = item.status?.lowercase() ?: "pending"
            statusView.text = when (status) {
                "available", "paid" -> "Received"
                "reversed"          -> "Reversed"
                else                -> "Pending"
            }
            val (bgTint, textColor) = when (status) {
                "available", "paid" -> "#D1FAE5" to "#10B981"
                "reversed"          -> "#FEE2E2" to "#EF4444"
                else                -> "#FEF3C7" to "#F59E0B"
            }
            statusView.setBackgroundColor(Color.parseColor(bgTint))
            statusView.setTextColor(Color.parseColor(textColor))

            // Show commission info on long click
            view.setOnLongClickListener {
                val gross = fmt.format(item.grossAmount ?: 0.0)
                val comm  = fmt.format(item.platformCommission ?: 0.0)
                val net   = fmt.format(item.netAmount ?: 0.0)
                Toast.makeText(this, "Gross: $gross | Commission: $comm | Net: $net", Toast.LENGTH_LONG).show()
                true
            }

            // Hide invoice download for doctor transactions
            view.findViewById<LinearLayout>(R.id.llDownloadInvoice).visibility = View.GONE

            llTransactionsContainer.addView(view)
        }
    }

    /** Legacy TransactionResponse UI (fallback) */
    private fun updateTransactionsUI(history: List<TransactionResponse>) {
        llTransactionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        history.forEach { transaction ->
            val view = inflater.inflate(R.layout.item_transaction, llTransactionsContainer, false)
            view.findViewById<TextView>(R.id.tvTransactionTitle).text =
                if (role == "doctor") "Consultation Fee"
                else transaction.doctorName ?: transaction.status
            view.findViewById<TextView>(R.id.tvTransactionDate).text =
                TimeUtils.convertUtcToLocal(transaction.date, outputPattern = "dd MMM yyyy, hh:mm a")
                    .ifEmpty { transaction.date }
            view.findViewById<TextView>(R.id.tvTransactionAmount).text = fmt.format(transaction.amount)
            view.findViewById<TextView>(R.id.tvTransactionStatus).text = transaction.status
            llTransactionsContainer.addView(view)
        }
    }

    /** Admin: enriched payment list */
    private fun updateAdminPaymentsRich(items: List<AdminPaymentItem>) {
        // Convert to TransactionResponse for the existing adapter
        val txns = items.map { item ->
            TransactionResponse(
                id            = item.id,
                patientName   = item.patientName,
                doctorName    = item.doctorName,
                amount        = item.amount ?: 0.0,
                status        = item.status ?: "unknown",
                date          = TimeUtils.convertUtcToLocal(
                                    item.paidAt ?: item.createdAt ?: "",
                                    outputPattern = "dd MMM yyyy, hh:mm a"
                                ).ifEmpty { item.createdAt ?: "" },
                paymentMethod = item.paymentMethod
            )
        }
        adminAdapter?.updateData(txns)

        // Also show refund info in extra admin section
        loadAdminWithdrawals()
    }

    private fun loadAdminWithdrawals() {
        api.getAdminWithdrawals("admin", "pending").enqueue(object : Callback<List<AdminWithdrawalItem>> {
            override fun onResponse(call: Call<List<AdminWithdrawalItem>>, response: Response<List<AdminWithdrawalItem>>) {
                if (response.isSuccessful) {
                    val pending = response.body() ?: emptyList()
                    if (pending.isNotEmpty()) {
                        // Show pending withdrawals alert
                        Toast.makeText(
                            this@PaymentsActivity,
                            "⚠️ ${pending.size} withdrawal request(s) pending approval",
                            Toast.LENGTH_LONG
                        ).show()

                        // Show approve/reject option for first pending one
                        Handler(Looper.getMainLooper()).postDelayed({
                            showWithdrawalApprovalDialog(pending)
                        }, 1000)
                    }
                }
            }
            override fun onFailure(call: Call<List<AdminWithdrawalItem>>, t: Throwable) {}
        })
    }

    /** Patient: saved payment methods (new backend) */
    private fun updateSavedMethodsUI(methods: List<SavedPaymentMethod>) {
        if (role != "patient") return
        llPaymentMethodsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        methods.forEach { method ->
            val card = inflater.inflate(R.layout.item_payment_method, llPaymentMethodsContainer, false)
            val label = when (method.methodType?.lowercase()) {
                "card"       -> "${method.provider ?: "Card"} ${method.maskedDetails ?: ""} ${if (!method.expiry.isNullOrEmpty()) "Exp ${method.expiry}" else ""}"
                "upi"        -> "${method.provider ?: "UPI"} ${method.maskedDetails ?: ""}"
                "netbanking" -> "${method.provider ?: "Net Banking"} ${method.maskedDetails ?: ""}"
                else         -> method.maskedDetails ?: "Payment Method"
            }
            card.findViewById<TextView>(R.id.tvCardInfo).text = label.trim()
            card.setOnLongClickListener {
                confirmDeleteSavedMethod(method.id)
                true
            }
            llPaymentMethodsContainer.addView(card)
        }
    }

    /** Legacy PaymentMethodResponse UI */
    private fun updatePaymentMethodsUI(methods: List<PaymentMethodResponse>) {
        if (role != "patient") return
        llPaymentMethodsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        methods.forEach { method ->
            val card = inflater.inflate(R.layout.item_payment_method, llPaymentMethodsContainer, false)
            card.findViewById<TextView>(R.id.tvCardInfo).text = "${method.provider ?: "Card"} ${method.maskedDetails ?: ""}"
            llPaymentMethodsContainer.addView(card)
        }
    }

    // ── PATIENT: Add Money info + Add Payment Method ────────────────────

    private fun showAddMoneyInfo() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Pay via Razorpay")
            .setMessage("Payments are processed securely via Razorpay when you book a consultation with a doctor.\n\nComplete a booking and pay directly from the booking confirmation screen.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAddPaymentMethodMenu() {
        val types = arrayOf("Credit / Debit Card", "UPI", "Net Banking")
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Payment Method")
            .setItems(types) { _, which ->
                when (which) {
                    0 -> showAddCardDialog()
                    1 -> showAddUpiDialog()
                    2 -> showAddNetBankingDialog()
                }
            }
            .show()
    }

    private fun showAddCardDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment_method, null)
        val etCardNumber = dialogView.findViewById<EditText>(R.id.etCardNumber)
        val etCardType   = dialogView.findViewById<EditText>(R.id.etCardType)
        etCardType.hint  = "Provider (Mastercard, Visa...)"

        // Add expiry field dynamically if not in existing layout
        val etExpiry = EditText(this).apply {
            hint = "Expiry (MM/YY)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        (dialogView as? LinearLayout)?.addView(etExpiry)

        AlertDialog.Builder(this)
            .setTitle("Add Card")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val cardNum  = etCardNumber.text.toString().trim()
                val provider = etCardType.text.toString().trim()
                val expiry   = etExpiry.text.toString().trim()
                if (cardNum.length < 4 || provider.isEmpty()) {
                    Toast.makeText(this, "Enter valid card details", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val last4 = cardNum.takeLast(4)
                submitSavedPaymentMethod(
                    methodType    = "card",
                    provider      = provider,
                    maskedDetails = "•••• $last4",
                    expiry        = expiry.ifEmpty { null }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddUpiDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment_method, null)
        val etUpiId   = dialogView.findViewById<EditText>(R.id.etCardNumber)
        val etAppName = dialogView.findViewById<EditText>(R.id.etCardType)
        etUpiId.hint   = "UPI ID (e.g. john@oksbi)"
        etAppName.hint = "App (Google Pay, PhonePe...)"

        AlertDialog.Builder(this)
            .setTitle("Add UPI")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val upiId   = etUpiId.text.toString().trim()
                val appName = etAppName.text.toString().trim()
                if (upiId.isEmpty()) { Toast.makeText(this, "Enter UPI ID", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                submitSavedPaymentMethod(
                    methodType    = "upi",
                    provider      = appName.ifEmpty { "UPI" },
                    maskedDetails = upiId,
                    expiry        = null
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddNetBankingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment_method, null)
        val etBankName = dialogView.findViewById<EditText>(R.id.etCardNumber)
        val etAcct     = dialogView.findViewById<EditText>(R.id.etCardType)
        etBankName.hint = "Bank Name"
        etAcct.hint     = "Account last 4 digits"

        AlertDialog.Builder(this)
            .setTitle("Add Net Banking")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val bank = etBankName.text.toString().trim()
                val acct = etAcct.text.toString().trim()
                if (bank.isEmpty()) { Toast.makeText(this, "Enter bank name", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                submitSavedPaymentMethod(
                    methodType    = "netbanking",
                    provider      = bank,
                    maskedDetails = if (acct.isNotEmpty()) "•••• $acct" else bank,
                    expiry        = null
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitSavedPaymentMethod(methodType: String, provider: String?, maskedDetails: String?, expiry: String?) {
        val req = AddSavedPaymentMethodRequest(
            patientId     = userId,
            methodType    = methodType,
            provider      = provider,
            maskedDetails = maskedDetails,
            expiry        = expiry
        )
        api.addSavedPaymentMethod(req).enqueue(object : Callback<SavedPaymentMethod> {
            override fun onResponse(call: Call<SavedPaymentMethod>, response: Response<SavedPaymentMethod>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PaymentsActivity, "Payment method saved!", Toast.LENGTH_SHORT).show()
                    fetchSavedPaymentMethods()
                } else {
                    // Fallback to legacy endpoint
                    val body = mapOf("user_id" to userId, "role" to role, "card_last4" to (maskedDetails?.takeLast(4) ?: ""), "card_type" to (provider ?: ""))
                    api.addPaymentMethod(body).enqueue(object : Callback<GenericResponse> {
                        override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                            if (response.isSuccessful) { Toast.makeText(this@PaymentsActivity, "Method saved", Toast.LENGTH_SHORT).show(); fetchLegacyPaymentMethods() }
                        }
                        override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
                    })
                }
            }
            override fun onFailure(call: Call<SavedPaymentMethod>, t: Throwable) {}
        })
    }

    private fun confirmDeleteSavedMethod(methodId: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Payment Method")
            .setMessage("Remove this payment method?")
            .setPositiveButton("Remove") { _, _ ->
                api.deleteSavedPaymentMethod(methodId, DeletePaymentMethodRequest(userId))
                    .enqueue(object : Callback<GenericResponse> {
                        override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                            if (response.isSuccessful) { Toast.makeText(this@PaymentsActivity, "Removed", Toast.LENGTH_SHORT).show(); fetchSavedPaymentMethods() }
                        }
                        override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── PATIENT: Invoice Detail Dialog ──────────────────────────────────

    private fun showInvoiceDetailDialog(paymentId: Int) {
        api.getInvoiceDetail(paymentId).enqueue(object : Callback<InvoiceDetailResponse> {
            override fun onResponse(call: Call<InvoiceDetailResponse>, response: Response<InvoiceDetailResponse>) {
                val inv = response.body()
                if (!response.isSuccessful || inv == null || inv.error != null) {
                    Toast.makeText(this@PaymentsActivity, "Could not load invoice", Toast.LENGTH_SHORT).show()
                    return
                }
                val paidAtLocal = TimeUtils.convertUtcToLocal(inv.paidAt ?: "", outputPattern = "dd MMM yyyy, hh:mm a")
                val msg = buildString {
                    appendLine("🧾 Invoice: ${inv.invoiceNumber ?: "N/A"}")
                    appendLine("👨‍⚕️ Doctor: Dr. ${inv.doctorName ?: "N/A"}")
                    appendLine("📅 Appointment: ${TimeUtils.formatSimpleDate(inv.appointmentDate ?: "")}")
                    appendLine("─────────────────────────────")
                    appendLine("Consultation Fee : ${fmt.format(inv.consultationFee ?: 0.0)}")
                    appendLine("Platform Fee     : ${fmt.format(inv.platformFee ?: 0.0)}")
                    appendLine("Tax              : ${fmt.format(inv.taxAmount ?: 0.0)}")
                    appendLine("─────────────────────────────")
                    appendLine("Total Paid       : ${fmt.format(inv.totalAmount ?: 0.0)}")
                    appendLine("─────────────────────────────")
                    appendLine("Status: ${(inv.paymentStatus ?: "N/A").uppercase()}")
                    appendLine("Method: ${inv.paymentMethod ?: "Razorpay"}")
                    if (!inv.refundStatus.isNullOrEmpty()) {
                        appendLine("Refund: ${inv.refundStatus.uppercase()} – ${fmt.format(inv.refundAmount ?: 0.0)}")
                    }
                    appendLine("Paid: $paidAtLocal")
                }
                MaterialAlertDialogBuilder(this@PaymentsActivity)
                    .setTitle("Invoice Details")
                    .setMessage(msg)
                    .setPositiveButton("Close", null)
                    .show()
            }
            override fun onFailure(call: Call<InvoiceDetailResponse>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── RAZORPAY PAYMENT FLOW ───────────────────────────────────────────

    /**
     * Called from booking flow or can be triggered programmatically.
     * Creates a Razorpay order then opens the payment sheet.
     */
    private fun startRazorpayPayment(
        doctorId: Int,
        appointmentId: Int,
        amount: Double,
        doctorName: String,
        patientEmail: String,
        patientPhone: String
    ) {
        val request = CreateOrderRequest(
            patientId = userId,
            doctorId = doctorId,
            appointmentId = appointmentId,
            amount = amount,
            timezone = java.util.TimeZone.getDefault().id
        )

        api.createFullPaymentOrder(request).enqueue(object : Callback<CreateOrderResponse> {
            override fun onResponse(
                call: Call<CreateOrderResponse>,
                response: Response<CreateOrderResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val order = response.body()!!

                    if (!order.error.isNullOrBlank()) {
                        Toast.makeText(this@PaymentsActivity, order.error, Toast.LENGTH_LONG).show()
                        return
                    }

                    val resolvedOrderId = order.resolvedOrderId
                    val razorpayKey = order.resolvedKey
                    val amountPaise = order.amount ?: (amount * 100).toInt()

                    if (resolvedOrderId.isNullOrBlank() || razorpayKey.isNullOrBlank()) {
                        Toast.makeText(this@PaymentsActivity, "Invalid payment order received", Toast.LENGTH_SHORT).show()
                        return
                    }

                    pendingOrderId = resolvedOrderId
                    pendingPaymentDbId = order.paymentId
                    pendingAmount = order.amountInr ?: amount
                    pendingDoctorId = doctorId
                    pendingAppointmentId = appointmentId

                    openRazorpayCheckout(
                        orderId = resolvedOrderId,
                        razorpayKey = razorpayKey,
                        amountPaise = amountPaise,
                        doctorName = doctorName,
                        patientEmail = patientEmail,
                        patientPhone = patientPhone
                    )
                } else {
                    val errBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@PaymentsActivity, "Failed to create order: $errBody", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CreateOrderResponse>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun openRazorpayCheckout(
        orderId: String,
        razorpayKey: String,
        amountPaise: Int,
        doctorName: String,
        patientEmail: String,
        patientPhone: String
    ) {
        val checkout = Checkout()
        checkout.setKeyID(razorpayKey)

        try {
            val options = JSONObject()
            options.put("name", "TM App")
            options.put("description", "Consultation with Dr. $doctorName")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("order_id", orderId)
            options.put("theme.color", "#3399cc")
            options.put("currency", "INR")
            options.put("amount", amountPaise)

            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            val prefill = JSONObject()
            prefill.put("email", patientEmail)
            prefill.put("contact", patientPhone)
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    /** Razorpay success callback */
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: com.razorpay.PaymentData?) {
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId ?: ""
        val orderId = paymentData?.orderId ?: pendingOrderId ?: ""
        val signature = paymentData?.signature ?: ""

        if (paymentId.isBlank() || orderId.isBlank() || signature.isBlank()) {
            Toast.makeText(this, "Payment response incomplete", Toast.LENGTH_LONG).show()
            return
        }

        verifyPaymentOnServer(paymentId, orderId, signature)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: com.razorpay.PaymentData?) {
        Toast.makeText(
            this,
            "Payment was not completed. Your slot is reserved temporarily.",
            Toast.LENGTH_LONG
        ).show()

        val appointmentId = pendingAppointmentId ?: -1
        if (appointmentId != -1) {
            val intent = Intent(this, AppointmentDetailsActivity::class.java)
            intent.putExtra("appointment_id", appointmentId)
            startActivity(intent)
        }

        finish()
    }

    private fun clearPendingPayment() {
        pendingOrderId       = null
        pendingPaymentDbId   = null
        pendingAmount        = null
        pendingDoctorId      = null
        pendingAppointmentId = null
        pendingDate          = null
        pendingTime          = null
        pendingDoctorName    = null
    }

    private fun showPaymentSuccessDialog(body: VerifyPaymentResponse?) {
        val amountPaid = body?.amount ?: pendingAmount ?: 0.0
        val invoiceNo = body?.invoiceNumber ?: "N/A"
        val confirmedAppointmentId = pendingAppointmentId ?: body?.appointmentId ?: -1

        MaterialAlertDialogBuilder(this)
            .setTitle("✅ Payment Successful")
            .setMessage(
                "Amount Paid: ${fmt.format(amountPaid)}\n" +
                "Invoice: $invoiceNo\n\n" +
                "Your appointment is confirmed.\nThe doctor has been notified."
            )
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                clearPendingPayment()
                loadData()

                if (confirmedAppointmentId != -1) {
                    val intent = Intent(this, BookingConfirmedActivity::class.java)
                    intent.putExtra("appointment_id", confirmedAppointmentId)
                    intent.putExtra("payment_success", true)
                    startActivity(intent)
                }

                finish()
            }
            .show()
    }

    // ── DOCTOR: Withdraw + Bank Accounts ────────────────────────────────

    private fun showBankAccountsMenu() {
        val options = arrayOf("Add Bank Account", "Add UPI ID", "View Accounts")
        MaterialAlertDialogBuilder(this)
            .setTitle("Payout Accounts")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddBankAccountDialog()
                    1 -> showAddUpiIdDialog()
                    2 -> showViewAccountsDialog()
                }
            }
            .show()
    }

    private fun showWithdrawDialog() {
        if (doctorBankAccounts.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("No Payout Account")
                .setMessage("Add a bank account or UPI ID first to withdraw.")
                .setPositiveButton("Add Account") { _, _ -> showBankAccountsMenu() }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_money, null)
        val etAmount   = dialogView.findViewById<EditText>(R.id.etAmount)
        etAmount.hint  = "Amount (max: ${fmt.format(currentAvailableBalance)})"

        val acctLabels = doctorBankAccounts.map { acct ->
            when (acct.accountType) {
                "bank" -> "${acct.bankName ?: "Bank"} – ${acct.accountNumber ?: ""}"
                "upi"  -> "UPI: ${acct.upiId ?: ""}"
                else   -> "Account ${acct.id}"
            }
        }
        var selectedAcctIdx = 0
        val spinnerContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, acctLabels)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { selectedAcctIdx = position }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        spinnerContainer.addView(dialogView)
        spinnerContainer.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle("Withdraw Earnings")
            .setView(spinnerContainer)
            .setPositiveButton("Request") { _, _ ->
                val amt = etAmount.text.toString().toDoubleOrNull()
                if (amt == null || amt <= 0) { Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                if (amt > currentAvailableBalance) { Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val selectedAcct = doctorBankAccounts.getOrNull(selectedAcctIdx)
                submitWithdrawal(amt, selectedAcct?.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitWithdrawal(amount: Double, bankAccountId: Int?) {
        api.requestWithdrawal(WithdrawalRequestBody(userId, amount, bankAccountId))
            .enqueue(object : Callback<WithdrawalResponse> {
                override fun onResponse(call: Call<WithdrawalResponse>, response: Response<WithdrawalResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.error == null) {
                        MaterialAlertDialogBuilder(this@PaymentsActivity)
                            .setTitle("✅ Withdrawal Requested")
                            .setMessage(
                                "Amount: ${fmt.format(body?.amount ?: amount)}\n" +
                                "Status: Pending Admin Approval\n" +
                                "Remaining: ${fmt.format(body?.availableBalance ?: 0.0)}"
                            )
                            .setPositiveButton("OK", null)
                            .show()
                        fetchDoctorData()
                    } else {
                        Toast.makeText(this@PaymentsActivity, body?.error ?: "Withdrawal failed", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<WithdrawalResponse>, t: Throwable) {
                    Toast.makeText(this@PaymentsActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showAddBankAccountDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment_method, null)
        val etHolder   = dialogView.findViewById<EditText>(R.id.etCardNumber)
        val etBankName = dialogView.findViewById<EditText>(R.id.etCardType)
        etHolder.hint   = "Account Holder Name"
        etBankName.hint = "Bank Name"

        // Add account number and IFSC dynamically
        val etAcctNum = EditText(this).apply { hint = "Account Number"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etIfsc    = EditText(this).apply { hint = "IFSC Code"; inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS }
        val container = dialogView as? LinearLayout
        container?.addView(etAcctNum)
        container?.addView(etIfsc)

        AlertDialog.Builder(this)
            .setTitle("Add Bank Account")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val holder  = etHolder.text.toString().trim()
                val bank    = etBankName.text.toString().trim()
                val acctNum = etAcctNum.text.toString().trim()
                val ifsc    = etIfsc.text.toString().trim().uppercase()
                if (bank.isEmpty() || acctNum.isEmpty()) { Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val req = AddBankAccountRequest(
                    doctorId          = userId,
                    accountType       = "bank",
                    accountHolderName = holder,
                    bankName          = bank,
                    accountNumber     = acctNum,
                    ifscCode          = ifsc,
                    upiId             = null,
                    setDefault        = true
                )
                submitBankAccount(req)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddUpiIdDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment_method, null)
        val etUpiId   = dialogView.findViewById<EditText>(R.id.etCardNumber)
        val etAppName = dialogView.findViewById<EditText>(R.id.etCardType)
        etUpiId.hint   = "UPI ID (e.g. doctor@oksbi)"
        etAppName.hint = "App (Google Pay, PhonePe...)"

        AlertDialog.Builder(this)
            .setTitle("Add UPI ID")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val upiId = etUpiId.text.toString().trim()
                if (upiId.isEmpty()) { Toast.makeText(this, "Enter UPI ID", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val req = AddBankAccountRequest(
                    doctorId          = userId,
                    accountType       = "upi",
                    accountHolderName = null,
                    bankName          = null,
                    accountNumber     = null,
                    ifscCode          = null,
                    upiId             = upiId,
                    setDefault        = true
                )
                submitBankAccount(req)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitBankAccount(req: AddBankAccountRequest) {
        api.addDoctorBankAccount(req).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PaymentsActivity, "Account added!", Toast.LENGTH_SHORT).show()
                    fetchDoctorBankAccounts()
                } else {
                    Toast.makeText(this@PaymentsActivity, "Failed to add account", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
        })
    }

    private fun showViewAccountsDialog() {
        if (doctorBankAccounts.isEmpty()) { Toast.makeText(this, "No accounts saved yet.", Toast.LENGTH_SHORT).show(); return }
        val labels = doctorBankAccounts.map { acct ->
            when (acct.accountType) {
                "bank" -> "${acct.bankName ?: "Bank"} – ${acct.accountNumber ?: ""} ${if (acct.isDefault) "(Default)" else ""}"
                "upi"  -> "UPI: ${acct.upiId ?: ""} ${if (acct.isDefault) "(Default)" else ""}"
                else   -> "Account ${acct.id}"
            }
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Payout Accounts")
            .setItems(labels) { _, which ->
                val acct = doctorBankAccounts[which]
                MaterialAlertDialogBuilder(this)
                    .setTitle("Remove Account?")
                    .setMessage(labels[which])
                    .setPositiveButton("Remove") { _, _ ->
                        api.deleteDoctorBankAccount(acct.id, DeleteBankAccountRequest(userId))
                            .enqueue(object : Callback<GenericResponse> {
                                override fun onResponse(call: Call<GenericResponse>, r: Response<GenericResponse>) {
                                    if (r.isSuccessful) { Toast.makeText(this@PaymentsActivity, "Removed", Toast.LENGTH_SHORT).show(); fetchDoctorBankAccounts() }
                                }
                                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {}
                            })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .show()
    }

    // ── ADMIN: Withdrawal Approval UI ──────────────────────────────────

    private fun showWithdrawalApprovalDialog(pendingList: List<AdminWithdrawalItem>) {
        val items = pendingList.take(5)   // show up to 5
        val labels = items.map { w ->
            "Dr. ${w.doctorName ?: "Unknown"} – ${fmt.format(w.amount ?: 0.0)}\n" +
            "Requested: ${TimeUtils.convertUtcToLocal(w.createdAt ?: "", outputPattern = "dd MMM yyyy")}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("⏳ Pending Withdrawal Requests")
            .setItems(labels) { _, which ->
                val item = items[which]
                showWithdrawalActionDialog(item)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showWithdrawalActionDialog(item: AdminWithdrawalItem) {
        val bankInfo = item.bankAccount?.let { b ->
            when (b.type) {
                "bank" -> "${b.bankName ?: "Bank"} – ${b.accountNumber ?: ""}"
                "upi"  -> "UPI: ${b.upiId ?: ""}"
                else   -> "N/A"
            }
        } ?: "No account"

        MaterialAlertDialogBuilder(this)
            .setTitle("Withdrawal: ${fmt.format(item.amount ?: 0.0)}")
            .setMessage("Doctor: Dr. ${item.doctorName ?: "Unknown"}\nAccount: $bankInfo")
            .setPositiveButton("✅ Approve") { _, _ ->
                processWithdrawalAction(item.id, true, "Approved by admin")
            }
            .setNegativeButton("❌ Reject") { _, _ ->
                processWithdrawalAction(item.id, false, "Rejected by admin")
            }
            .setNeutralButton("Later", null)
            .show()
    }

    private fun processWithdrawalAction(withdrawalId: Int, approve: Boolean, note: String) {
        val req  = AdminWithdrawalActionRequest(userId, note)
        val call = if (approve) api.approveWithdrawal(withdrawalId, req) else api.rejectWithdrawal(withdrawalId, req)

        call.enqueue(object : Callback<AdminWithdrawalActionResponse> {
            override fun onResponse(call: Call<AdminWithdrawalActionResponse>, response: Response<AdminWithdrawalActionResponse>) {
                val body = response.body()
                if (response.isSuccessful && body?.error == null) {
                    val label = if (approve) "✅ Approved" else "❌ Rejected"
                    Toast.makeText(this@PaymentsActivity, "$label – ${fmt.format(body?.amount ?: 0.0)}", Toast.LENGTH_LONG).show()
                    fetchAdminData()
                } else {
                    Toast.makeText(this@PaymentsActivity, body?.error ?: "Action failed", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<AdminWithdrawalActionResponse>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
