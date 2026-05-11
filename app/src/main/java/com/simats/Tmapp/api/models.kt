package com.simats.Tmapp.api

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
    @SerializedName("total_patients") val total_patients: Int = 0,
    @SerializedName("patients_growth_percent") val patients_growth_percent: Int = 0,
    @SerializedName("total_doctors") val total_doctors: Int = 0,
    @SerializedName("new_doctors_count") val new_doctors_count: Int = 0,
    @SerializedName("total_appointments") val total_appointments: Int = 0,
    @SerializedName("today_appointments") val today_appointments: Int = 0,
    @SerializedName("total_revenue") val total_revenue: Double = 0.0,
    @SerializedName("revenue_growth_percent") val revenue_growth_percent: Int = 0,
    @SerializedName("pending_approvals") val pending_approvals: Int = 0
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
    @SerializedName(value = "name", alternate = ["full_name"]) val name: String,
    val specialization: String,
    val status: String? = null,
    val experience: Int? = null,
    val fee: Double? = null,
    @SerializedName("license_number") val licenseNumber: String? = null,
    @SerializedName("license_file") val licenseFile: String? = null,
    @SerializedName("medical_record_file") val medicalRecordFile: String? = null,
    @SerializedName("experience_years") val experienceYears: Int? = null,
    @SerializedName("profile_image") val profileImage: String? = null,
    val email: String? = null,
    val phone: String? = null,

    // NEW ADMIN FIELDS
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("total_appointments") val totalAppointments: Int? = 0,
    @SerializedName("today_appointments") val todayAppointments: Int? = 0,
    @SerializedName("last_appointment") val lastAppointment: String? = null
)

data class Patient(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String?,
    @SerializedName("age") val age: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("profile_image") val profileImage: String?,
    @SerializedName("blood_group") val blood_group: String?,
    @SerializedName("last_appointment") val lastAppointment: String?,
    @SerializedName("total_records") val totalRecords: Int?,

    // NEW ADMIN FIELDS
    @SerializedName("registered_at") val registeredAt: String? = null,
    @SerializedName("last_doctor_name") val lastDoctorName: String? = null,
    @SerializedName("total_appointments") val totalAppointments: Int? = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("address") val address: String? = null
)

data class Appointment(
    @SerializedName("id") val id: Int,
    @SerializedName("doctor_id") val doctorId: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("doctor_name") val doctorName: String?,
    @SerializedName("patient_name") val patientName: String?,
    @SerializedName("specialization") val specialization: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("time") val time: String? = null,
    @SerializedName("utc_time") val utcTime: String?,
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
    @SerializedName("role") val role: String? = null,
    @SerializedName("device_name") val deviceName: String? = null,
    @SerializedName("location") val location: String? = null
)

data class LoginResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("user_id") val userId: Int?
)

data class GenericResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val error: String? = null
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
    @SerializedName("bio") val bio: String?,
    @SerializedName("status") val status: String? = null
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
    @SerializedName(value = "date", alternate = ["appointment_date"]) val date: String?,
    @SerializedName("time") val time: String? = null,
    @SerializedName("status") val status: String?,
    @SerializedName("utc_time") val utcTime: String? = null,
    @SerializedName("consultation_status") val consultationStatus: String? = null,
    @SerializedName("cancelled_by") val cancelledBy: String? = null,
    @SerializedName("specialization") val specialization: String? = null
)

data class BookAppointmentRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String = "patient",
    @SerializedName("availability_id") val availabilityId: Int,
    @SerializedName("symptoms") val symptoms: String? = null,
    @SerializedName("priority") val priority: String? = "low"
)

data class BookingResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("appointment_id") val appointmentId: Int? = null,
    @SerializedName("doctor_id") val doctorId: Int? = null,
    @SerializedName("doctor_name") val doctorName: String? = null,
    @SerializedName("specialization") val specialization: String? = null,
    @SerializedName("doctor_image") val doctorImage: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("payment_required") val paymentRequired: Boolean? = null,
    @SerializedName("error") val error: String? = null
)

