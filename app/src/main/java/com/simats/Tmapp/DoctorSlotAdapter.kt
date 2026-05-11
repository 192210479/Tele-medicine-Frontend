package com.simats.Tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.AvailabilityResponse

class DoctorSlotAdapter(
    private val onDeleteSlot: (AvailabilityResponse) -> Unit,
    private val onCancelSlot: (AvailabilityResponse) -> Unit
) : RecyclerView.Adapter<DoctorSlotAdapter.SlotViewHolder>() {

    private var slots: List<AvailabilityResponse> = emptyList()

    fun updateList(newList: List<AvailabilityResponse>) {
        slots = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doctor_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(slots[position])
    }

    override fun getItemCount(): Int = slots.size

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvSlotDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvSlotTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvSlotStatus)
        private val btnAction: MaterialButton = itemView.findViewById(R.id.btnSlotAction)

        fun bind(slot: AvailabilityResponse) {
            val displayTime = if (slot.utcTime != null) {
                TimeUtils.convertIsoUtcToLocal(slot.utcTime)
            } else {
                slot.time
            }

            tvDate.text = slot.date
            tvTime.text = displayTime
            
            val today = java.time.LocalDate.now()
            val localizedDateStr = if (slot.utcTime != null) TimeUtils.convertIsoUtcToLocalDate(slot.utcTime) else slot.date
            val slotDate = try { java.time.LocalDate.parse(localizedDateStr ?: "") } catch (e: Exception) { null }
            val isPast = slotDate != null && slotDate.isBefore(today)

            // Status logic from prompt: status field or is_booked
            var status = slot.status ?: if (slot.is_booked == 1) "Booked" else "Available"
            
            if (isPast) {
                status = "Expired"
                btnAction.visibility = View.GONE
            } else {
                btnAction.visibility = View.VISIBLE
                if (status.equals("Available", true)) {
                    btnAction.text = "Delete Slot"
                    btnAction.setBackgroundColor(itemView.context.getColor(android.R.color.holo_red_light))
                    btnAction.setOnClickListener { onDeleteSlot(slot) }
                } else {
                    btnAction.text = "Cancel Slot"
                    btnAction.setBackgroundColor(itemView.context.getColor(android.R.color.holo_orange_light))
                    btnAction.setOnClickListener { onCancelSlot(slot) }
                }
            }
            tvStatus.text = status
        }
    }
}
