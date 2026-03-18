package com.simats.tmapp.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface TelemedicineAPI {

    @POST("api/login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("api/register/doctor")
    fun registerDoctor(@Body doctorRegisterRequest: DoctorRegisterRequest): Call<RegisterResponse>

    @POST("api/register/patient")
    fun registerPatient(@Body patientRegisterRequest: PatientRegisterRequest): Call<RegisterResponse>

    @POST("api/password/send-otp")
    fun sendOtp(@Body body: Map<String, String>): Call<GenericResponse>

    @POST("api/password/verify-otp")
    fun verifyOtp(@Body body: Map<String, String>): Call<GenericResponse>

    @POST("api/password/reset")
    fun resetPassword(@Body body: Map<String, String>): Call<GenericResponse>

    @PUT("api/password/change")
    fun changePassword(@Body request: ChangePasswordRequest): Call<GenericResponse>

    @GET("api/emergency/contacts")
    fun getEmergencyContacts(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<EmergencyContact>>

    @POST("api/emergency/contact/add")
    fun addEmergencyContact(@Body contact: AddEmergencyContactRequest): Call<GenericResponse>

    @POST("api/emergency/alert")
    fun sendEmergencyAlert(@Body alertRequest: EmergencyAlertRequest): Call<EmergencyAlertResponse>

    @POST("api/emergency/location")
    fun shareLocation(@Body request: ShareLocationRequest): Call<GenericResponse>

    @POST("api/emergency/sos")
    fun sendSOS(@Body request: SOSRequest): Call<GenericResponse>

    @DELETE("api/emergency/contact/delete/{contact_id}")
    fun deleteEmergencyContact(@Path("contact_id") contactId: Int): Call<GenericResponse>

    @GET("api/hospitals")
    fun getHospitals(): Call<List<Hospital>>

    @GET("api/doctors")
    fun getDoctors(@Query("specialization") specialization: String? = null): Call<List<DoctorResponse>>

    @GET("api/doctor/patient-records/{patientId}")
    fun getPatientReports(
        @Path("patientId") patientId: Int,
        @Query("user_id") doctorId: Int,
        @Query("role") role: String = "doctor"
    ): Call<List<MedicalReport>>

    @GET("api/doctor/patients")
    fun getDoctorPatients(
        @Query("user_id") doctorId: Int,
        @Query("role") role: String = "doctor"
    ): Call<List<Patient>>

    @GET("api/doctor/availability/{doctor_id}")
    fun getDoctorAvailability(@Path("doctor_id") doctorId: Int): Call<List<AvailabilityResponse>>

    @POST("api/doctor/availability")
    fun postAvailability(@Body request: PostAvailabilityRequest): Call<GenericResponse>

    @POST("api/doctor/availability/block")
    fun postBulkAvailability(@Body request: PostBulkAvailabilityRequest): Call<GenericResponse>

    @POST("api/system/cleanup-slots")
    fun cleanupSlots(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @DELETE("api/doctor/availability/{slot_id}")
    fun deleteAvailability(
        @Path("slot_id") slotId: Int,
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<GenericResponse>

    @POST("api/appointment/book")
    fun bookAppointment(@Body request: BookAppointmentRequest): Call<BookingResponse>

    @GET("api/my-appointments")
    fun getMyAppointments(
        @Query("user_id") userId: Int,
        @Query("role") role: String,
        @Query("status") status: String? = null
    ): Call<List<AppointmentResponse>>

    @GET("api/doctor/{doctor_id}")
    fun getDoctorDetails(@Path("doctor_id") doctorId: Int): Call<DoctorResponse>

    @GET("api/appointment/{appointment_id}")
    fun getAppointmentDetails(@Path("appointment_id") appointmentId: Int): Call<AppointmentResponse>

    @GET("api/consultation/status/{appointment_id}")
    fun getConsultationStatus(
        @Path("appointment_id") appointmentId: Int
    ): Call<ConsultationStatusResponse>

    @GET("api/video/token")
    fun getVideoToken(
        @Query("user_id") userId: Int,
        @Query("channel_name") channelName: String,
        @Query("role") role: String
    ): Call<VideoTokenResponse>

    @POST("api/consultation/start")
    fun startConsultation(
        @Body request: ConsultationStartRequest
    ): Call<ConsultationStartResponse>

    @PUT("api/consultation/ready/{appointment_id}")
    fun setConsultationReadyPut(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): Call<ConsultationStartResponse>

    @PUT("api/consultation/end/{appointment_id}")
    fun endConsultation(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    @GET("api/medical-records")
    fun getMedicalRecords(
        @Query("user_id") userId: Int,
        @Query("role") role: String,
        @Query("appointment_id") appointmentId: Int? = null,
        @Query("patient_id") patientId: Int? = null
    ): Call<List<MedicalRecordResponse>>

    @Multipart
    @POST("api/patient/upload-medical-record")
    fun uploadMedicalRecord(
        @Part("user_id") userId: RequestBody,
        @Part("role") role: RequestBody,
        @Part("record_type") recordType: RequestBody,
        @Part("patient_id") patientId: RequestBody? = null,
        @Part("doctor_id") doctorId: RequestBody? = null,
        @Part("appointment_id") appointmentId: RequestBody? = null,
        @Part file: MultipartBody.Part
    ): Call<GenericResponse>

    @GET("api/medical-record/download/{record_id}")
    @Streaming
    fun downloadMedicalRecord(
        @Path("record_id") recordId: Int,
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<ResponseBody>

    @POST("api/medical-record/request")
    fun requestMedicalRecord(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @POST("api/medical-record/approve/{record_id}")
    fun approveMedicalRecord(
        @Path("record_id") recordId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    @POST("api/medical-record/share")
    fun shareRecordsToAppointment(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("api/profile")
    fun getProfile(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<ProfileResponse>

    @PUT("api/profile/update")
    fun updateProfile(@Body body: UpdateProfileRequest): Call<GenericResponse>

    @DELETE("api/account/delete")
    fun deleteAccount(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<GenericResponse>

    @GET("api/privacy-info")
    fun getPrivacyInfo(): Call<Map<String, String>>

    @GET("api/devices")
    fun getDevices(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<DeviceResponse>>

    @DELETE("api/device/delete/{device_id}")
    fun deleteDevice(@Path("device_id") deviceId: Int): Call<GenericResponse>

    @DELETE("api/devices/logout-all")
    fun logoutAllDevices(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("api/login-activity")
    fun getLoginActivity(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<LoginHistoryResponse>>

    // Admin Endpoints
    @GET("api/admin/dashboard-summary")
    fun getDashboardSummary(): Call<DashboardSummary>

    @GET("api/admin/revenue-trend")
    fun getRevenueTrendV2(): Call<List<RevenueTrend>>

    @GET("api/admin/appointments-week")
    fun getWeeklyAppointmentsV2(): Call<List<WeeklyAppointments>>

    @GET("api/admin/doctor-activity")
    fun getDoctorActivity(): Call<List<DoctorActivity>>

    @GET("api/admin/patient-registrations")
    fun getPatientRegistrations(): Call<List<PatientRegistration>>

    @GET("api/admin/doctors/pending")
    fun getPendingDoctorsV2(): Call<List<Doctor>>

    @GET("api/admin/doctors/pending")
    fun getPendingDoctors(
        @Query("user_id") userId: Int,
        @Query("role") role: String = "admin"
    ): Call<List<PendingDoctorResponse>>

    @GET("api/doctors")
    fun getAllDoctorsV2(): Call<List<Doctor>>

    @GET("api/admin/patients")
    fun getAllPatientsV2(): Call<List<Patient>>

    @GET("api/admin/appointments")
    fun getAllAppointmentsV2(@Query("role") role: String): Call<List<Appointment>>

    @GET("api/admin/doctors")
    fun getAllDoctors(
        @Query("user_id") userId: Int,
        @Query("role") role: String = "admin"
    ): Call<List<AdminDoctorResponse>>

    @GET("api/admin/users")
    fun getAllPatients(
        @Query("user_id") userId: Int,
        @Query("role") role: String = "admin"
    ): Call<List<AdminPatientResponse>>

    @GET("api/admin/appointments")
    fun getAllAppointments(
        @Query("user_id") userId: Int,
        @Query("role") role: String = "admin"
    ): Call<List<AdminAppointmentResponse>>

    @GET("api/admin/payments")
    fun getAdminPayments(
        @Query("role") role: String = "admin"
    ): Call<List<AdminPaymentResponse>>

    @PUT("api/appointment/cancel/{appointment_id}")
    fun cancelAppointment(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    @PUT("api/admin/appointment/reassign/{appointment_id}")
    fun reassignAppointment(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    @PUT("api/admin/doctors/approve/{doctor_id}")
    fun approveDoctor(
        @Path("doctor_id") doctorId: Int,
        @Body body: Map<String, String>
    ): Call<GenericResponse>

    @PUT("api/admin/doctors/reject/{doctor_id}")
    fun rejectDoctor(
        @Path("doctor_id") doctorId: Int,
        @Body body: Map<String, String>
    ): Call<GenericResponse>

    @GET("api/notifications")
    fun getNotifications(
        @Query("user_id") userId: Int,
        @Query("role") role: String,
        @Query("type") type: String? = null
    ): Call<List<NotificationResponse>>

    @PUT("api/notification/read/{notif_id}")
    fun markNotificationRead(@Path("notif_id") notifId: Int): Call<ApiResponse>

    @PUT("api/notifications/read-all")
    fun markAllNotificationsRead(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @POST("api/notification/create")
    fun createNotification(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("api/chat/{appointment_id}")
    fun getChatMessages(
        @Path("appointment_id") appointmentId: Int,
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<ChatMessageResponse>>

    @POST("api/chat/send/{appointment_id}")
    fun sendChatMessage(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    @POST("api/consultation/ready")
    fun setConsultationReady(@Body request: ConsultationReadyRequest): Call<GenericResponse>

    @POST("api/consultation/end")
    fun endConsultationV2(
        @Body request: ConsultationEndRequest
    ): Call<GenericResponse>

    @GET("api/consultation/poll/{appointment_id}")
    fun pollConsultation(
        @Path("appointment_id") appointmentId: Int,
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<ConsultationStatusResponse>

    @POST("api/prescriptions/create")
    fun createPrescription(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Call<ApiResponse>

    @POST("api/prescription/create/{appointment_id}")
    fun createPrescriptionLegacy(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<CreatePrescriptionResponse>

    @POST("api/prescription/{prescription_id}/add-medicine")
    fun addMedicine(
        @Path("prescription_id") prescriptionId: Int,
        @Body request: AddMedicineRequest
    ): Call<GenericResponse>

    @PUT("api/prescription/{prescription_id}/mark-ready")
    fun markPrescriptionReady(
        @Path("prescription_id") prescriptionId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    @GET("api/prescription/{appointment_id}")
    fun getPrescription(
        @Path("appointment_id") appointmentId: Int,
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<PrescriptionResponse>

    @GET("api/prescription/status/{appointment_id}")
    fun getPrescriptionStatus(@Path("appointment_id") appointmentId: Int): Call<PrescriptionStatusResponse>

    @GET("api/prescription/history")
    fun getPrescriptionHistory(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<PrescriptionHistoryResponse>>

    @GET("api/patient/prescriptions")
    fun getPatientPrescriptions(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<PrescriptionHistoryResponse>>

    @POST("api/consultation/chat/send")
    fun sendConsultationChat(@Body request: ConsultationChatSendRequest): Call<GenericResponse>

    @GET("api/consultation/chat/{consultation_id}")
    fun getConsultationChat(@Path("consultation_id") consultationId: Int): Call<List<ConsultationChatMessageResponse>>

    @GET("api/patient/profile/{patient_id}")
    fun getPatientProfileV2(@Path("patient_id") patientId: Int): Call<PatientProfileV2Response>

    @GET("api/patient/history/{patient_id}")
    fun getPatientHistory(@Path("patient_id") patientId: Int): Call<List<PatientHistoryResponse>>

    @GET("api/patient/reports/{patient_id}")
    fun getPatientReportsV2(@Path("patient_id") patientId: Int): Call<List<PatientReportResponse>>

    @POST("api/prescription/create")
    fun createPrescriptionV3(@Body request: CreatePrescriptionRequestV2): Call<GenericResponse>

    @GET("api/prescription/consultation/{consultation_id}")
    fun getPrescriptionByConsultation(@Path("consultation_id") consultationId: Int): Call<PrescriptionDetailResponse>

    @POST("api/consultation/reconnect")
    fun reconnectConsultation(@Body request: ReconnectRequest): Call<VideoTokenResponse>

    @POST("api/consultation/record/start")
    fun startRecording(@Body body: Map<String, Int>): Call<GenericResponse>

    // Medication Reminder Endpoints
    @POST("api/reminder/add")
    fun addMedicationReminder(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("api/reminders")
    fun getMedicationReminders(@Query("user_id") userId: Int): Call<List<MedicationReminderResponse>>

    @PUT("api/reminder/complete/{reminder_id}")
    fun completeMedicationReminder(@Path("reminder_id") reminderId: Int): Call<GenericResponse>

    @POST("api/wallet/add")
    fun addWalletMoney(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<WalletResponse>

    @GET("api/wallet/history")
    fun getWalletHistory(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<WalletHistoryResponse>>

    @POST("api/payment-method/add")
    fun addPaymentMethod(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("api/payment-methods")
    fun listPaymentMethods(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<PaymentMethodResponse>>
}
