package com.simats.tmapp

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout

class BiometricIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageView: android.widget.ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_biometric_icon, this, true)
        imageView = findViewById(R.id.biometricImage)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val size = minOf(width, height)
            val padding = (size * 0.18).toInt() // 18% padding on each side = ~64% icon size
            imageView.setPadding(padding, padding, padding, padding)
        }
    }
}
