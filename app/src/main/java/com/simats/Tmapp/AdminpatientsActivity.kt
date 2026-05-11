package com.simats.Tmapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminPatientsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AdminDashboardViewModel

    private var rvPatients: RecyclerView? = null
    private var llEmptyState: LinearLayout? = null
    private var patientAdapter: PatientListAdapter? = null
    private var ivBack: ImageView? = null
    private var etSearch: EditText? = null

    // Filter buttons
    private var filterAll: Button? = null
    private var filterRecent: Button? = null
    private var filterActive: Button? = null

    // Data holders
    private var fullPatientList: List<Patient> = emptyList()
    private var filteredPatientList: List<Patient> = emptyList()
    private var displayedPatientList: MutableList<Patient> = mutableListOf()

    // Search / filter / pagination
    private var currentSearchQuery: String = ""
    private var currentFilter: String = "all"
    private var currentPage = 1
    private val pageSize = 10
    private var isLoadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_patients)

        sessionManager = SessionManager.getInstance(this)
        viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        setupFilters()
        setupObservers()

        viewModel.loadPatients()
    }

    private fun initializeViews() {
        rvPatients = findViewById(R.id.rvPatients)
        llEmptyState = findViewById(R.id.llEmptyState)
        ivBack = findViewById(R.id.ivBack)
        etSearch = findViewById(R.id.etSearch)

        filterAll = findViewById(R.id.filterAll)
        filterRecent = findViewById(R.id.filterRecent)
        filterActive = findViewById(R.id.filterActive)
    }

    private fun setupRecyclerView() {
        rvPatients?.layoutManager = LinearLayoutManager(this)
        rvPatients?.setHasFixedSize(false)

        patientAdapter = PatientListAdapter(emptyList()) { patient, action ->
            when (action) {
                "click" -> {
                    // Future: open patient profile/details
                }
                "menu" -> {
                    // Future: popup actions
                }
            }
        }

        rvPatients?.adapter = patientAdapter

        rvPatients?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                    loadMorePatients()
                }
            }
        })
    }

    private fun setupClickListeners() {
        ivBack?.setOnClickListener { finish() }
    }

    private fun setupSearch() {
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                resetAndApplyFilters()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFilters() {
        filterAll?.setOnClickListener {
            currentFilter = "all"
            resetAndApplyFilters()
        }

        filterRecent?.setOnClickListener {
            currentFilter = "recent"
            resetAndApplyFilters()
        }

        filterActive?.setOnClickListener {
            currentFilter = "active"
            resetAndApplyFilters()
        }
    }

    private fun setupObservers() {
        viewModel.allPatients.observe(this) { patients ->
            fullPatientList = patients ?: emptyList()
            resetAndApplyFilters()
        }
    }

    private fun resetAndApplyFilters() {
        currentPage = 1
        displayedPatientList.clear()
        applyFiltersAndPagination()
    }

    private fun applyFiltersAndPagination() {
        filteredPatientList = fullPatientList.filter { patient ->

            // Search matching
            val matchesSearch =
                patient.fullName.contains(currentSearchQuery, ignoreCase = true) ||
                (patient.email?.contains(currentSearchQuery, ignoreCase = true) == true) ||
                (patient.phone?.contains(currentSearchQuery, ignoreCase = true) == true)

            // Derived logical states
            val isNew = isPatientNew(patient)
            val isActive = isPatientActive(patient)

            val matchesFilter = when (currentFilter) {
                "recent" -> isNew
                "active" -> isActive
                else -> true
            }

            matchesSearch && matchesFilter
        }

        renderPage()
    }

    private fun renderPage() {
        val endIndex = minOf(currentPage * pageSize, filteredPatientList.size)

        displayedPatientList = filteredPatientList.take(endIndex).toMutableList()

        if (displayedPatientList.isEmpty()) {
            rvPatients?.visibility = View.GONE
            llEmptyState?.visibility = View.VISIBLE
        } else {
            rvPatients?.visibility = View.VISIBLE
            llEmptyState?.visibility = View.GONE
            patientAdapter?.updateData(displayedPatientList)
        }
    }

    private fun loadMorePatients() {
        if (isLoadingMore) return
        if (displayedPatientList.size >= filteredPatientList.size) return

        isLoadingMore = true
        currentPage++
        renderPage()
        isLoadingMore = false
    }

    /**
     * PATIENT "NEW" RULE
     * Since backend does not provide createdAt, we use a safe fallback:
     *
     * NEW = no last appointment AND no records
     *
     * This is the cleanest possible approximation with your current model.
     */
    private fun isPatientNew(patient: Patient): Boolean {
        val backendStatus = patient.status?.trim()?.lowercase()
        if (backendStatus == "new") return true

        val hasAppointment = !patient.lastAppointment.isNullOrBlank()
        val totalRecords = patient.totalRecords ?: 0
        return !hasAppointment && totalRecords == 0
    }

    /**
     * PATIENT "ACTIVE" RULE
     * ACTIVE = has last appointment OR has records
     *
     * This is your current best meaningful admin logic.
     */
    private fun isPatientActive(patient: Patient): Boolean {
        val backendStatus = patient.status?.trim()?.lowercase()
        if (backendStatus == "active") return true
        if (backendStatus == "inactive") return false

        val hasAppointment = !patient.lastAppointment.isNullOrBlank()
        val totalRecords = patient.totalRecords ?: 0
        return hasAppointment || totalRecords > 0
    }

    /**
     * Optional utility for future use if backend later sends proper dates.
     * Keeping it here makes this file future-ready.
     */
    private fun isWithinDays(dateString: String?, days: Int): Boolean {
        if (dateString.isNullOrBlank()) return false

        val possibleFormats = listOf(
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )

        for (format in possibleFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val parsedDate: Date = sdf.parse(dateString) ?: continue
                val diffMillis = System.currentTimeMillis() - parsedDate.time
                val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
                return diffDays <= days
            } catch (_: Exception) {
            }
        }

        return false
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPatients()
    }
}