data class CreatePaymentOrderRequest(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("doctor_id") val doctorId: Int,
    @SerializedName("appointment_id") val appointmentId: Int,
    @SerializedName("amount") val amount: Double,
    @SerializedName("timezone") val timezone: String
)

data class CreatePaymentOrderResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("payment_id") val paymentId: Int? = null,
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("currency") val currency: String? = "INR",
    @SerializedName("appointment_id") val appointmentId: Int? = null,
    @SerializedName("doctor_id") val doctorId: Int? = null,
    @SerializedName("doctor_name") val doctorName: String? = null,
    @SerializedName("error") val error: String? = null
)

data class AdminPaymentResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("patient_id") val patientId: Int?,
    @SerializedName("patient_name") val patientName: String?,
    @SerializedName("doctor_id") val doctorId: Int?,
    @SerializedName("doctor_name") val doctorName: String?,
    @SerializedName("appointment_id") val appointmentId: Int?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("status") val status: String?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("invoice_number") val invoiceNumber: String?,
    @SerializedName("refund_status") val refundStatus: String?,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("date") val date: String? = null
)

data class ConsultationStatusResponse(
    @SerializedName("consultation_id") val consultationId: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("channel") val channel: String?,
    @SerializedName("can_join") val canJoin: Boolean?,
    @SerializedName("started_at") val startedAt: Long? = null
)

data class ConsultationStartResponse(
    @SerializedName("consultation_id") val consultationId: Int,
    @SerializedName("channel") val channel: String,
    @SerializedName("status") val status: String,
    @SerializedName("started_at") val startedAt: Long? = null
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
    @SerializedName("uid") val uid: Int,
    @SerializedName("started_at") val startedAt: Long? = null
)

data class ConsultationEndRequest(
    @SerializedName("consultation_id") val consultationId: Int
)

data class ReconnectRequest(
    @SerializedName("consultation_id") val consultationId: Int,
    @SerializedName("user_id") val userId: Int
)

data class AppointmentResponse(
    @SerializedName("id") val id: Int,

    @SerializedName("doctor_id") val doctorId: Int?,
    @SerializedName("patient_id") val patientId: Int?,

    @SerializedName(value = "doctor_name", alternate = ["doctor", "doctor_full_name", "doctorName"])
    val doctorName: String?,

    @SerializedName(value = "patient_name", alternate = ["patient", "patient_full_name", "patientName", "full_name", "name"])
    val patientName: String?,

    @SerializedName(value = "specialization", alternate = ["doctor_specialization"])
    val specialization: String?,

    @SerializedName(value = "date", alternate = ["appointment_date", "scheduled_date"])
    val date: String?,

    @SerializedName(value = "time", alternate = ["appointment_time", "scheduled_time", "slot_time"])
    val time: String? = null,

    @SerializedName("end_time") val endTime: String? = null,

    @SerializedName("status") val status: String?,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("booking_status") val bookingStatus: String? = null,

    @SerializedName(value = "consultation_fee", alternate = ["fee", "doctor_fee"])
    val consultationFee: Double? = null,

    @SerializedName("consultation_status") val consultationStatus: String?,
    @SerializedName("consultation_room_id") val roomId: String? = null,

    @SerializedName("profile_image") val profileImage: String? = null,
    @SerializedName(value = "doctor_image", alternate = ["doctor_profile_image"])
    val doctorImage: String? = null,
    @SerializedName("patient_image") val patientImage: String? = null,

    @SerializedName("patient_age") val patientAge: Int? = null,
    @SerializedName("patient_gender") val patientGender: String? = null,

    @SerializedName(value = "local_time", alternate = ["time_slot"])
    val localTime: String? = null,

    @SerializedName("utc_time") val utcTime: String? = null,

    @SerializedName("cancellation_reason") val cancellationReason: String? = null,

    // NEW SAFE BACKEND FIELDS
    @SerializedName("cancelled_by") val cancelledBy: String? = null,
    @SerializedName("timezone") val timezone: String? = null,

    // Optional nested payment object support
    @SerializedName("payment_info") val paymentInfo: PaymentInfo? = null
) {
    val fee: Double? get() = consultationFee

    // Rating runtime state — not serialized from JSON
    var ratingStatus: RatingStatusResponse? = null
    var isRatingStatusLoading: Boolean = false
}

