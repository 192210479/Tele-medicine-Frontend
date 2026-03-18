package com.simats.tmapp

import com.simats.tmapp.api.ApiService
import com.simats.tmapp.api.Doctor
import com.simats.tmapp.api.GenericResponse
import com.simats.tmapp.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoctorRepository {
    private val api: ApiService = RetrofitClient.instance

    fun getPendingDoctors(callback: (List<Doctor>?) -> Unit) {
        api.getPendingDoctorsV2("admin").enqueue(object : Callback<List<Doctor>> {
            override fun onResponse(call: Call<List<Doctor>>, response: Response<List<Doctor>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<Doctor>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getApprovedDoctors(callback: (List<Doctor>?) -> Unit) {
        api.getApprovedDoctors("admin").enqueue(object : Callback<List<Doctor>> {
            override fun onResponse(call: Call<List<Doctor>>, response: Response<List<Doctor>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<Doctor>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getRejectedDoctors(callback: (List<Doctor>?) -> Unit) {
        api.getRejectedDoctors("admin").enqueue(object : Callback<List<Doctor>> {
            override fun onResponse(call: Call<List<Doctor>>, response: Response<List<Doctor>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<Doctor>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getAllDoctors(callback: (List<Doctor>?) -> Unit) {
        api.getAllDoctors("admin").enqueue(object : Callback<List<Doctor>> {
            override fun onResponse(call: Call<List<Doctor>>, response: Response<List<Doctor>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<Doctor>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun approveDoctor(doctorId: Int, callback: (Boolean, String?) -> Unit) {
        val bodyMap = mapOf("role" to "admin")
        api.approveDoctorV2(doctorId, bodyMap).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                val body = response.body()
                val isSuccess = if (body != null) {
                    body.success == true || body.message != null
                } else false
                val message = body?.message ?: body?.error ?: "Failed to approve doctor"
                callback(isSuccess, message)
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }

    fun rejectDoctor(doctorId: Int, callback: (Boolean, String?) -> Unit) {
        val bodyMap = mapOf("role" to "admin")
        api.rejectDoctorV2(doctorId, bodyMap).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                val body = response.body()
                val isSuccess = if (body != null) {
                    body.success == true || body.message != null
                } else false
                val message = body?.message ?: body?.error ?: "Failed to reject doctor"
                callback(isSuccess, message)
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }
}
