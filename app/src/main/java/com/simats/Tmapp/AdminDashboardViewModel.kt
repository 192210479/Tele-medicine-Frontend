package com.simats.Tmapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.simats.Tmapp.api.DashboardSummary
import com.simats.Tmapp.api.RevenueTrend
import com.simats.Tmapp.api.WeeklyAppointments
import com.simats.Tmapp.api.Doctor
import com.simats.Tmapp.api.Patient
import com.simats.Tmapp.api.Appointment
import com.simats.Tmapp.api.DoctorActivity
import com.simats.Tmapp.api.PatientRegistration

class AdminDashboardViewModel : ViewModel() {
    private val repository = AdminRepository()

    private val _summary = MutableLiveData<DashboardSummary?>()
    val summary: LiveData<DashboardSummary?> = _summary

    private val _revenueTrend = MutableLiveData<List<RevenueTrend>?>()
    val revenueTrend: LiveData<List<RevenueTrend>?> = _revenueTrend

    private val _weeklyAppointments = MutableLiveData<List<WeeklyAppointments>?>()
    val weeklyAppointments: LiveData<List<WeeklyAppointments>?> = _weeklyAppointments

    private val _pendingDoctors = MutableLiveData<List<Doctor>?>()
    val pendingDoctors: LiveData<List<Doctor>?> = _pendingDoctors

    private val _allDoctors = MutableLiveData<List<Doctor>?>()
    val allDoctors: LiveData<List<Doctor>?> = _allDoctors

    private val _allPatients = MutableLiveData<List<Patient>?>()
    val allPatients: LiveData<List<Patient>?> = _allPatients

    private val _allAppointments = MutableLiveData<List<Appointment>?>()
    val allAppointments: LiveData<List<Appointment>?> = _allAppointments

    private val _doctorActivity = MutableLiveData<List<DoctorActivity>?>()
    val doctorActivity: LiveData<List<DoctorActivity>?> = _doctorActivity

    private val _patientRegistrations = MutableLiveData<List<PatientRegistration>?>()
    val patientRegistrations: LiveData<List<PatientRegistration>?> = _patientRegistrations

    private val _patientsGrowth = MutableLiveData<String>()
    val patientsGrowth: LiveData<String> = _patientsGrowth

    private val _revenueGrowth = MutableLiveData<String>()
    val revenueGrowth: LiveData<String> = _revenueGrowth

    private val _newDoctorsCount = MutableLiveData<String>()
    val newDoctorsCount: LiveData<String> = _newDoctorsCount

    private val _todayAppointmentsCount = MutableLiveData<String>()
    val todayAppointmentsCount: LiveData<String> = _todayAppointmentsCount

    fun loadDashboardData() {
        repository.getDashboardSummary { summary ->
            _summary.postValue(summary)

            summary?.let {
                _patientsGrowth.postValue(
                    "${if (it.patients_growth_percent >= 0) "+" else ""}${it.patients_growth_percent}%"
                )

                _revenueGrowth.postValue(
                    "${if (it.revenue_growth_percent >= 0) "+" else ""}${it.revenue_growth_percent}%"
                )

                _newDoctorsCount.postValue("${it.new_doctors_count} New")
                _todayAppointmentsCount.postValue("${it.today_appointments} Today")
            }
        }

        repository.getRevenueTrend { trend ->
            _revenueTrend.postValue(trend)
        }

        repository.getWeeklyAppointments { weekly ->
            _weeklyAppointments.postValue(weekly)
        }

        repository.getPendingDoctors { _pendingDoctors.postValue(it) }

        repository.getDoctorActivity { activity ->
            _doctorActivity.postValue(activity)
        }

        repository.getPatientRegistrations { registrations ->
            _patientRegistrations.postValue(registrations)
        }
    }

    fun loadDoctors() {
        repository.getAllDoctors { _allDoctors.postValue(it) }
    }

    fun loadPatients() {
        repository.getAllPatients { _allPatients.postValue(it) }
    }

    fun loadAppointments(adminId: Int) {
        repository.getMyAppointmentsForAdmin(adminId) { _allAppointments.postValue(it) }
    }

    fun approveDoctor(doctorId: Int, callback: (Boolean, String?) -> Unit) {
        repository.approveDoctor(doctorId) { success, message ->
            if (success) {
                loadDashboardData()
                loadDoctors()
            }
            callback(success, message)
        }
    }

    fun rejectDoctor(doctorId: Int, callback: (Boolean, String?) -> Unit) {
        repository.rejectDoctor(doctorId) { success, message ->
            if (success) {
                loadDashboardData()
                loadDoctors()
            }
            callback(success, message)
        }
    }

    fun reassignAppointment(id: Int, doctorId: Int, date: String, time: String, adminId: Int, callback: (Boolean, String?) -> Unit) {
        val body = mapOf(
            "role" to "admin",
            "doctor_id" to doctorId,
            "date" to date,
            "time" to time
        )
        repository.reassignAppointment(id, body) { success, message ->
            if (success) {
                loadAppointments(adminId)
                loadDashboardData()
            }
            callback(success, message)
        }
    }

    fun cancelAppointment(id: Int, adminId: Int, callback: (Boolean, String?) -> Unit) {
        repository.cancelAppointment(id) { success, message ->
            if (success) {
                loadAppointments(adminId)
                loadDashboardData()
            }
            callback(success, message)
        }
    }
}