data class PaymentInfo(
    @SerializedName("payment_id") val paymentId: Int? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("razorpay_order_id") val razorpayOrderId: String? = null,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String? = null
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
    @SerializedName(value = "time", alternate = ["time_slot"]) val time: String,
    @SerializedName("date") val date: String,
    @SerializedName("status") val status: String? = null,
    @SerializedName("is_booked") val is_booked: Int? = 0,
    @SerializedName("utc_time") val utcTime: String? = null,
    @SerializedName("appointment_id") val appointment_id: Int? = null
)

data class PostAvailabilityRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("date") val date: String,
    @SerializedName("time_slot") val timeSlot: String,
    @SerializedName("timezone") val timezone: String
)

data class PostBulkAvailabilityRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("date") val date: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("slot_duration") val slotDuration: Int,
    @SerializedName("timezone") val timezone: String
)

data class DoctorResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("specialization") val specialization: String,
    @SerializedName("experience") val experience: Int,
    @SerializedName("fee") val fee: Double,
    @SerializedName("languages") val languages: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("rating") val rating: Float? = 0.0f,
    @SerializedName("reviews_count") val reviewsCount: Int? = 0,
    @SerializedName("profile_image") val profileImage: String? = null
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
    @SerializedName("reference_id") val referenceId: Int? = null,
    @SerializedName("appointment_id") val appointmentId: Int? = null,
    @SerializedName("data") val data: String? = null,
    @SerializedName("doctor_id") val doctorId: Int? = null
)

data class NotificationsWrapper(
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("notifications") val notifications: List<NotificationResponse>
)

data class PaymentMethodResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("method_type") val methodType: String?,
    @SerializedName("provider") val provider: String?,
    @SerializedName("masked_details") val maskedDetails: String?,
    @SerializedName("expiry") val expiry: String?,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
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

data class ShareLocationResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("map_link") val mapLink: String,
    @SerializedName("contacts_notified") val contactsNotified: List<ContactNotified>
)

data class ContactNotified(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String
)

data class HealthTipResponse(
    @SerializedName("tip") val tip: String,
    @SerializedName("date") val date: String,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("disclaimer") val disclaimer: String? = null
)

data class DashboardResponse(
    @SerializedName("greeting") val greeting: String?,
    @SerializedName("total") val total: Int? = 0,
    @SerializedName("upcoming") val upcoming: Int? = 0,
    @SerializedName("completed") val completed: Int? = 0,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("current_hour") val currentHour: Int? = null
)

// =========================================================
// ==================== PAYMENTS & FINANCE =================
// =========================================================

// ── Razorpay Order (legacy simple endpoint) ───────────────

data class RazorpayOrderRequest(
    @SerializedName("appointment_id") val appointmentId: Int,
    @SerializedName("user_id") val userId: Int
)

data class RazorpayOrderResponse(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("currency") val currency: String,
    @SerializedName("key") val key: String
)

// ── Create Order (Full — new endpoint) ────────────────────

data class CreateOrderRequest(
    @SerializedName("patient_id")     val patientId: Int,
    @SerializedName("doctor_id")      val doctorId: Int,
    @SerializedName("appointment_id") val appointmentId: Int,
    @SerializedName("amount")         val amount: Double,
    @SerializedName("timezone")       val timezone: String? = null
)

