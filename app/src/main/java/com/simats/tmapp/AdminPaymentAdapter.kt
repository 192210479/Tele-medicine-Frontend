package com.simats.tmapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.AdminPaymentResponse

class AdminPaymentAdapter(private var payments: List<AdminPaymentResponse>) :
    RecyclerView.Adapter<AdminPaymentAdapter.PaymentViewHolder>() {

    class PaymentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView = view.findViewById(R.id.tvTransactionTitle)
        val tvPatientId: TextView = view.findViewById(R.id.tvTransactionStatus)
        val tvAmount: TextView = view.findViewById(R.id.tvTransactionAmount)
        val tvDate: TextView = view.findViewById(R.id.tvTransactionDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]
        holder.tvId.text = "Payment ID: ${payment.id}"
        holder.tvPatientId.text = "Patient ID: ${payment.patientId}"
        holder.tvAmount.text = "₹${payment.amount}"
        holder.tvDate.text = payment.date
        
        // Use status for coloring if needed
        if (payment.status.lowercase() == "success" || payment.status.lowercase() == "completed") {
            holder.tvAmount.setTextColor(Color.parseColor("#10B981"))
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#EF4444"))
        }
    }

    override fun getItemCount() = payments.size

    fun updateData(newPayments: List<AdminPaymentResponse>) {
        payments = newPayments
        notifyDataSetChanged()
    }
}
