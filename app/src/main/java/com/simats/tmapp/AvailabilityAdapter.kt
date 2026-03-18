package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.AvailabilityResponse

class AvailabilityAdapter(
    private var slots: List<AvailabilityResponse>,
    private val onDeleteClick: (AvailabilityResponse) -> Unit
) : RecyclerView.Adapter<AvailabilityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_availability_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val slot = slots[position]
        holder.tvDate.text = slot.date
        holder.tvTime.text = slot.time_slot
        holder.ivDelete.setOnClickListener { onDeleteClick(slot) }
    }

    override fun getItemCount() = slots.size

    fun updateList(newList: List<AvailabilityResponse>) {
        slots = newList
        notifyDataSetChanged()
    }
}
