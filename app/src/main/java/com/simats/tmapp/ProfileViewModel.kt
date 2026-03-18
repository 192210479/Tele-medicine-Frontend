package com.simats.tmapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.simats.tmapp.api.ChangePasswordRequest
import com.simats.tmapp.api.DeviceResponse
import com.simats.tmapp.api.LoginHistoryResponse
import com.simats.tmapp.api.ProfileResponse
import com.simats.tmapp.api.UpdateProfileRequest

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    private val _profile = MutableLiveData<ProfileResponse?>()
    val profile: LiveData<ProfileResponse?> get() = _profile

    private val _updateStatus = MutableLiveData<Pair<Boolean, String?>>()
    val updateStatus: LiveData<Pair<Boolean, String?>> get() = _updateStatus

    private val _passwordStatus = MutableLiveData<Pair<Boolean, String>>()
    val passwordStatus: LiveData<Pair<Boolean, String>> get() = _passwordStatus

    private val _loginHistory = MutableLiveData<List<LoginHistoryResponse>?>()
    val loginHistory: LiveData<List<LoginHistoryResponse>?> get() = _loginHistory

    private val _devices = MutableLiveData<List<DeviceResponse>?>()
    val devices: LiveData<List<DeviceResponse>?> get() = _devices

    private val _actionStatus = MutableLiveData<Pair<Boolean, String?>>()
    val actionStatus: LiveData<Pair<Boolean, String?>> get() = _actionStatus

    private val _privacyInfo = MutableLiveData<String?>()
    val privacyInfo: LiveData<String?> get() = _privacyInfo

    fun fetchProfile(userId: Int, role: String) {
        repository.getProfile(userId, role) { profile, error ->
            _profile.postValue(profile)
        }
    }

    fun updateProfile(request: UpdateProfileRequest) {
        repository.updateProfile(request) { success, error ->
            _updateStatus.postValue(Pair(success, error))
        }
    }

    fun changePassword(request: ChangePasswordRequest) {
        repository.changePassword(request) { success, message ->
            _passwordStatus.postValue(Pair(success, message))
        }
    }

    fun fetchLoginHistory(userId: Int, role: String) {
        repository.getLoginActivity(userId, role) { history, error ->
            _loginHistory.postValue(history)
        }
    }

    fun fetchDevices(userId: Int, role: String) {
        repository.getDevices(userId, role) { devices, error ->
            _devices.postValue(devices)
        }
    }

    fun deleteDevice(deviceId: Int, userId: Int, role: String) {
        repository.deleteDevice(deviceId) { success, error ->
            _actionStatus.postValue(Pair(success, error))
            if (success) {
                fetchDevices(userId, role) // UI Realtime Update Section 11
            }
        }
    }

    fun logoutAllDevices(userId: Int, role: String) {
        repository.logoutAllDevices(userId, role) { success, error ->
            _actionStatus.postValue(Pair(success, error))
        }
    }

    fun fetchPrivacyInfo() {
        repository.getPrivacyInfo { text ->
            _privacyInfo.postValue(text)
        }
    }

    fun deleteAccount(userId: Int, role: String) {
        repository.deleteAccount(userId, role) { success, error ->
            _actionStatus.postValue(Pair(success, error))
        }
    }
}
