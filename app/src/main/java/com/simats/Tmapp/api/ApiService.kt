package com.simats.Tmapp.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("api/login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("api/register/doctor")
    fun registerDoctor(@Body doctorRegisterRequest: DoctorRegisterRequest): Call<RegisterResponse>

    @Multipart
    @POST("api/register/doctor")
    fun registerDoctorMultipart(
        @Part("full_name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("specialization") specialization: RequestBody,
        @Part("license_number") licenseNumber: RequestBody,
        @Part("experience_years") experience: RequestBody?,
        @Part("fee") fee: RequestBody?,
        @Part("languages") languages: RequestBody?,
        @Part("bio") bio: RequestBody?,
        @Part license_file: MultipartBody.Part,
        @Part medical_record_file: MultipartBody.Part
    ): Call<RegisterResponse>

    @Multipart
    @POST("api/doctor/update-documents")
    fun updateDoctorDocuments(
        @Part("user_id") userId: RequestBody,
        @Part("license_file") license_file: MultipartBody.Part? = null,
        @Part("medical_record_file") medical_record_file: MultipartBody.Part? = null
    ): Call<GenericResponse>

    @POST("api/register/patient")
    fun registerPatient(@Body patientRegisterRequest: PatientRegisterRequest): Call<RegisterResponse>

    // Password Reset Endpoints
    @POST("api/password/send-otp")
    fun sendOtp(@Body body: Map<String, String>): Call<GenericResponse>

    @POST("api/password/verify-otp")
    fun verifyOtp(@Body body: Map<String, String>): Call<GenericResponse>

    @PUT("api/password/reset")
    fun resetPassword(@Body body: Map<String, String>): Call<GenericResponse>

    @PUT("api/password/change")
    fun changePassword(@Body request: ChangePasswordRequest): Call<GenericResponse>

    // Emergency Endpoints
    @GET("api/emergency/contacts")
    fun getEmergencyContacts(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<EmergencyContact>>

    @POST("api/emergency/contact/add")
    fun addEmergencyContact(@Body contact: AddEmergencyContactRequest): Call<GenericResponse>

    @DELETE("api/emergency/contact/delete/{contact_id}")
    fun deleteEmergencyContact(@Path("contact_id") contactId: Int): Call<GenericResponse>

    @GET("api/hospitals")
    fun getHospitals(@Query("lat") lat: Double? = null, @Query("lon") lon: Double? = null): Call<List<Hospital>>

    @POST("api/emergency/alert")
    fun sendEmergencyAlert(@Body alertRequest: EmergencyAlertRequest): Call<EmergencyAlertResponse>

    @POST("api/emergency/location")
    fun shareLocation(@Body request: ShareLocationRequest): Call<ShareLocationResponse>

    @POST("api/emergency/sos")
    fun sendSOS(@Body request: SOSRequest): Call<GenericResponse>

    // Appointment Booking
    @GET("api/doctors")
    fun getDoctors(@Query("specialization") specialization: String? = null): Call<List<DoctorResponse>>

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
    
    @POST("api/payments/create-order")
    fun createPaymentOrder(
        @Body request: CreatePaymentOrderRequest
    ): Call<CreatePaymentOrderResponse>

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

    @PUT("api/consultation/ready/{appointment_id}")
    fun setConsultationReadyPut(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): Call<ConsultationStartResponse>

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

    @PUT("api/consultation/end/{appointment_id}")
    fun endConsultation(
        @Path("appointment_id") appointmentId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    // Medical Records
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
        @Part("patient_id") patientId: RequestBody?,
        @Part("doctor_id") doctorId: RequestBody?,
        @Part("appointment_id") appointmentId: RequestBody?,
        @Part file: List<MultipartBody.Part>
    ): Call<GenericResponse>

    @GET("api/medical-record/download/{record_id}")
    @Streaming
    fun downloadMedicalRecord(
        @Path("record_id") recordId: Int,
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<okhttp3.ResponseBody>

    @POST("api/medical-record/request")
    fun requestMedicalRecord(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @POST("api/medical-record/approve/{record_id}")
    fun approveMedicalRecord(
        @Path("record_id") recordId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    @POST("api/medical-record/share-bulk")
    fun shareRecordsToAppointment(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    // Profile
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

    @Multipart
    @POST("api/profile/image/upload")
    fun uploadProfileImage(
        @Part("user_id") userId: okhttp3.RequestBody,
        @Part("role") role: okhttp3.RequestBody,
        @Part image: MultipartBody.Part
    ): Call<Map<String, Any>>

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
    fun getRevenueTrend(): Call<List<RevenueTrend>>

    @GET("api/admin/appointments-week")
    fun getWeeklyAppointments(): Call<List<WeeklyAppointments>>

    @GET("api/admin/doctor-activity")
    fun getDoctorActivity(): Call<List<DoctorActivity>>

    @GET("api/admin/patient-registrations")
    fun getPatientRegistrations(): Call<List<PatientRegistration>>

    @GET("api/admin/patients")
    fun getAllPatients(
        @Query("role") role: String = "admin"
    ): Call<List<Patient>>

    @GET("api/admin/appointments")
    fun getAllAppointmentsV2(
        @Query("role") role: String = "admin"
    ): Call<List<Appointment>>

    @GET("api/admin/doctors")
    fun getAllDoctors(
        @Query("role") role: String = "admin"
    ): Call<List<Doctor>>

    @GET("api/admin/doctors/pending")
    fun getPendingDoctorsV2(
        @Query("role") role: String = "admin"
    ): Call<List<Doctor>>

    @GET("api/admin/doctors/approved")
    fun getApprovedDoctors(
        @Query("role") role: String = "admin"
    ): Call<List<Doctor>>

    @GET("api/admin/doctors/rejected")
    fun getRejectedDoctors(
        @Query("role") role: String = "admin"
    ): Call<List<Doctor>>

    @GET("api/admin/doctors/all")
    fun getAdminDoctorsFull(
        @Query("role") role: String = "admin"
    ): Call<List<Doctor>>

    @GET("api/admin/patients/all")
    fun getAdminPatientsFull(
        @Query("role") role: String = "admin"
    ): Call<List<Patient>>

    @PUT("api/admin/doctors/approve/{doctor_id}")
    fun approveDoctorV2(
        @Path("doctor_id") doctorId: Int,
        @Body body: Map<String, String>
    ): Call<GenericResponse>

    @PUT("api/admin/doctors/reject/{doctor_id}")
    fun rejectDoctorV2(
        @Path("doctor_id") doctorId: Int,
        @Body body: Map<String, String>
    ): Call<GenericResponse>

    @DELETE("api/admin/user/delete/{user_id}")
    fun deleteUser(
        @Path("user_id") userId: Int,
        @Query("role") role: String = "admin"
    ): Call<GenericResponse>

    @DELETE("api/admin/doctor/delete/{doctor_id}")
    fun deleteDoctor(
        @Path("doctor_id") doctorId: Int,
        @Query("role") role: String = "admin"
    ): Call<GenericResponse>

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

    @GET("api/admin/notifications")
    fun getAdminNotifications(
        @Query("role") role: String = "admin"
    ): Call<List<NotificationResponse>>

    // Wallet Endpoints
    @POST("api/wallet/add")
    fun addWalletMoney(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<WalletResponse>

    @GET("api/wallet/history")
    fun getWalletHistory(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<WalletHistoryResponse>>

    // Payment Methods Endpoints
    @POST("api/payment-method/add")
    fun addPaymentMethod(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("api/payment-methods")
    fun listPaymentMethods(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<PaymentMethodResponse>>

    // Notification Endpoints
    @GET("api/notifications")
    fun getNotifications(
        @Query("user_id") userId: Int,
        @Query("role") role: String,
        @Query("type") type: String? = null
    ): Call<NotificationsWrapper>

    @PUT("api/notification/read/{notif_id}")
    fun markNotificationRead(@Path("notif_id") notifId: Int): Call<ApiResponse>

    @PUT("api/notifications/read-all")
    fun markAllNotificationsRead(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @POST("api/notification/create")
    fun createNotification(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    // Chat Endpoints
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

    // Medication Reminder Endpoints
    @POST("api/reminder/add")
    fun addMedicationReminder(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    @GET("api/reminders")
    fun getMedicationReminders(@Query("user_id") userId: Int): Call<List<MedicationReminderResponse>>

    @PUT("api/reminder/complete/{reminder_id}")
    fun completeMedicationReminder(@Path("reminder_id") reminderId: Int): Call<GenericResponse>

    @POST("api/consultation/ready")
    fun setConsultationReadyV2(
        @Body request: ConsultationReadyRequest
    ): Call<GenericResponse>

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

    // Prescription Endpoints

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

    // --- Step 4 Endpoints ---

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

    @GET("api/daily-health-tip")
    fun getDailyHealthTip(@Query("user_id") userId: Int): Call<HealthTipResponse>

    @GET("api/dashboard")
    fun getDashboardData(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<DashboardResponse>

    // AI Endpoints
    @POST("ai/symptom-triage")
    fun analyzeSymptoms(@Body request: AITriageRequest): Call<AITriageResponse>

    @POST("ai/recommend-doctor")
    fun recommendDoctor(@Body request: AIRecommendRequest): Call<List<DoctorResponse>>

    @POST("ai/explain-prescription")
    fun explainPrescription(@Body request: AIExplainPrescriptionRequest): Call<AIExplainPrescriptionResponse>

    // =================================================================
    // ==================== PAYMENTS & FINANCE (FULL) ==================
    // =================================================================

    // ── PATIENT: Create order (full body with patient_id/doctor_id) ────
    @POST("api/payments/create-order-simple")
    fun createPaymentOrderSimple(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<CreateOrderResponse>

    @POST("api/payments/create-order-legacy")
    fun createLegacyPaymentOrder(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<CreateOrderResponse>

    @POST("api/payments/create-order")
    fun createFullPaymentOrder(@Body request: CreateOrderRequest): Call<CreateOrderResponse>

    // ── PATIENT: Verify payment after Razorpay success (full body) ─────
    @POST("api/payments/verify")
    fun verifyPaymentFull(@Body request: VerifyPaymentRequest): Call<VerifyPaymentResponse>

    @POST("api/payments/verify")
    fun verifyPayment(@Body request: PaymentVerificationRequest): Call<PaymentVerificationResponse>

    // ── PATIENT: Billing / invoice history ────────────────────────────
    @GET("api/billing/history")
    fun getBillingHistory(@Query("patient_id") patientId: Int): Call<List<BillingHistoryItem>>

    // ── PATIENT: Invoice detail ────────────────────────────────────────
    @GET("api/invoice/{payment_id}")
    fun getInvoiceDetail(@Path("payment_id") paymentId: Int): Call<InvoiceDetailResponse>

    // ── PATIENT: Saved payment methods (new backend) ───────────────────
    @GET("api/payment-methods")
    fun getSavedPaymentMethods(@Query("patient_id") patientId: Int): Call<List<SavedPaymentMethod>>

    @POST("api/payment-methods/add")
    fun addSavedPaymentMethod(@Body request: AddSavedPaymentMethodRequest): Call<SavedPaymentMethod>

    @HTTP(method = "DELETE", path = "api/payment-methods/{method_id}", hasBody = true)
    fun deleteSavedPaymentMethod(
        @Path("method_id") methodId: Int,
        @Body request: DeletePaymentMethodRequest
    ): Call<GenericResponse>

    // ── PATIENT: Refund history ────────────────────────────────────────
    @GET("api/refunds/history")
    fun getRefundHistory(@Query("patient_id") patientId: Int): Call<List<RefundHistoryItem>>

    // ── PATIENT/ADMIN: Initiate refund ────────────────────────────────
    @POST("api/payments/refund")
    fun initiateRefund(@Body request: RefundRequest): Call<RefundResponse>

    // ── DOCTOR: Wallet summary (new) ───────────────────────────────────
    @GET("api/doctor/wallet")
    fun getDoctorWallet(@Query("doctor_id") doctorId: Int): Call<DoctorWalletResponse>

    // ── DOCTOR: Transaction history (enriched) ─────────────────────────
    @GET("api/doctor/transactions")
    fun getDoctorTransactions(
        @Query("doctor_id") doctorId: Int,
        @Query("type") type: String? = null
    ): Call<List<DoctorTransactionItem>>

    // ── DOCTOR: Bank accounts ──────────────────────────────────────────
    @GET("api/doctor/bank-accounts")
    fun getDoctorBankAccounts(@Query("doctor_id") doctorId: Int): Call<List<DoctorBankAccount>>

    @POST("api/doctor/bank-account/add")
    fun addDoctorBankAccount(@Body request: AddBankAccountRequest): Call<GenericResponse>

    @HTTP(method = "DELETE", path = "api/doctor/bank-account/{account_id}", hasBody = true)
    fun deleteDoctorBankAccount(
        @Path("account_id") accountId: Int,
        @Body request: DeleteBankAccountRequest
    ): Call<GenericResponse>

    // ── DOCTOR: Withdrawal ─────────────────────────────────────────────
    @POST("api/doctor/withdrawal/request")
    fun requestWithdrawal(@Body request: WithdrawalRequestBody): Call<WithdrawalResponse>

    @GET("api/doctor/withdrawal/history")
    fun getWithdrawalHistory(@Query("doctor_id") doctorId: Int): Call<List<WithdrawalHistoryItem>>

    // ── ADMIN: Finance summary ─────────────────────────────────────────
    @GET("api/admin/finance/summary")
    fun getAdminFinanceSummary(@Query("role") role: String = "admin"): Call<AdminFinanceSummaryResponse>

    // ── ADMIN: Payments list (enriched) ───────────────────────────────
    @GET("api/admin/finance/payments")
    fun getAdminFinancePayments(
        @Query("role") role: String = "admin",
        @Query("status") status: String? = null
    ): Call<List<AdminPaymentItem>>

    // ── ADMIN: Refunds list ────────────────────────────────────────────
    @GET("api/admin/finance/refunds")
    fun getAdminRefunds(
        @Query("role") role: String = "admin",
        @Query("status") status: String? = null
    ): Call<List<AdminRefundItem>>

    // ── ADMIN: Withdrawals list ────────────────────────────────────────
    @GET("api/admin/finance/withdrawals")
    fun getAdminWithdrawals(
        @Query("role") role: String = "admin",
        @Query("status") status: String? = null
    ): Call<List<AdminWithdrawalItem>>

    // ── ADMIN: Approve withdrawal ──────────────────────────────────────
    @POST("api/admin/withdrawal/approve/{withdrawal_id}")
    fun approveWithdrawal(
        @Path("withdrawal_id") withdrawalId: Int,
        @Body request: AdminWithdrawalActionRequest
    ): Call<AdminWithdrawalActionResponse>

    // ── ADMIN: Reject withdrawal ───────────────────────────────────────
    @POST("api/admin/withdrawal/reject/{withdrawal_id}")
    fun rejectWithdrawal(
        @Path("withdrawal_id") withdrawalId: Int,
        @Body request: AdminWithdrawalActionRequest
    ): Call<AdminWithdrawalActionResponse>

    // ── ADMIN: Analytics ──────────────────────────────────────────────
    @GET("api/admin/finance/analytics")
    fun getAdminFinanceAnalytics(
        @Query("role") role: String = "admin",
        @Query("period") period: String = "month"
    ): Call<AdminFinanceAnalytics>

    // ── ADMIN: Release pending balance ────────────────────────────────
    @POST("api/admin/wallet/release-pending")
    fun releasePendingBalance(@Body body: Map<String, @JvmSuppressWildcards Any>): Call<GenericResponse>

    // ── FALLBACK / LEGACY PAYMENTS ────────────────────────────────────
    @GET("api/patient/payments")
    fun getPatientPayments(@Query("patient_id") userId: Int): Call<List<TransactionResponse>>

    @GET("api/doctor/transactions")
    fun getDoctorTransactionsV2(@Query("doctor_id") userId: Int): Call<List<TransactionResponse>>

    @GET("api/admin/payments")
    fun getAdminPaymentsV2(): Call<List<TransactionResponse>>

    // ===========================
    // Help & Support APIs
    // ===========================

    @GET("api/support/faqs")
    fun getSupportFaqs(
        @Query("role") role: String
    ): Call<List<SupportFaqResponse>>

    @POST("api/support/ticket/create")
    fun createSupportTicket(
        @Body request: CreateSupportTicketRequest
    ): Call<SupportTicketCreateResponse>

    @GET("api/support/my-tickets")
    fun getMySupportTickets(
        @Query("user_id") userId: Int,
        @Query("role") role: String
    ): Call<List<SupportTicketResponse>>

    @GET("api/support/ticket/{ticket_id}")
    fun getSupportTicketDetails(
        @Path("ticket_id") ticketId: Int
    ): Call<SupportTicketDetailsResponse>

    @POST("api/support/ticket/reply/{ticket_id}")
    fun replySupportTicket(
        @Path("ticket_id") ticketId: Int,
        @Body request: ReplySupportTicketRequest
    ): Call<GenericResponse>

    @GET("api/admin/support/tickets")
    fun getAdminSupportTickets(
        @Query("status") status: String? = null,
        @Query("issue_type") issueType: String? = null
    ): Call<List<SupportTicketResponse>>

    @PUT("api/admin/support/ticket/status/{ticket_id}")
    fun updateSupportTicketStatus(
        @Path("ticket_id") ticketId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<GenericResponse>

    // ===========================
    // AI Support Chat APIs
    // ===========================

    @POST("api/support/ai-chat")
    fun sendAiSupportMessage(
        @Body request: AiSupportRequest
    ): Call<AiSupportResponse>

    // ===========================
    // Doctor Rating APIs
    // ===========================

    @GET("api/appointment/{appointmentId}/rating-status")
    fun getRatingStatus(
        @Path("appointmentId") appointmentId: Int,
        @Query("user_id") userId: Int
    ): Call<RatingStatusResponse>

    @POST("api/appointment/rate/{appointmentId}")
    fun submitRating(
        @Path("appointmentId") appointmentId: Int,
        @Body request: SubmitRatingRequest
    ): Call<com.google.gson.JsonObject>
}
