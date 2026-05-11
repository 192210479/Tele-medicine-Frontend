package com.simats.Tmapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        
        sessionManager = SessionManager.getInstance(this)

        val rootView = findViewById<View>(android.R.id.content)
        rootView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_scale))

        // Floating background icons
        animateFloat(findViewById(R.id.ivHeartTopLeft), 8f, 2600L, 0L)
        animateFloat(findViewById(R.id.ivStethoscope), 10f, 3000L, 400L)
        animateFloat(findViewById(R.id.ivPill), 12f, 2800L, 800L)
        animateFloat(findViewById(R.id.ivHeartBottomRight), 9f, 2400L, 200L)

        // Reset session for testing purposes to see the full flow
        // Comment this out once you're satisfied with the flow
        // sessionManager.logout()

        // Delay for 3000ms and then decide destination
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user has seen onboarding
            val hasSeenOnboarding = sessionManager.hasSeenOnboarding()
            
            // Flow: Splash -> Onboarding (if not seen) -> Welcome -> Login -> Dashboard
            val targetActivity = if (!hasSeenOnboarding) {
                Onboarding1Activity::class.java
            } else {
                AuthWelcomeActivity::class.java
            }

            val intent = Intent(this, targetActivity)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }, 3000)
    }

    private fun animateFloat(view: View?, distance: Float, cycleDuration: Long, delay: Long) {
        view?.let {
            val animator = ObjectAnimator.ofFloat(it, View.TRANSLATION_Y, 0f, -distance)
            animator.duration = cycleDuration / 2
            animator.repeatCount = ValueAnimator.INFINITE
            animator.repeatMode = ValueAnimator.REVERSE
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.startDelay = delay
            animator.start()
        }
    }
}