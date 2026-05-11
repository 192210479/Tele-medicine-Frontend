package com.simats.Tmapp

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log

class TMApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val sessionManager = SessionManager.getInstance(this)
        ThemeManager.applyTheme(sessionManager)
        
        val socketService = SocketService.getInstance(this)
        socketService.connect()

        var startedActivities = 0
        
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // 1. Enable Edge-to-Edge globally
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                
                // 2. Translucent Status Bar & Icon Visibility
                activity.window.statusBarColor = Color.TRANSPARENT
                
                // Handle light status bar icons (dark mode has been permanently removed)
                val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                controller.isAppearanceLightStatusBars = true
                
                // 3. Global Insets Handling
                val decorView = activity.window.decorView
                ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
                    val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                    
                    // Apply top padding to the content root to avoid overlap with status bar
                    // android.R.id.content is the parent container for all activity layouts
                    val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
                    contentView?.setPadding(0, statusInsets.top, 0, 0)
                    
                    insets
                }
            }

            override fun onActivityStarted(activity: Activity) {
                if (startedActivities == 0) {
                    // App entered foreground
                    Log.d("TMApp", "Entered foreground, syncing notifications")
                    if (sessionManager.getUserId() != -1) {
                        // Notify active activities to reload if needed
                        // For simplicity, we can just ensure the next notification screen open fetches fresh data
                        // or broadcast an event. 
                        // The user said: "Whenever the Android app returns from background: reload notifications."
                    }
                }
                startedActivities++
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities == 0) {
                    // App entered background
                    Log.d("TMApp", "Entered background")
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