data class CreateOrderResponse(
    @SerializedName("message")           val message: String? = null,
    @SerializedName("payment_id")        val paymentId: Int? = null,

    // New full endpoint
    @SerializedName("razorpay_order_id") val razorpayOrderId: String? = null,
    @SerializedName("razorpay_key")      val razorpayKey: String? = null,

    // Legacy/simple fallback
    @SerializedName("order_id")          val orderIdLegacy: String? = null,
    @SerializedName("key")               val keyLegacy: String? = null,

    // Backend may return paise OR INR depending on endpoint
    @SerializedName("amount")            val amount: Int? = null,
    @SerializedName("amount_inr")        val amountInr: Double? = null,
    @SerializedName("currency")          val currency: String? = "INR",

    @SerializedName("payment_status")    val paymentStatus: String? = null,
    @SerializedName("appointment_status") val appointmentStatus: String? = null,
    @SerializedName("doctor_name")       val doctorName: String? = null,
    @SerializedName("patient_name")      val patientName: String? = null,
    @SerializedName("doctor_id")         val doctorId: Int? = null,
    @SerializedName("patient_id")        val patientId: Int? = null,
    @SerializedName("appointment_id")    val appointmentId: Int? = null,
    @SerializedName("created_at_utc")    val createdAtUtc: String? = null,
    @SerializedName("created_at_local")  val createdAtLocal: String? = null,
    @SerializedName("timezone")          val timezone: String? = null,

    @SerializedName("error")             val error: String? = null
) {
    val resolvedOrderId: String? get() = razorpayOrderId ?: orderIdLegacy
    val resolvedKey: String? get() = razorpayKey ?: keyLegacy
}

// ── Verify Payment ────────────────────────────────────────

data class PaymentVerificationRequest(
    @SerializedName("razorpay_order_id")   val orderId: String,
    @SerializedName("razorpay_payment_id") val paymentId: String,
    @SerializedName("razorpay_signature")  val signature: String
)

data class VerifyPaymentRequest(
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("doctor_id") val doctorId: Int?,
    @SerializedName("appointment_id") val appointmentId: Int?,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_signature") val razorpaySignature: String,
    @SerializedName("amount") val amount: Double?,
    @SerializedName("timezone") val timezone: String?
)

/**
 * Response from /api/payments/verify.
 * Consolidates all fields from multiple duplicate definitions.
 */
data class VerifyPaymentResponse(
    @SerializedName("success")                val successRaw: Boolean? = null,
    @SerializedName("message")                val message: String? = null,
    @SerializedName("payment_id")             val paymentId: Int? = null,
    @SerializedName("invoice_number")         val invoiceNumber: String? = null,
    @SerializedName("amount")                 val amount: Double? = null,
    @SerializedName("net_to_doctor")          val netToDoctor: Double? = null,
    @SerializedName("platform_fee")           val platformFee: Double? = null,
    @SerializedName("doctor_pending_balance") val doctorPendingBalance: Double? = null,
    @SerializedName("appointment_id")         val appointmentId: Int? = null,
    @SerializedName("appointment_status")     val appointmentStatus: String? = null,
    @SerializedName("payment_status")         val paymentStatus: String? = null,
    @SerializedName("doctor_name")            val doctorName: String? = null,
    @SerializedName("patient_name")           val patientName: String? = null,
    @SerializedName("error")                  val error: String? = null
) {
    val success: Boolean
        get() = successRaw == true || (error == null && !message.isNullOrEmpty())
}

data class PaymentVerificationResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

// ── Invoice ────────────────────────────────────────────────

data class InvoiceResponse(
    @SerializedName("id")             val id: Int,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("amount")         val amount: Double,
    @SerializedName("date")           val date: String,
    @SerializedName("appointment_id") val appointmentId: Int
)

data class InvoiceDetailResponse(
    @SerializedName("invoice_id")            val invoiceId: Int?,
    @SerializedName("invoice_number")        val invoiceNumber: String?,
    @SerializedName("payment_id")            val paymentId: Int?,
    @SerializedName("appointment_id")        val appointmentId: Int?,
    @SerializedName("appointment_date")      val appointmentDate: String?,
    @SerializedName("patient_id")            val patientId: Int?,
    @SerializedName("patient_name")          val patientName: String?,
    @SerializedName("doctor_id")             val doctorId: Int?,
    @SerializedName("doctor_name")           val doctorName: String?,
    @SerializedName("doctor_specialization") val doctorSpecialization: String?,
    @SerializedName("consultation_fee")      val consultationFee: Double?,
    @SerializedName("platform_fee")          val platformFee: Double?,
    @SerializedName("tax_amount")            val taxAmount: Double?,
    @SerializedName("total_amount")          val totalAmount: Double?,
    @SerializedName("status")                val status: String?,
    @SerializedName("payment_status")        val paymentStatus: String?,
    @SerializedName("payment_method")        val paymentMethod: String?,
    @SerializedName("razorpay_payment_id")   val razorpayPaymentId: String?,
    @SerializedName("refund_status")         val refundStatus: String?,
    @SerializedName("refund_amount")         val refundAmount: Double?,
    @SerializedName("paid_at")               val paidAt: String?,
    @SerializedName("generated_at")          val generatedAt: String?,
    @SerializedName("error")                 val error: String?
)

