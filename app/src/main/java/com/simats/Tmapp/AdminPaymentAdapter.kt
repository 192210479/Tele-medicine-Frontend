package com.simats.Tmapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.Tmapp.api.TransactionResponse

class AdminPaymentAdapter(private var transactions: List<TransactionResponse>) :
    RecyclerView.Adapter<AdminPaymentAdapter.PaymentViewHolder>() {

    private var onItemClick: ((TransactionResponse) -> Unit)? = null

    fun setOnItemClickListener(listener: (TransactionResponse) -> Unit) {
        onItemClick = listener
    }

    class PaymentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView = view.findViewById(R.id.tvTransactionTitle)
        val tvSub: TextView = view.findViewById(R.id.tvTransactionStatus)
        val tvAmount: TextView = view.findViewById(R.id.tvTransactionAmount)
        val tvDate: TextView = view.findViewById(R.id.tvTransactionDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val trans = transactions[position]

        val patientLabel = if (!trans.patientName.isNullOrEmpty()) trans.patientName else "Patient"
        val doctorLabel = if (!trans.doctorName.isNullOrEmpty()) "Dr. ${trans.doctorName}" else "Doctor"

        holder.tvId.text = "#${trans.id} — $patientLabel"
        val method = trans.paymentMethod ?: "Razorpay"
        val status = trans.status?.replaceFirstChar { it.uppercase() } ?: "Unknown"

        holder.tvSub.text = "$doctorLabel | $method | $status"

        holder.tvAmount.text = try {
            val fmt = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN"))
            fmt.format(trans.amount ?: 0.0)
        } catch (e: Exception) {
            "₹${trans.amount ?: 0.0}"
        }

        val rawDate = trans.date ?: ""

        holder.tvDate.text = try {
            if (rawDate.isNotEmpty()) {
                TimeUtils.convertUtcToLocal(
                    rawDate,
                    outputPattern = "dd MMM yyyy, hh:mm a"
                )
            } else {
                ""
            }
        } catch (e: Exception) {
            rawDate
        }

        val statusLower = trans.status?.lowercase() ?: ""
        val color = when (statusLower) {
            "success" -> Color.parseColor("#10B981")   // green
            "refunded" -> Color.parseColor("#F59E0B")  // orange
            "failed" -> Color.parseColor("#EF4444")    // red
            "pending" -> Color.parseColor("#3B82F6")   // blue
            else -> Color.parseColor("#64748B")        // grey
        }

        holder.tvAmount.setTextColor(color)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(trans)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<TransactionResponse>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}