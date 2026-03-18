package com.simats.tmapp.api

import com.google.gson.annotations.SerializedName

// Emergency Alert
data class EmergencyAlertRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("message") val message: String = "Emergency triggered"
)

data class EmergencyAlertResponse(
    @SerializedName("message") val message: String
)

data class DashboardSummary(
    @SerializedName("patients") val patients: Int,
    @SerializedName("revenue") val revenue: Double,
    @SerializedName("active_doctors") val activeDoctors: Int,
    @SerializedName("appointments") val appointments: Int
)

data class RevenueTrend(
    @SerializedName("month") val month: String,
    @SerializedName("amount") val amount: Double
)

data class WeeklyAppointments(
    @SerializedName("day") val day: String,
    @SerializedName("count") val count: Int
)

data class DoctorActivity(
    @SerializedName("day") val day: String,
    @SerializedName("count") val count: Int
)

data class PatientRegistration(
    @SerializedName("day") val day: String,
    @SerializedName("count") val count: Int
)

data class Doctor(
    val id: Int,
    val name: String,
    val specialization: String,
    val status: String? = null,
    val experience: Int? = null,
    val fee: Double? = null
)

data class Patient(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("age") val age: Int,
    @SerializedName("last_visit") val lastVisit: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("last_appointment") val lastAppointment: String? = null,
    @SerializedName("total_records") val totalRecords: Int = 0
)

data class Appointment(
    @SerializedName("id") val id: Int,
    @SerializedName("doctor_id") val doctorId: Int?,
    @SerializedName(value = "patient_name", alternate = ["patient"]) val patientName: String?,
    @SerializedName(value = "doctor_name", alternate = ["doctor"]) val doctorName: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("consultation_status") val consultationStatus: String?
)

// Hospital
data class Hospital(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("distance") val distance: String,
    @SerializedName("status") val status: String
)

// Emergency Contact
data class EmergencyContact(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String
)

data class AddEmergencyContactRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String
)

data class ShareLocationRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class SOSRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

// Patient Registration
data class PatientRegisterRequest(
    @SerializedName("full_name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null
)

// Doctor Registration
data class DoctorRegisterRequest(
    @SerializedName("full_name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("specialization") val specialization: String?,
    @SerializedName("license_number") val licenseNumber: String?,
    @SerializedName("experience_years") val experience_years: Int? = 0,
    @SerializedName("fee") val fee: Double? = 0.0,
    @SerializedName("languages") val languages: String? = "",
    @SerializedName("bio") val bio: String? = ""
)

data class RegisterResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)

// Login
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_name") val deviceName: String? = null,
    @SerializedName("location") val location: String? = null
)

// Updated LoginResponse to match usage if needed, but looks okay
data class LoginResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("user_id") val userId: Int?
)

data class GenericResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)

data class MedicalRecordResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("record_type") val recordType: String? = null,
    @SerializedName("uploaded_by") val uploadedBy: String? = null,
    @SerializedName("access_granted") val accessGranted: Boolean = false,
    @SerializedName("created_at") val createdAt: String
)

// Profile
data class ProfileResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("specialization") val specialization: String?,
    @SerializedName("experience_years") val experienceYears: Int?,
    @SerializedName("fee") val fee: Double?,
    @SerializedName("languages") val languages: String?,
    @SerializedName("bio") val bio: String?
)

data class UpdateProfileRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("full_name") val name: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("specialization") val specialization: String? = null,
    @SerializedName("experience_years") val experience_years: Int? = null,
    @SerializedName("fee") val fee: Double? = null,
    @SerializedName("languages") val languages: String? = null,
    @SerializedName("bio") val bio: String? = null
)

// Medication Reminders
data class MedicationReminderResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("medicine_name") val medicineName: String,
    @SerializedName("reminder_time") val reminderTime: String,
    @SerializedName("status") val status: String
)

// Admin Models
data class PlatformStatsResponse(
    @SerializedName("patients") val patients: Int,
    @SerializedName("doctors") val doctors: Int,
    @SerializedName("appointments") val appointments: Int,
    @SerializedName("revenue") val revenue: Double
)

data class DashboardStatsResponse(
    @SerializedName("patients") val patients: Int,
    @SerializedName("revenue") val revenue: Double,
    @SerializedName("active_doctors") val activeDoctors: Int,
    @SerializedName("today_appointments") val todayAppointments: Int
)

