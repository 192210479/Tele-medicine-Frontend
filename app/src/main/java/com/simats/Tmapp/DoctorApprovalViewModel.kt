package com.simats.Tmapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.simats.Tmapp.api.Doctor

class DoctorApprovalViewModel : ViewModel() {
    private val repository = DoctorRepository()

    private val _pendingDoctors = MutableLiveData<List<Doctor>>(emptyList())
    val pendingDoctors: LiveData<List<Doctor>> = _pendingDoctors

    private val _approvedDoctors = MutableLiveData<List<Doctor>>(emptyList())
    val approvedDoctors: LiveData<List<Doctor>> = _approvedDoctors

    private val _rejectedDoctors = MutableLiveData<List<Doctor>>(emptyList())
    val rejectedDoctors: LiveData<List<Doctor>> = _rejectedDoctors

    fun loadDoctors() {
        repository.getPendingDoctors { list ->
            _pendingDoctors.postValue(list ?: emptyList())
        }
        repository.getApprovedDoctors { list ->
            _approvedDoctors.postValue(list ?: emptyList())
        }
        repository.getRejectedDoctors { list ->
            _rejectedDoctors.postValue(list ?: emptyList())
        }
    }

    fun approveDoctor(doctor: Doctor) {
        // Step 1: Optimistic UI update
        val currentPending = _pendingDoctors.value.orEmpty().toMutableList()
        val currentApproved = _approvedDoctors.value.orEmpty().toMutableList()
        
        val target = currentPending.find { it.id == doctor.id }
        if (target != null) {
            currentPending.remove(target)
            val updatedDoc = target.copy(status = "Approved")
            currentApproved.add(0, updatedDoc)
            
            _pendingDoctors.postValue(currentPending)
            _approvedDoctors.postValue(currentApproved)
        }

        // Step 2: Backend update
        repository.approveDoctor(doctor.id) { success, message ->
            // Step 3: Always refresh all to ensure consistency
            loadDoctors()
        }
    }

    fun rejectDoctor(doctor: Doctor) {
        // Step 1: Optimistic UI update
        val currentPending = _pendingDoctors.value.orEmpty().toMutableList()
        val currentRejected = _rejectedDoctors.value.orEmpty().toMutableList()
        
        val target = currentPending.find { it.id == doctor.id }
        if (target != null) {
            currentPending.remove(target)
            val updatedDoc = target.copy(status = "Rejected")
            currentRejected.add(0, updatedDoc)
            
            _pendingDoctors.postValue(currentPending)
            _rejectedDoctors.postValue(currentRejected)
        }

        // Step 2: Backend update
        repository.rejectDoctor(doctor.id) { success, message ->
            loadDoctors()
        }
    }
}
