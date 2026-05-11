package com.simats.Tmapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.simats.Tmapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmergencyHelpActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var llContactsList: LinearLayout
    private lateinit var llHospitalsList: LinearLayout
    private lateinit var tvNoContacts: TextView
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (fineLocationGranted || coarseLocationGranted) {
            shareCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_help)

        sessionManager = SessionManager.getInstance(this)
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()

        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }

        // Initialize Views
        llContactsList = findViewById(R.id.llEmergencyContactsList)
        llHospitalsList = findViewById(R.id.llHospitalsList)
        tvNoContacts = findViewById(R.id.tvNoContacts)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnCallAmbulance).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:102")
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnCallAmbulance).setOnLongClickListener {
            sendEmergencyAlert(userId, role)
            true
        }

        findViewById<MaterialButton>(R.id.btnShareLocation).setOnClickListener {
            checkLocationPermissionAndShare()
        }

        findViewById<MaterialButton>(R.id.btnEmergencyContact).setOnClickListener {
            findViewById<android.widget.ScrollView>(R.id.scrollView)?.smoothScrollTo(0, llContactsList.top)
        }

        findViewById<ImageView>(R.id.ivRefreshHospitals)?.setOnClickListener {
            fetchHospitalsWithLocation()
        }

        findViewById<MaterialButton>(R.id.btnAddEmergencyContact).setOnClickListener {
            checkLimitAndShowDialog(userId, role)
        }

        setupBottomNav()

        // Initial Data Fetch
        fetchEmergencyContacts(userId, role)
        fetchHospitalsWithLocation()
    }

    private var progressDialog: android.app.ProgressDialog? = null

    private fun fetchHospitalsWithLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = locationManager.getProviders(true)
                var bestLocation: Location? = null
                for (provider in providers) {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                }
                
                if (bestLocation != null) {
                    currentLat = bestLocation.latitude
                    currentLon = bestLocation.longitude
                    fetchNearbyHospitals()
                } else {
                    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    val provider = if (isNetworkEnabled) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
                    locationManager.requestLocationUpdates(provider, 0L, 0f, object : LocationListener {
                        @SuppressLint("MissingPermission")
                        override fun onLocationChanged(location: Location) {
                            currentLat = location.latitude
                            currentLon = location.longitude
                            fetchNearbyHospitals()
                            locationManager.removeUpdates(this)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    })
                }
            } else {
                Toast.makeText(this, "Location permission required for full proximity search.", Toast.LENGTH_SHORT).show()
                fetchNearbyHospitals()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fetchNearbyHospitals()
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, PatientDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
        findViewById<LinearLayout>(R.id.navBook).setOnClickListener {
            startActivity(Intent(this, SelectDoctorActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, ConsultationHistoryActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun sendEmergencyAlert(userId: Int, role: String) {
        val request = EmergencyAlertRequest(userId, role, "Emergency triggered from App")
        ApiClient.instance.sendEmergencyAlert(request).enqueue(object : Callback<EmergencyAlertResponse> {
            override fun onResponse(call: Call<EmergencyAlertResponse>, response: Response<EmergencyAlertResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EmergencyHelpActivity, "Emergency Alert Sent!", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<EmergencyAlertResponse>, t: Throwable) {
                Toast.makeText(this@EmergencyHelpActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchEmergencyContacts(userId: Int, role: String) {
        ApiClient.instance.getEmergencyContacts(userId, role).enqueue(object : Callback<List<EmergencyContact>> {
            override fun onResponse(call: Call<List<EmergencyContact>>, response: Response<List<EmergencyContact>>) {
                if (response.isSuccessful) {
                    val contacts = response.body() ?: emptyList()
                    displayContacts(contacts)
                }
            }
            override fun onFailure(call: Call<List<EmergencyContact>>, t: Throwable) {
                Toast.makeText(this@EmergencyHelpActivity, "Failed to load contacts", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayContacts(contacts: List<EmergencyContact>) {
        llContactsList.removeAllViews()
        
        // Hide add button if limit reached
        val btnAdd = findViewById<MaterialButton>(R.id.btnAddEmergencyContact)
        if (contacts.size >= 3) {
            btnAdd.visibility = View.GONE
        } else {
            btnAdd.visibility = View.VISIBLE
        }

        if (contacts.isEmpty()) {
            tvNoContacts.visibility = View.VISIBLE
            return
        }
        tvNoContacts.visibility = View.GONE

        val inflater = LayoutInflater.from(this)
        for (contact in contacts) {
            val itemView = inflater.inflate(R.layout.item_emergency_contact, llContactsList, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvContactName)
            val tvPhone = itemView.findViewById<TextView>(R.id.tvContactPhone)
            val ivDelete = itemView.findViewById<ImageView>(R.id.ivDeleteContact)
            val flCall = itemView.findViewById<View>(R.id.flCallIcon)

            tvName.text = contact.name
            tvPhone.text = contact.phone

            flCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${contact.phone}")
                startActivity(intent)
            }

            ivDelete.setOnClickListener {
                deleteContact(contact.id, sessionManager.getUserId(), sessionManager.getUserRole().lowercase())
            }

            llContactsList.addView(itemView)
        }
    }

    private fun deleteContact(contactId: Int, userId: Int, role: String) {
        ApiClient.instance.deleteEmergencyContact(contactId).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EmergencyHelpActivity, "Contact deleted", Toast.LENGTH_SHORT).show()
                    fetchEmergencyContacts(userId, role)
                }
            }
            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@EmergencyHelpActivity, "Delete failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkLimitAndShowDialog(userId: Int, role: String) {
        ApiClient.instance.getEmergencyContacts(userId, role).enqueue(object : Callback<List<EmergencyContact>> {
            override fun onResponse(call: Call<List<EmergencyContact>>, response: Response<List<EmergencyContact>>) {
                if (response.isSuccessful) {
                    val contacts = response.body() ?: emptyList()
                    if (contacts.size >= 3) {
                        AlertDialog.Builder(this@EmergencyHelpActivity)
                            .setMessage("Maximum 3 emergency contacts allowed.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        showAddContactDialog(userId, role)
                    }
                } else {
                    Toast.makeText(this@EmergencyHelpActivity, "Failed to fetch contacts", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<EmergencyContact>>, t: Throwable) {
                Toast.makeText(this@EmergencyHelpActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddContactDialog(userId: Int, role: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etContactName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etContactPhone)
        val btnAdd = dialogView.findViewById<MaterialButton>(R.id.btnAddContact)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val phoneRegex = Regex("^[6-9][0-9]{9}$")
            
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                if (!phoneRegex.matches(phone)) {
                    Toast.makeText(this@EmergencyHelpActivity, "Enter a valid 10-digit phone number.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val body = AddEmergencyContactRequest(
                    userId = userId,
                    role = role,
                    name = name,
                    phone = phone
                )
                ApiClient.instance.addEmergencyContact(body).enqueue(object : Callback<GenericResponse> {
                    override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@EmergencyHelpActivity, "Emergency contact added.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            fetchEmergencyContacts(userId, role)
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                            Toast.makeText(this@EmergencyHelpActivity, "Failed to add: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                        Toast.makeText(this@EmergencyHelpActivity, "Failed to add: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun fetchNearbyHospitals() {
        ApiClient.instance.getHospitals(currentLat, currentLon).enqueue(object : Callback<List<Hospital>> {
            override fun onResponse(call: Call<List<Hospital>>, response: Response<List<Hospital>>) {
                if (response.isSuccessful) {
                    val hospitals = response.body() ?: emptyList()
                    displayHospitals(hospitals)
                } else {
                    displayHospitals(emptyList())
                }
            }
            override fun onFailure(call: Call<List<Hospital>>, t: Throwable) {
                displayHospitals(emptyList())
            }
        })
    }

    private fun displayHospitals(hospitals: List<Hospital>) {
        llHospitalsList.removeAllViews()
        if (hospitals.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "No nearby hospitals found"
            emptyText.setPadding(0, 20, 0, 20)
            emptyText.setTextColor(Color.GRAY)
            llHospitalsList.addView(emptyText)
            return
        }
        val inflater = LayoutInflater.from(this)
        for (hospital in hospitals) {
            val itemView = inflater.inflate(R.layout.item_hospital, llHospitalsList, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvHospitalName)
            val tvAddress = itemView.findViewById<TextView>(R.id.tvHospitalAddress)
            val tvDistance = itemView.findViewById<TextView>(R.id.tvHospitalDistance)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvHospitalStatus)
            val btnDirections = itemView.findViewById<MaterialButton>(R.id.btnGetDirections)

            tvName.text = hospital.name
            tvAddress.text = hospital.address
            tvDistance.text = hospital.distance
            tvStatus.text = hospital.status
            
            btnDirections.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${hospital.address}"))
                startActivity(intent)
            }
            llHospitalsList.addView(itemView)
        }
    }

    private fun checkLocationPermissionAndShare() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                shareCurrentLocation()
            } else {
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error checking location permissions", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun shareCurrentLocation() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Location services disabled", Toast.LENGTH_SHORT).show()
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                return
            }

            progressDialog = android.app.ProgressDialog(this).apply {
                setMessage("Retrieving location...")
                setCancelable(false)
                show()
            }

            // Try to get last known location first for immediate response
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            if (bestLocation != null) {
                sendLocationToBackend(bestLocation.latitude, bestLocation.longitude)
            } else {
                // Request fresh location if last known is not available
                val provider = if (isNetworkEnabled) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
                locationManager.requestLocationUpdates(provider, 0L, 0f, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        sendLocationToBackend(location.latitude, location.longitude)
                        locationManager.removeUpdates(this)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog?.dismiss()
            Toast.makeText(this, "Error getting current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendLocationToBackend(lat: Double, lon: Double) {
        try {
            val body = ShareLocationRequest(
                userId = sessionManager.getUserId(),
                role = sessionManager.getUserRole().lowercase(),
                latitude = lat,
                longitude = lon
            )
            ApiClient.instance.shareLocation(body).enqueue(object : Callback<ShareLocationResponse> {
                override fun onResponse(call: Call<ShareLocationResponse>, response: Response<ShareLocationResponse>) {
                    progressDialog?.dismiss()
                    if (response.isSuccessful) {
                        val res = response.body()
                        val mapLink = res?.mapLink ?: "https://www.google.com/maps?q=$lat,$lon"
                        
                        // Show Dialog with Map Link
                        AlertDialog.Builder(this@EmergencyHelpActivity)
                            .setTitle("Location Shared")
                            .setMessage("Your live location has been shared with your emergency contacts.\n\nLink: $mapLink")
                            .setPositiveButton("Open Map") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapLink))
                                startActivity(intent)
                            }
                            .setNegativeButton("Close", null)
                            .show()

                        // Load WebView with the map
                        val cvMap = findViewById<View>(R.id.cvMap)
                        val webView = findViewById<android.webkit.WebView>(R.id.webViewMap)
                        cvMap.visibility = View.VISIBLE
                        webView.settings.javaScriptEnabled = true
                        webView.settings.domStorageEnabled = true
                        webView.loadUrl(mapLink)

                        Toast.makeText(this@EmergencyHelpActivity, "Location shared with emergency contacts.", Toast.LENGTH_LONG).show()
                    } else {
                        val error = response.errorBody()?.string() ?: "Access Denied"
                        Toast.makeText(this@EmergencyHelpActivity, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ShareLocationResponse>, t: Throwable) {
                    progressDialog?.dismiss()
                    Toast.makeText(this@EmergencyHelpActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            progressDialog?.dismiss()
            Toast.makeText(this, "Failed to share location", Toast.LENGTH_SHORT).show()
        }
    }
}