data class RevenueTrendResponse(
    @SerializedName("months") val months: List<String>,
    @SerializedName("revenue") val revenue: List<Double>
)

data class WeeklyAppointmentsResponse(
    @SerializedName("days") val days: List<String>,
    @SerializedName("appointments") val appointments: List<Int>
)

data class DoctorActivityTrendResponse(
    @SerializedName("days") val days: List<String>,
    @SerializedName("activity") val activity: List<Int>
)

data class PatientRegistrationTrendResponse(
    @SerializedName("days") val days: List<String>,
    @SerializedName("registrations") val registrations: List<Int>
)

data class PendingDoctorResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("specialization") val specialization: String,
    @SerializedName("experience_years") val experienceYears: Int,
    @SerializedName("status") val status: String
)

data class AdminDoctorResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("specialization") val specialization: String,
    @SerializedName("experience_years") val experienceYears: Int,
    @SerializedName("fee") val fee: Double,
    @SerializedName("languages") val languages: String,
    @SerializedName("status") val status: String
)

data class AdminPatientResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("wallet_balance") val walletBalance: Double? = 0.0
)

data class AdminAppointmentResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("doctor_id") val doctorId: Int?,
    @SerializedName("patient_id") val patientId: Int?,
    @SerializedName(value = "doctor_name", alternate = ["doctor"]) val doctorName: String?,
    @SerializedName(value = "patient_name", alternate = ["patient"]) val patientName: String?,
    @SerializedName("appointment_date") val date: String?,
    @SerializedName("time") val time: String? = null,
    @SerializedName("status") val status: String?,
    @SerializedName("consultation_status") val consultationStatus: String? = null
)

data class AdminPaymentResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("amount") val amount: Double,
    @SerializedName("status") val status: String,
    @SerializedName("date") val date: String
)

data class ConsultationStatusResponse(
    @SerializedName("consultation_id") val consultationId: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("channel") val channel: String?,
    @SerializedName("can_join") val canJoin: Boolean?
)

data class ConsultationStartResponse(
    @SerializedName("consultation_id") val consultationId: Int,
    @SerializedName("channel") val channel: String,
    @SerializedName("status") val status: String
)

data class ConsultationStartRequest(
    @SerializedName("appointment_id") val appointmentId: Int,
    @SerializedName("doctor_id") val doctorId: Int
)

data class ConsultationReadyRequest(
    @SerializedName("consultation_id") val consultationId: Int
)

data class VideoTokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("uid") val uid: Int
)

data class ConsultationEndRequest(
    @SerializedName("consultation_id") val consultationId: Int
)

data class ReconnectRequest(
    @SerializedName("consultation_id") val consultationId: Int,
    @SerializedName("user_id") val userId: Int
)

data class AppointmentResponse(

 @SerializedName("id")
 val id: Int,

 @SerializedName("doctor_id")
 val doctorId: Int?,

 @SerializedName("patient_id")
 val patientId: Int?,

 @SerializedName("doctor_name")
 val doctorName: String?,

 @SerializedName("patient_name")
 val patientName: String?,

 @SerializedName("specialization")
 val specialization: String?,

 @SerializedName("date")
 val date: String?,

 @SerializedName("time") val time: String?,

 @SerializedName("status")
 val status: String?,

 @SerializedName("consultation_status")
 val consultationStatus: String?,

 @SerializedName("patient_age")
 val patientAge: Int? = null,

 @SerializedName("patient_gender")
 val patientGender: String? = null,

 @SerializedName("cancellation_reason")
 val cancellationReason: String? = null
)

data class MedicalReport(
    val id: Int,
    @SerializedName("file_name") val file_name: String,
    @SerializedName("created_at") val created_at: String
)

data class UploadResponse(
    val message: String,
    @SerializedName("record_id") val recordId: Int? = null,
    @SerializedName("file_name") val fileName: String? = null
)



data class AvailabilityResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("time_slot") val time_slot: String,
    @SerializedName("date") val date: String,
    @SerializedName("status") val status: String? = null,
    @SerializedName("is_booked") val is_booked: Int? = 0,
    @SerializedName("appointment_id") val appointment_id: Int? = null
)

data class PostAvailabilityRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("date") val date: String,
    @SerializedName("time_slot") val timeSlot: String
)

data class PostBulkAvailabilityRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("date") val date: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("slot_duration") val slotDuration: Int
)

