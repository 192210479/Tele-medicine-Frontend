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
import com.simats.Tmapp.api.Doctor

class AdminDoctorsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AdminDashboardViewModel

    private var rvDoctors: RecyclerView? = null
    private var llEmptyState: LinearLayout? = null
    private var doctorAdapter: DoctorListAdapter? = null
    private var ivBack: ImageView? = null
    private var etSearch: EditText? = null

    // Filter buttons
    private var filterAll: Button? = null
    private var filterActive: Button? = null
    private var filterPending: Button? = null
    private var filterRejected: Button? = null

    // Data holders
    private var fullDoctorList: List<Doctor> = emptyList()
    private var filteredDoctorList: List<Doctor> = emptyList()
    private var displayedDoctorList: MutableList<Doctor> = mutableListOf()

    // Search / filter / pagination
    private var currentSearchQuery: String = ""
    private var currentFilter: String = "all"
    private var currentPage = 1
    private val pageSize = 10
    private var isLoadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_doctors)

        sessionManager = SessionManager.getInstance(this)
        viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        setupFilters()
        setupObservers()

        viewModel.loadDoctors()
    }

    private fun initializeViews() {
        rvDoctors = findViewById(R.id.rvDoctors)
        llEmptyState = findViewById(R.id.llEmptyState)
        ivBack = findViewById(R.id.ivBack)
        etSearch = findViewById(R.id.etSearch)

        filterAll = findViewById(R.id.filterAll)
        filterActive = findViewById(R.id.filterActive)
        filterPending = findViewById(R.id.filterPending)
        filterRejected = findViewById(R.id.filterRejected)
    }

    private fun setupRecyclerView() {
        rvDoctors?.layoutManager = LinearLayoutManager(this)
        rvDoctors?.setHasFixedSize(false)

        doctorAdapter = DoctorListAdapter(emptyList()) { doctor, action ->
            when (action) {
                "click" -> {
                    // Future: open doctor profile/details
                }
                "menu" -> {
                    // Future: popup actions
                }
            }
        }

        rvDoctors?.adapter = doctorAdapter

        rvDoctors?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                    loadMoreDoctors()
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

        filterActive?.setOnClickListener {
            currentFilter = "available"
            resetAndApplyFilters()
        }

        filterPending?.setOnClickListener {
            currentFilter = "unavailable"
            resetAndApplyFilters()
        }

        filterRejected?.setOnClickListener {
            currentFilter = "incomplete"
            resetAndApplyFilters()
        }
    }

    private fun setupObservers() {
        viewModel.allDoctors.observe(this) { doctors ->
            val originalList = doctors ?: emptyList()

            fullDoctorList = originalList.map { doctor ->

                val isIncomplete =
                    doctor.name.isNullOrBlank() ||
                    doctor.specialization.isNullOrBlank() ||
                    doctor.experience == null

                val normalizedStatus = doctor.status?.lowercase()?.trim() ?: ""
                val totalAppointments = doctor.totalAppointments ?: 0
                val todayAppointments = doctor.todayAppointments ?: 0

                val derivedStatus = when {
                    isIncomplete -> "incomplete"

                    normalizedStatus == "inactive" ||
                            normalizedStatus == "disabled" ||
                            normalizedStatus == "unavailable" -> "unavailable"

                    normalizedStatus == "approved" ||
                            normalizedStatus == "active" -> {
                        if (totalAppointments == 0 && todayAppointments == 0) "available"
                        else "available"
                    }

                    else -> "available"
                }

                doctor.copy(status = derivedStatus)
            }

            resetAndApplyFilters()
        }
    }

    private fun resetAndApplyFilters() {
        currentPage = 1
        displayedDoctorList.clear()
        applyFiltersAndPagination()
    }

    private fun applyFiltersAndPagination() {
        filteredDoctorList = fullDoctorList.filter { doctor ->

            val matchesSearch =
                doctor.name?.contains(currentSearchQuery, ignoreCase = true) == true ||
                doctor.specialization?.contains(currentSearchQuery, ignoreCase = true) == true ||
                doctor.email?.contains(currentSearchQuery, ignoreCase = true) == true ||
                doctor.phone?.contains(currentSearchQuery, ignoreCase = true) == true

            val status = doctor.status?.lowercase()?.trim() ?: ""

            val matchesFilter = when (currentFilter) {
                "available" -> status == "approved" || status == "active"
                "unavailable" -> status == "inactive" || status == "disabled"
                "incomplete" -> status.isBlank()
                else -> true
            }

            matchesSearch && matchesFilter
        }

        renderPage()
    }

    private fun renderPage() {
        val endIndex = minOf(currentPage * pageSize, filteredDoctorList.size)

        displayedDoctorList = filteredDoctorList.take(endIndex).toMutableList()

        if (displayedDoctorList.isEmpty()) {
            rvDoctors?.visibility = View.GONE
            llEmptyState?.visibility = View.VISIBLE
        } else {
            rvDoctors?.visibility = View.VISIBLE
            llEmptyState?.visibility = View.GONE
            doctorAdapter?.updateData(displayedDoctorList)
        }
    }

    private fun loadMoreDoctors() {
        if (isLoadingMore) return
        if (displayedDoctorList.size >= filteredDoctorList.size) return

        isLoadingMore = true
        currentPage++
        renderPage()
        isLoadingMore = false
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDoctors()
    }
}