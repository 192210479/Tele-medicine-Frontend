package com.simats.Tmapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.simats.Tmapp.api.AppointmentResponse
import com.simats.Tmapp.api.DashboardResponse
import com.simats.Tmapp.api.ProfileResponse
import java.time.LocalDate

class DoctorDashboardViewModel : ViewModel() {
    private val repository = DoctorDashboardRepository()

    private val _allAppointments = MutableLiveData<List<AppointmentResponse>>()
    val allAppointments: LiveData<List<AppointmentResponse>> get() = _allAppointments

    private val _upcomingAppointments = MutableLiveData<List<AppointmentResponse>>()
    val upcomingAppointments: LiveData<List<AppointmentResponse>> get() = _upcomingAppointments

    private val _todayAppointments = MutableLiveData<List<AppointmentResponse>>()
    val todayAppointments: LiveData<List<AppointmentResponse>> get() = _todayAppointments

    private val _availabilitySlots = MutableLiveData<List<com.simats.Tmapp.api.AvailabilityResponse>>()
    val availabilitySlots: LiveData<List<com.simats.Tmapp.api.AvailabilityResponse>> get() = _availabilitySlots

    private val _doctorProfile = MutableLiveData<ProfileResponse?>()
    val doctorProfile: LiveData<ProfileResponse?> get() = _doctorProfile

    private val _dashboardData = MutableLiveData<DashboardResponse?>()
    val dashboardData: LiveData<DashboardResponse?> get() = _dashboardData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadDashboardData(userId: Int) {
        fetchDashboardGreeting(userId)
        fetchAppointments(userId, "doctor")
        fetchProfile(userId)
    }

    private fun fetchDashboardGreeting(userId: Int) {
        com.simats.Tmapp.api.ApiClient.instance.getDashboardData(userId, "doctor")
            .enqueue(object : retrofit2.Callback<DashboardResponse> {
                override fun onResponse(call: retrofit2.Call<DashboardResponse>, response: retrofit2.Response<DashboardResponse>) {
                    if (response.isSuccessful) {
                        _dashboardData.postValue(response.body())
                    }
                }
                override fun onFailure(call: retrofit2.Call<DashboardResponse>, t: Throwable) {
                    // Fail silently, fallback in UI
                }
            })
    }

    fun fetchAppointments(userId: Int, role: String) {
        repository.getDoctorAppointments(userId, role) { appointments, errorMessage ->
            if (appointments != null) {
                _allAppointments.postValue(appointments)
                processAppointments(appointments)
            } else {
                _error.postValue(errorMessage)
            }
        }
    }

    private fun processAppointments(appointments: List<AppointmentResponse>) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val now = java.util.Date()

        // TODAY's dashboard appointments:
        // - only today's appointments
        // - exclude cancelled/completed/missed
        // - only current or future time for today
        // - sorted ascending
        // - limited to next 3
        val todayAppts = appointments.filter { appt ->
            val status = DoctorAppointmentHelper.getStatus(appt)
            val apptDateTime = DoctorAppointmentHelper.parseDateTime(appt.date, appt.localTime)

            appt.date == todayStr &&
            status != DoctorAppointmentHelper.AppointmentStatus.CANCELLED &&
            status != DoctorAppointmentHelper.AppointmentStatus.COMPLETED &&
            status != DoctorAppointmentHelper.AppointmentStatus.MISSED &&
            (apptDateTime == null || !apptDateTime.before(now))
        }.sortedWith(compareBy { appt ->
            DoctorAppointmentHelper.parseDateTime(appt.date, appt.localTime)
        })

        // Keep this populated so other parts of dashboard logic still work safely
        _upcomingAppointments.postValue(emptyList())

        // Dashboard shows only next 3
        _todayAppointments.postValue(todayAppts.take(3))

        // Still keep full source list for next-appointment card / other logic
        _allAppointments.postValue(appointments)
    }

    fun fetchProfile(userId: Int) {
        com.simats.Tmapp.api.ApiClient.instance.getProfile(userId, "doctor")
            .enqueue(object : retrofit2.Callback<ProfileResponse> {
                override fun onResponse(call: retrofit2.Call<ProfileResponse>, response: retrofit2.Response<ProfileResponse>) {
                    if (response.isSuccessful) {
                        _doctorProfile.postValue(response.body())
                    }
                }
                override fun onFailure(call: retrofit2.Call<ProfileResponse>, t: Throwable) {
                    _error.postValue(t.message)
                }
            })
    }

    fun fetchSlots(doctorId: Int) {
        repository.getDoctorSlots(doctorId) { slots, errorMessage ->
            if (slots != null) {
                val today = LocalDate.now()
                val filteredSlots = slots.filter { slot ->
                    val slotDate = try { LocalDate.parse(slot.date) } catch (e: Exception) { null }
                    slotDate != null && (slotDate.isEqual(today) || slotDate.isAfter(today))
                }
                _availabilitySlots.postValue(filteredSlots)
            } else {
                _error.postValue(errorMessage)
            }
        }
    }

    fun deleteSlot(slotId: Int, userId: Int, role: String) {
        repository.deleteSlot(slotId, userId, role) { success, errorMessage ->
            if (success) {
                fetchSlots(userId)
            } else {
                _error.postValue(errorMessage)
            }
        }
    }

    fun cleanupSlots(userId: Int, role: String) {
        repository.cleanupSlots(userId, role)
    }
}
