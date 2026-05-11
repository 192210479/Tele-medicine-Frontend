package com.simats.Tmapp

import com.simats.Tmapp.api.ChangePasswordRequest
import com.simats.Tmapp.api.DeviceResponse
import com.simats.Tmapp.api.GenericResponse
import com.simats.Tmapp.api.LoginHistoryResponse
import com.simats.Tmapp.api.ProfileResponse
import com.simats.Tmapp.api.ApiClient
import com.simats.Tmapp.api.UpdateProfileRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileRepository {

    fun updateDoctorDocuments(
        userId: RequestBody,
        license: MultipartBody.Part?,
        medical: MultipartBody.Part?,
        onResult: (Boolean, String?) -> Unit
    ) {
        ApiClient.instance.updateDoctorDocuments(userId, license, medical).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "Failed to update documents")
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                onResult(false, t.message)
            }
        })
    }

    fun getProfile(userId: Int, role: String, onResult: (ProfileResponse?, String?) -> Unit) {
        ApiClient.instance.getProfile(userId, role).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful) onResult(response.body(), null)
                else onResult(null, "Failed to fetch profile")
            }
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                onResult(null, t.message)
            }
        })
    }

    fun updateProfile(request: UpdateProfileRequest, onResult: (Boolean, String?) -> Unit) {
        ApiClient.instance.updateProfile(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "Failed to update profile")
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                onResult(false, t.message)
            }
        })
    }

    fun changePassword(request: ChangePasswordRequest, onResult: (Boolean, String) -> Unit) {
        ApiClient.instance.changePassword(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) onResult(true, "Password updated successfully")
                else onResult(false, response.body()?.message ?: response.body()?.error ?: "Failed to change password: ${response.code()}")
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                onResult(false, "Network error: ${t.message}")
            }
        })
    }

    fun getLoginActivity(userId: Int, role: String, onResult: (List<LoginHistoryResponse>?, String?) -> Unit) {
        ApiClient.instance.getLoginActivity(userId, role).enqueue(object : Callback<List<LoginHistoryResponse>> {
            override fun onResponse(call: Call<List<LoginHistoryResponse>>, response: Response<List<LoginHistoryResponse>>) {
                if (response.isSuccessful) onResult(response.body(), null)
                else onResult(null, "Failed to load login history")
            }
            override fun onFailure(call: Call<List<LoginHistoryResponse>>, t: Throwable) {
                onResult(null, t.message)
            }
        })
    }

    fun getDevices(userId: Int, role: String, onResult: (List<DeviceResponse>?, String?) -> Unit) {
        ApiClient.instance.getDevices(userId, role).enqueue(object : Callback<List<DeviceResponse>> {
            override fun onResponse(call: Call<List<DeviceResponse>>, response: Response<List<DeviceResponse>>) {
                if (response.isSuccessful) onResult(response.body(), null)
                else onResult(null, "Failed to load devices")
            }
            override fun onFailure(call: Call<List<DeviceResponse>>, t: Throwable) {
                onResult(null, t.message)
            }
        })
    }

    fun deleteDevice(deviceId: Int, onResult: (Boolean, String?) -> Unit) {
        ApiClient.instance.deleteDevice(deviceId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "Failed to delete device")
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                onResult(false, t.message)
            }
        })
    }

    fun logoutAllDevices(userId: Int, role: String, onResult: (Boolean, String?) -> Unit) {
        val body = mapOf<String, Any>("user_id" to userId, "role" to role)
        ApiClient.instance.logoutAllDevices(body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "Failed to logout all devices")
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                onResult(false, t.message)
            }
        })
    }

    fun getPrivacyInfo(onResult: (String?) -> Unit) {
        ApiClient.instance.getPrivacyInfo().enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) onResult(response.body()?.get("text"))
                else onResult(null)
            }
            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                onResult(null)
            }
        })
    }

    fun deleteAccount(userId: Int, role: String, onResult: (Boolean, String?) -> Unit) {
        ApiClient.instance.deleteAccount(userId, role).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "Failed to delete account")
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                onResult(false, t.message)
            }
        })
    }
}
