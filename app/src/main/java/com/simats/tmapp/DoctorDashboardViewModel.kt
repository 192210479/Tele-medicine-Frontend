package com.simats.tmapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.simats.tmapp.api.AppointmentResponse
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

class DoctorDashboardViewModel : ViewModel() {
    private val repository = DoctorDashboardRepository()

    private val _allAppointments = MutableLiveData<List<AppointmentResponse>>()
    val allAppointments: LiveData<List<AppointmentResponse>> get() = _allAppointments

    private val _upcomingAppointments = MutableLiveData<List<AppointmentResponse>>()
    val upcomingAppointments: LiveData<List<AppointmentResponse>> get() = _upcomingAppointments

    private val _todayAppointments = MutableLiveData<List<AppointmentResponse>>()
    val todayAppointments: LiveData<List<AppointmentResponse>> get() = _todayAppointments

    private val _availabilitySlots = MutableLiveData<List<com.simats.tmapp.api.AvailabilityResponse>>()
    val availabilitySlots: LiveData<List<com.simats.tmapp.api.AvailabilityResponse>> get() = _availabilitySlots

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

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
        val today = LocalDate.now()
        
        // Upcoming Appointments: date >= today and strictly NOT completed
        val upcoming = appointments.filter { appt ->
            val isCompleted = appt.status.equals("Completed", true) || appt.consultationStatus.equals("Completed", true)
            val apptDate = appt.date?.let { try { LocalDate.parse(it) } catch (e: Exception) { null } }
            !isCompleted && apptDate != null && (apptDate.isAfter(today) || apptDate.isEqual(today))
        }.sortedWith { a, b ->
            val d1 = a.date?.let { try { LocalDate.parse(it) } catch (e: Exception) { LocalDate.MIN } } ?: LocalDate.MIN
            val d2 = b.date?.let { try { LocalDate.parse(it) } catch (e: Exception) { LocalDate.MIN } } ?: LocalDate.MIN
            val res = d1.compareTo(d2)
            if (res != 0) res 
            else {
                val t1 = a.time?.let { try { LocalTime.parse(it) } catch (e: Exception) { LocalTime.MIN } } ?: LocalTime.MIN
                val t2 = b.time?.let { try { LocalTime.parse(it) } catch (e: Exception) { LocalTime.MIN } } ?: LocalTime.MIN
                t1.compareTo(t2)
            }
        }

        // Today's Schedule: appointment_date == today and strictly NOT completed
        val todayAppts = appointments.filter { appt ->
            val isCompleted = appt.status.equals("Completed", true) || appt.consultationStatus.equals("Completed", true)
            val apptDate = appt.date?.let { try { LocalDate.parse(it) } catch (e: Exception) { null } }
            !isCompleted && apptDate != null && apptDate.isEqual(today)
        }.sortedWith { a, b ->
            val t1 = a.time?.let { try { LocalTime.parse(it) } catch (e: Exception) { LocalTime.MIN } } ?: LocalTime.MIN
            val t2 = b.time?.let { try { LocalTime.parse(it) } catch (e: Exception) { LocalTime.MIN } } ?: LocalTime.MIN
            t1.compareTo(t2)
        }

        _upcomingAppointments.postValue(upcoming)
        _todayAppointments.postValue(todayAppts.take(3))
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