// ── Patient Billing History ────────────────────────────────

data class BillingHistoryItem(
    @SerializedName("id")                    val id: Int,
    @SerializedName("invoice_number")        val invoiceNumber: String?,
    @SerializedName("payment_id")            val paymentId: Int?,
    @SerializedName("appointment_id")        val appointmentId: Int?,
    @SerializedName("doctor_id")             val doctorId: Int?,
    @SerializedName("doctor_name")           val doctorName: String?,
    @SerializedName("doctor_specialization") val doctorSpecialization: String?,
    @SerializedName("appointment_date")      val appointmentDate: String?,
    @SerializedName("consultation_fee")      val consultationFee: Double,
    @SerializedName("platform_fee")          val platformFee: Double,
    @SerializedName("tax_amount")            val taxAmount: Double,
    @SerializedName("total_amount")          val totalAmount: Double,
    @SerializedName("status")                val status: String?,
    @SerializedName("payment_status")        val paymentStatus: String?,
    @SerializedName("payment_method")        val paymentMethod: String?,
    @SerializedName("refund_status")         val refundStatus: String?,
    @SerializedName("refund_amount")         val refundAmount: Double?,
    @SerializedName("paid_at")               val paidAt: String?,
    @SerializedName("created_at")            val createdAt: String?
)

// ── Payment Methods ────────────────────────────────────────

data class SavedPaymentMethod(
    @SerializedName("id")             val id: Int,
    @SerializedName("method_type")    val methodType: String?,
    @SerializedName("provider")       val provider: String?,
    @SerializedName("masked_details") val maskedDetails: String?,
    @SerializedName("expiry")         val expiry: String?,
    @SerializedName("is_default")     val isDefault: Boolean,
    @SerializedName("created_at")     val createdAt: String?
)

data class AddSavedPaymentMethodRequest(
    @SerializedName("patient_id")     val patientId: Int,
    @SerializedName("method_type")    val methodType: String,
    @SerializedName("provider")       val provider: String?,
    @SerializedName("masked_details") val maskedDetails: String?,
    @SerializedName("expiry")         val expiry: String?,
    @SerializedName("set_default")    val setDefault: Boolean = false
)

data class DeletePaymentMethodRequest(
    @SerializedName("patient_id") val patientId: Int
)

// ── Refund ─────────────────────────────────────────────────

data class RefundRequest(
    @SerializedName("payment_id")    val paymentId: Int,
    @SerializedName("initiated_by")  val initiatedBy: String,
    @SerializedName("actor_id")      val actorId: Int,
    @SerializedName("reason")        val reason: String?,
    @SerializedName("refund_amount") val refundAmount: Double?
)

data class RefundResponse(
    @SerializedName("message")       val message: String?,
    @SerializedName("refund_id")     val refundId: Int?,
    @SerializedName("payment_id")    val paymentId: Int?,
    @SerializedName("refund_amount") val refundAmount: Double?,
    @SerializedName("status")        val status: String?,
    @SerializedName("error")         val error: String?
)

data class RefundHistoryItem(
    @SerializedName("id")               val id: Int,
    @SerializedName("payment_id")       val paymentId: Int?,
    @SerializedName("appointment_id")   val appointmentId: Int?,
    @SerializedName("appointment_date") val appointmentDate: String?,
    @SerializedName("doctor_id")        val doctorId: Int?,
    @SerializedName("doctor_name")      val doctorName: String?,
    @SerializedName("refund_amount")    val refundAmount: Double?,
    @SerializedName("reason")           val reason: String?,
    @SerializedName("initiated_by")     val initiatedBy: String?,
    @SerializedName("status")           val status: String?,
    @SerializedName("processed_at")     val processedAt: String?,
    @SerializedName("created_at")       val createdAt: String?
)

