package com.simats.Tmapp

import com.simats.Tmapp.api.RetrofitClient
import com.simats.Tmapp.api.DashboardSummary
import com.simats.Tmapp.api.RevenueTrend
import com.simats.Tmapp.api.WeeklyAppointments
import com.simats.Tmapp.api.DoctorActivity
import com.simats.Tmapp.api.PatientRegistration
import com.simats.Tmapp.api.Doctor
import com.simats.Tmapp.api.Patient
import com.simats.Tmapp.api.Appointment
import com.simats.Tmapp.api.GenericResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminRepository {
    private val api = RetrofitClient.instance

    fun getDashboardSummary(callback: (DashboardSummary?) -> Unit) {
        api.getDashboardSummary().enqueue(object : Callback<DashboardSummary> {
            override fun onResponse(call: Call<DashboardSummary>, response: Response<DashboardSummary>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<DashboardSummary>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getRevenueTrend(callback: (List<RevenueTrend>?) -> Unit) {
        api.getRevenueTrend().enqueue(object : Callback<List<RevenueTrend>> {
            override fun onResponse(call: Call<List<RevenueTrend>>, response: Response<List<RevenueTrend>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<RevenueTrend>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getWeeklyAppointments(callback: (List<WeeklyAppointments>?) -> Unit) {
        api.getWeeklyAppointments().enqueue(object : Callback<List<WeeklyAppointments>> {
            override fun onResponse(call: Call<List<WeeklyAppointments>>, response: Response<List<WeeklyAppointments>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<WeeklyAppointments>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getDoctorActivity(callback: (List<DoctorActivity>?) -> Unit) {
        api.getDoctorActivity().enqueue(object : Callback<List<DoctorActivity>> {
            override fun onResponse(call: Call<List<DoctorActivity>>, response: Response<List<DoctorActivity>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<DoctorActivity>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getPatientRegistrations(callback: (List<PatientRegistration>?) -> Unit) {
        api.getPatientRegistrations().enqueue(object : Callback<List<PatientRegistration>> {
            override fun onResponse(call: Call<List<PatientRegistration>>, response: Response<List<PatientRegistration>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<PatientRegistration>>, t: Throwable) {
                callback(null)
            }
        })
    }

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

    fun approveDoctor(doctorId: Int, callback: (Boolean, String?) -> Unit) {
        val bodyMap = mapOf("status" to "Approved")
        api.approveDoctorV2(doctorId, bodyMap).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                val body = response.body()
                val isSuccess = if (body != null) {
                    body.success == true || body.message != null
                } else false
                val message = body?.message ?: body?.error ?: if (isSuccess) "Doctor approved successfully" else "Failed to approve doctor"
                callback(isSuccess, message)
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }

    fun rejectDoctor(doctorId: Int, callback: (Boolean, String?) -> Unit) {
        val bodyMap = mapOf("status" to "Rejected")
        api.rejectDoctorV2(doctorId, bodyMap).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                val body = response.body()
                val isSuccess = if (body != null) {
                    body.success == true || body.message != null
                } else false
                val message = body?.message ?: body?.error ?: if (isSuccess) "Doctor rejected successfully" else "Failed to reject doctor"
                callback(isSuccess, message)
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }

    fun getAllDoctors(callback: (List<Doctor>?) -> Unit) {
        api.getAdminDoctorsFull("admin").enqueue(object : Callback<List<Doctor>> {
            override fun onResponse(call: Call<List<Doctor>>, response: Response<List<Doctor>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }

            override fun onFailure(call: Call<List<Doctor>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getAllPatients(callback: (List<Patient>?) -> Unit) {
        api.getAdminPatientsFull("admin").enqueue(object : Callback<List<Patient>> {
            override fun onResponse(call: Call<List<Patient>>, response: Response<List<Patient>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }

            override fun onFailure(call: Call<List<Patient>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun getMyAppointmentsForAdmin(adminId: Int, callback: (List<Appointment>?) -> Unit) {
        api.getAllAppointmentsV2("admin").enqueue(object : Callback<List<Appointment>> {
            override fun onResponse(call: Call<List<Appointment>>, response: Response<List<Appointment>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<Appointment>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun reassignAppointment(id: Int, body: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        api.reassignAppointment(id, body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                val bodyResp = response.body()
                val isSuccess = if (bodyResp != null) {
                    bodyResp.success == true || bodyResp.message != null
                } else false
                val message = bodyResp?.message ?: bodyResp?.error ?: "Failed to reassign appointment"
                callback(isSuccess, message)
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }

    fun cancelAppointment(id: Int, callback: (Boolean, String?) -> Unit) {
        val body = mapOf("role" to "admin")
        api.cancelAppointment(id, body).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                val bodyResp = response.body()
                val isSuccess = if (bodyResp != null) {
                    bodyResp.success == true || bodyResp.message != null
                } else false
                val message = bodyResp?.message ?: bodyResp?.error ?: "Failed to cancel appointment"
                callback(isSuccess, message)
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }
}
