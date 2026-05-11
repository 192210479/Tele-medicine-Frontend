package com.simats.Tmapp.api

import com.google.gson.annotations.SerializedName

data class AITriageRequest(
    @SerializedName("symptoms") val symptoms: String
)

data class AITriageResponse(
    @SerializedName("conditions") val conditions: List<String>?,
    @SerializedName("specialization") val specialization: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("disclaimer") val disclaimer: String?
)

data class AIRecommendRequest(
    @SerializedName("specialization") val specialization: String
)

data class MedicineParam(
    @SerializedName("name") val name: String,
    @SerializedName("dosage") val dosage: String
)

data class AIExplainPrescriptionRequest(
    @SerializedName("medicines") val medicines: List<MedicineParam>
)

data class ExplainedMedicine(
    @SerializedName("name") val name: String?,
    @SerializedName("dosage") val dosage: String?,
    @SerializedName("purpose") val purpose: String?,
    @SerializedName("precaution") val precaution: String?
)

data class AIExplainPrescriptionResponse(
    @SerializedName("summary") val summary: String?,
    @SerializedName("medicines") val medicines: List<ExplainedMedicine>?,
    @SerializedName("disclaimer") val disclaimer: String?
)