// ── Doctor Wallet ──────────────────────────────────────────

data class DoctorWalletResponse(
    @SerializedName("doctor_id")         val doctorId: Int? = null,
    @SerializedName("doctor_name")       val doctorName: String? = null,
    @SerializedName("available_balance") val availableBalance: Double,
    @SerializedName("total_earned")      val totalEarned: Double,
    @SerializedName("pending_balance")   val pendingBalance: Double,
    @SerializedName("paid_out_balance")  val paidOutBalance: Double,
    @SerializedName("updated_at")        val updatedAt: String? = null,
    @SerializedName("error")             val error: String? = null
)

// ── Doctor Transactions ────────────────────────────────────

data class DoctorTransactionItem(
    @SerializedName("id")                  val id: Int,
    @SerializedName("transaction_type")    val transactionType: String?,
    @SerializedName("gross_amount")        val grossAmount: Double?,
    @SerializedName("platform_commission") val platformCommission: Double?,
    @SerializedName("net_amount")          val netAmount: Double?,
    @SerializedName("status")              val status: String?,
    @SerializedName("note")                val note: String?,
    @SerializedName("appointment_id")      val appointmentId: Int?,
    @SerializedName("appointment_date")    val appointmentDate: String?,
    @SerializedName("patient_name")        val patientName: String?,
    @SerializedName("payment_id")          val paymentId: Int?,
    @SerializedName("created_at")          val createdAt: String?
)

// ── Doctor Bank Accounts ───────────────────────────────────

data class DoctorBankAccount(
    @SerializedName("id")                  val id: Int,
    @SerializedName("account_type")        val accountType: String?,
    @SerializedName("is_default")          val isDefault: Boolean,
    @SerializedName("is_verified")         val isVerified: Boolean,
    @SerializedName("account_holder_name") val accountHolderName: String?,
    @SerializedName("bank_name")           val bankName: String?,
    @SerializedName("account_number")      val accountNumber: String?,
    @SerializedName("ifsc_code")           val ifscCode: String?,
    @SerializedName("upi_id")              val upiId: String?,
    @SerializedName("created_at")          val createdAt: String?
)

data class AddBankAccountRequest(
    @SerializedName("doctor_id")           val doctorId: Int,
    @SerializedName("account_type")        val accountType: String,
    @SerializedName("account_holder_name") val accountHolderName: String?,
    @SerializedName("bank_name")           val bankName: String?,
    @SerializedName("account_number")      val accountNumber: String?,
    @SerializedName("ifsc_code")           val ifscCode: String?,
    @SerializedName("upi_id")              val upiId: String?,
    @SerializedName("set_default")         val setDefault: Boolean = true
)

data class DeleteBankAccountRequest(
    @SerializedName("doctor_id") val doctorId: Int
)

// ── Withdrawal ─────────────────────────────────────────────

data class WithdrawalRequestBody(
    @SerializedName("doctor_id")       val doctorId: Int,
    @SerializedName("amount")          val amount: Double,
    @SerializedName("bank_account_id") val bankAccountId: Int?
)

data class WithdrawalResponse(
    @SerializedName("message")           val message: String?,
    @SerializedName("withdrawal_id")     val withdrawalId: Int?,
    @SerializedName("amount")            val amount: Double?,
    @SerializedName("status")            val status: String?,
    @SerializedName("available_balance") val availableBalance: Double?,
    @SerializedName("error")             val error: String?
)

data class BankAccountSummary(
    @SerializedName("type")           val type: String?,
    @SerializedName("bank_name")      val bankName: String?,
    @SerializedName("account_number") val accountNumber: String?,
    @SerializedName("upi_id")         val upiId: String?
)

data class WithdrawalHistoryItem(
    @SerializedName("id")           val id: Int,
    @SerializedName("amount")       val amount: Double?,
    @SerializedName("status")       val status: String?,
    @SerializedName("admin_note")   val adminNote: String?,
    @SerializedName("processed_at") val processedAt: String?,
    @SerializedName("bank_account") val bankAccount: BankAccountSummary?,
    @SerializedName("created_at")   val createdAt: String?
)

