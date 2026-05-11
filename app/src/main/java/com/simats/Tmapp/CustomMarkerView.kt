package com.simats.Tmapp

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(context: Context, layoutResource: Int, private val labels: List<String>) : 
    MarkerView(context, layoutResource) {

    private val tvLabel: TextView = findViewById(R.id.tvMarkerLabel)
    private val tvValue: TextView = findViewById(R.id.tvMarkerValue)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val index = it.x.toInt()
            if (index >= 0 && index < labels.size) {
                tvLabel.text = labels[index]
            } else {
                tvLabel.text = "Data"
            }
            
            // Format value: if it's revenue, add ₹
            val valueText = if (it.y >= 1000) {
                String.format("₹%,.0f", it.y)
            } else {
                it.y.toInt().toString()
            }
            tvValue.text = valueText
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
