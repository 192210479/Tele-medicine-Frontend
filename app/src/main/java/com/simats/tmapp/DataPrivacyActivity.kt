package com.simats.tmapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import androidx.activity.viewModels

class DataPrivacyActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_privacy)

        // Close activity on back arrow click
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { 
            finish() 
        }

        // Close activity on "I Understand" button click
        findViewById<MaterialButton>(R.id.btnIUnderstand).setOnClickListener { 
            finish() 
        }

        // Fetch Data from Backend
        viewModel.fetchPrivacyInfo()
        viewModel.privacyInfo.observe(this) { text ->
            if (text != null) {
                try {
                    val root = findViewById<android.view.View>(R.id.vHeaderDivider).parent as android.view.ViewGroup
                    var scrollView: androidx.core.widget.NestedScrollView? = null
                    for (i in 0 until root.childCount) {
                        if (root.getChildAt(i) is androidx.core.widget.NestedScrollView) {
                            scrollView = root.getChildAt(i) as androidx.core.widget.NestedScrollView
                            break
                        }
                    }
                    val linearLayout = scrollView?.getChildAt(0) as? android.widget.LinearLayout
                    
                    if (linearLayout != null && linearLayout.childCount > 0) {
                        // Title is index 0. We'll keep it and replace the rest
                        linearLayout.removeViews(1, linearLayout.childCount - 1)
                        val tvNewPrivacy = android.widget.TextView(this@DataPrivacyActivity).apply {
                            setTextColor(android.graphics.Color.parseColor("#475569"))
                            textSize = 15f
                            setLineSpacing(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 4f, resources.displayMetrics), 1f)
                            this.text = text
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { topMargin = 32 }
                        }
                        linearLayout.addView(tvNewPrivacy)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