// ── Admin Finance ──────────────────────────────────────────

data class AdminFinanceSummaryResponse(
    @SerializedName("total_platform_revenue")    val totalRevenue: Double,
    @SerializedName("total_commissions")         val totalCommissions: Double,
    @SerializedName("total_payouts")             val totalPayouts: Double,
    @SerializedName("net_profit")                val netProfit: Double,
    @SerializedName("total_transactions")        val totalTransactions: Int,
    @SerializedName("total_payments_count")      val totalPaymentsCount: Int? = null,
    @SerializedName("total_revenue")             val totalRevenueFull: Double? = null,
    @SerializedName("total_commission")          val totalCommission: Double? = null,
    @SerializedName("net_platform_earnings")     val netPlatformEarnings: Double? = null,
    @SerializedName("total_refunds_count")       val totalRefundsCount: Int? = null,
    @SerializedName("total_refunded_amount")     val totalRefundedAmount: Double? = null,
    @SerializedName("pending_payments_count")    val pendingPaymentsCount: Int? = null,
    @SerializedName("failed_payments_count")     val failedPaymentsCount: Int? = null,
    @SerializedName("pending_withdrawals_count") val pendingWithdrawalsCount: Int? = null,
    @SerializedName("total_withdrawn")           val totalWithdrawn: Double? = null,
    @SerializedName("commission_percent")        val commissionPercent: Double? = null,
    @SerializedName("error")                     val error: String? = null
)

data class AdminPaymentItem(
    @SerializedName("id")                    val id: Int,
    @SerializedName("appointment_id")        val appointmentId: Int?,
    @SerializedName("patient_id")            val patientId: Int?,
    @SerializedName("patient_name")          val patientName: String?,
    @SerializedName("patient_email")         val patientEmail: String?,
    @SerializedName("doctor_id")             val doctorId: Int?,
    @SerializedName("doctor_name")           val doctorName: String?,
    @SerializedName("doctor_specialization") val doctorSpecialization: String?,
    @SerializedName("amount")                val amount: Double?,
    @SerializedName("currency")              val currency: String?,
    @SerializedName("status")                val status: String?,
    @SerializedName("payment_method")        val paymentMethod: String?,
    @SerializedName("invoice_number")        val invoiceNumber: String?,
    @SerializedName("refund_status")         val refundStatus: String?,
    @SerializedName("refund_amount")         val refundAmount: Double?,
    @SerializedName("paid_at")               val paidAt: String?,
    @SerializedName("created_at")            val createdAt: String?
)

data class AdminRefundItem(
    @SerializedName("id")              val id: Int,
    @SerializedName("payment_id")      val paymentId: Int?,
    @SerializedName("appointment_id")  val appointmentId: Int?,
    @SerializedName("patient_name")    val patientName: String?,
    @SerializedName("doctor_name")     val doctorName: String?,
    @SerializedName("original_amount") val originalAmount: Double?,
    @SerializedName("refund_amount")   val refundAmount: Double?,
    @SerializedName("reason")          val reason: String?,
    @SerializedName("initiated_by")    val initiatedBy: String?,
    @SerializedName("status")          val status: String?,
    @SerializedName("processed_at")    val processedAt: String?,
    @SerializedName("created_at")      val createdAt: String?
)

data class AdminWithdrawalItem(
    @SerializedName("id")           val id: Int,
    @SerializedName("doctor_id")    val doctorId: Int?,
    @SerializedName("doctor_name")  val doctorName: String?,
    @SerializedName("doctor_email") val doctorEmail: String?,
    @SerializedName("amount")       val amount: Double?,
    @SerializedName("status")       val status: String?,
    @SerializedName("admin_note")   val adminNote: String?,
    @SerializedName("processed_at") val processedAt: String?,
    @SerializedName("bank_account") val bankAccount: BankAccountSummary?,
    @SerializedName("created_at")   val createdAt: String?
)