data class DoctorResponse(
    val id: Int,
    val name: String,
    val specialization: String,
    val experience: Int,
    val fee: Double,
    val languages: String,
    val bio: String?
)

data class ChatMessageResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("sender_role") val sender_role: String,
    @SerializedName("message") val message: String,
    @SerializedName("created_at") val created_at: String
)

data class NotificationResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("appointment_id") val appointmentId: Int? = null,
    @SerializedName("doctor_id") val doctorId: Int? = null,
    @SerializedName("reference_id") val referenceId: Int? = null
)

data class PaymentMethodResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val card_type: String,
    @SerializedName("last4") val card_last4: String
)

data class WalletHistoryResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val status: String,
    @SerializedName("created_at") val date: String
)

data class WalletResponse(
    @SerializedName("balance") val wallet_balance: Double,
    @SerializedName("message") val message: String?
)

data class ConsultationActionRequest(
    val user_id: Int,
    val role: String
)

data class ApiResponse(
    val message: String? = null,
    val error: String? = null
)

// Login Activity / History
data class LoginHistoryResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("device") val device: String,
    @SerializedName("location") val location: String,
    @SerializedName("date") val date: String,
    @SerializedName("is_current") val isCurrent: Boolean = false
)

// Device Management
data class DeviceResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("ip_address") val ipAddress: String,
    @SerializedName("last_login") val lastLogin: String
)

// password change
data class ChangePasswordRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

// Prescription Models
data class CreatePrescriptionRequest(
    @SerializedName("appointment_id") val appointmentId: Int,
    @SerializedName("doctor_id") val doctorId: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("diagnosis") val diagnosis: String,
    @SerializedName("advice") val advice: String,
    @SerializedName("medicines") val medicines: List<MedicineRequest>
)

data class MedicineRequest(
    @SerializedName("name") val name: String,
    @SerializedName("dosage") val dosage: String,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("instructions") val instructions: String
)

data class CreatePrescriptionResponse(
    @SerializedName("prescription_id") val prescriptionId: Int
)

data class AddMedicineRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("name") val name: String,
    @SerializedName("dosage") val dosage: String,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("instructions") val instructions: String
)

data class PrescriptionStatusResponse(
    @SerializedName("status") val status: String?
)

data class PrescriptionResponse(
    @SerializedName("diagnosis") val diagnosis: String?,
    @SerializedName("advice") val advice: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("medicines") val medicines: List<MedicineResponse>?
)

data class MedicineResponse(
    @SerializedName("name") val name: String?,
    @SerializedName("dosage") val dosage: String?,
    @SerializedName("frequency") val frequency: String?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("instructions") val instructions: String?
)

data class PrescriptionHistoryResponse(
    @SerializedName("id") val id: Int?,
    @SerializedName("doctor_id") val doctorId: Int?,
    @SerializedName("doctor_name") val doctor_name: String?,
    @SerializedName("specialization") val specialization: String? = null,
    @SerializedName("diagnosis") val diagnosis: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("appointment_id") val appointmentId: Int?
)

// --- Step 4 Models ---

data class ConsultationChatSendRequest(
    @SerializedName("consultation_id") val consultationId: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("message") val message: String
)

data class ConsultationChatMessageResponse(
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: String
)

data class PatientProfileV2Response(
    @SerializedName("name") val name: String,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("blood_group") val bloodGroup: String?
)

data class PatientHistoryResponse(
    @SerializedName("date") val date: String,
    @SerializedName("diagnosis") val diagnosis: String
)

data class PatientReportResponse(
    @SerializedName("report_name") val reportName: String,
    @SerializedName("file_url") val fileUrl: String
)

data class CreatePrescriptionRequestV2(
    @SerializedName("consultation_id") val consultationId: Int,
    @SerializedName("diagnosis") val diagnosis: String,
    @SerializedName("advice") val advice: String,
    @SerializedName("medicines") val medicines: List<MedicineRequest>
)

data class PrescriptionDetailResponse(
    @SerializedName("diagnosis") val diagnosis: String,
    @SerializedName("advice") val advice: String,
    @SerializedName("medicines") val medicines: List<MedicineResponseV2>
)

data class MedicineResponseV2(
    @SerializedName("name") val name: String,
    @SerializedName("dosage") val dosage: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("frequency") val frequency: String
)
