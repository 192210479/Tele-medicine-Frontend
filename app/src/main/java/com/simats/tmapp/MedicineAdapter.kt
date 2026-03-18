package com.simats.tmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.tmapp.api.MedicineRequest

class MedicineAdapter(
    private val medicines: MutableList<MedicineRequest>
) : RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMedicineName)
        val tvDosage: TextView = view.findViewById(R.id.tvDosage)
        val tvFrequencyBase: TextView = view.findViewById(R.id.tvFrequency)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val medicine = medicines[position]
        holder.tvName.text = medicine.name
        holder.tvDosage.text = medicine.dosage
        holder.tvFrequencyBase.text = "${medicine.frequency} | ${medicine.duration}"
        
        holder.ivDelete.setOnClickListener {
            medicines.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, medicines.size)
        }
    }

    override fun getItemCount() = medicines.size
}