// ── Transaction Response (used by AdminPaymentAdapter / legacy payment lists) ──

data class TransactionResponse(
    @SerializedName("id")             val id: Int,
    @SerializedName("patient_name")   val patientName: String? = null,
    @SerializedName("doctor_name")    val doctorName: String? = null,
    @SerializedName("amount")         val amount: Double? = 0.0,
    @SerializedName("status")         val status: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("date")           val date: String? = null
)

// ── Admin Withdrawal Action ────────────────────────────────

data class AdminWithdrawalActionRequest(
    @SerializedName("admin_id") val adminId: Int,
    @SerializedName("note")     val note: String? = null
)

data class AdminWithdrawalActionResponse(
    @SerializedName("message")       val message: String? = null,
    @SerializedName("withdrawal_id") val withdrawalId: Int? = null,
    @SerializedName("amount")        val amount: Double? = null,
    @SerializedName("status")        val status: String? = null,
    @SerializedName("error")         val error: String? = null
)

// ── Admin Finance Analytics ────────────────────────────────

data class AdminFinanceAnalytics(
    @SerializedName("period")               val period: String? = null,
    @SerializedName("total_revenue")        val totalRevenue: Double? = 0.0,
    @SerializedName("total_commission")     val totalCommission: Double? = 0.0,
    @SerializedName("net_platform_earnings") val netPlatformEarnings: Double? = 0.0,
    @SerializedName("total_refunds")        val totalRefunds: Double? = 0.0,
    @SerializedName("total_withdrawals")    val totalWithdrawals: Double? = 0.0,
    @SerializedName("successful_payments")  val successfulPayments: Int? = 0,
    @SerializedName("failed_payments")      val failedPayments: Int? = 0,
    @SerializedName("pending_withdrawals")  val pendingWithdrawals: Int? = 0,
    @SerializedName("labels")              val labels: List<String>? = emptyList(),
    @SerializedName("revenue_series")      val revenueSeries: List<Double>? = emptyList(),
    @SerializedName("commission_series")   val commissionSeries: List<Double>? = emptyList(),
    @SerializedName("error")               val error: String? = null
)

// =======================================================
// ================= HELP & SUPPORT ======================
// =======================================================

data class SupportFaqResponse(
    val id: Int,
    val role: String,
    val category: String,
    val question: String,
    val answer: String
)

data class CreateSupportTicketRequest(
    val user_id: Int,
    val role: String,
    val issue_type: String,
    val title: String,
    val description: String,
    val priority: String = "Medium",
    val appointment_id: Int? = null,
    val payment_id: Int? = null,
    val doctor_id: Int? = null,
    val patient_id: Int? = null,
    val ai_summary: String? = null
)

data class SupportTicketCreateResponse(
    val message: String,
    val ticket_id: Int,
    val status: String
)

data class SupportTicketResponse(
    val id: Int,
    val issue_type: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val appointment_id: Int? = null,
    val payment_id: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class SupportMessageResponse(
    val id: Int,
    val sender_id: Int,
    val sender_role: String,
    val message: String,
    val is_ai_generated: Boolean,
    val created_at: String? = null
)

data class SupportTicketDetailsResponse(
    val ticket: SupportTicketDetail,
    val messages: List<SupportMessageResponse>
)

data class SupportTicketDetail(
    val id: Int,
    val user_id: Int,
    val role: String,
    val issue_type: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val appointment_id: Int? = null,
    val payment_id: Int? = null,
    val doctor_id: Int? = null,
    val patient_id: Int? = null,
    val ai_summary: String? = null,
    val admin_notes: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val resolved_at: String? = null
)

data class ReplySupportTicketRequest(
    val sender_id: Int,
    val sender_role: String,
    val message: String
)


// =======================================================
// =================== AI SUPPORT CHAT ===================
// =======================================================

data class AiSupportRequest(
    val user_id: Int,
    val role: String,
    val message: String
)

data class AiSupportResponse(
    val reply: String,
    val issue_type: String? = null,
    val should_create_ticket: Boolean = false,
    val suggested_priority: String? = null
)

data class AiChatMessage(
    val text: String,
    val isUser: Boolean
)