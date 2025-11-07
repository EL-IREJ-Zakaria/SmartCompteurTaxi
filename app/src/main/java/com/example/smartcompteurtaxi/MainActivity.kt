package com.example.smartcompteurtaxi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    private lateinit var distanceTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var fareTextView: TextView
    private lateinit var startButton: MaterialButton
    private lateinit var profileButton: ImageButton

    private var isTripStarted = false
    private var startTime: Long = 0
    private var distance = 0.0
    private var lastLocation: Location? = null

    private val handler = android.os.Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        private const val TAG = "MainActivity"
        private const val BASE_FARE = 2.50
        private const val PER_KILOMETER_RATE = 1.50
        private const val PER_MINUTE_RATE = 0.50
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        distanceTextView = findViewById(R.id.distance_textview)
        timeTextView = findViewById(R.id.time_textview)
        fareTextView = findViewById(R.id.fare_textview)
        startButton = findViewById(R.id.start_button)
        profileButton = findViewById(R.id.profile_button)

        startButton.setOnClickListener { animateAndToggleTripState() }
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    updateLocation(location)
                }
            }
        }
        updateUI()
    }

    private fun animateAndToggleTripState() {
        startButton.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            toggleTripState()
            startButton.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    private fun toggleTripState() {
        if (isTripStarted) {
            stopTrip()
        } else {
            startTrip()
        }
    }

    @AfterPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE)
    private fun requestLocationPermission() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            startLocationUpdates()
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.location_permission_rationale), LOCATION_PERMISSION_REQUEST_CODE, *perms)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        startLocationUpdates()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(5)
            fastestInterval = TimeUnit.SECONDS.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted.", e)
        }
    }

    private fun updateLocation(location: Location) {
        if (::mMap.isInitialized) {
            val latLng = LatLng(location.latitude, location.longitude)
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title(getString(R.string.my_position)))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }

        if (isTripStarted) {
            if (lastLocation != null) {
                distance += lastLocation!!.distanceTo(location) / 1000.0 // en km
            }
            lastLocation = location
        }
    }


    private fun startTrip() {
        isTripStarted = true
        startTime = System.currentTimeMillis()
        distance = 0.0
        lastLocation = null
        updateRunnable = object : Runnable {
            override fun run() {
                updateUI()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable)
        startLocationUpdates()
        updateStartButton(true)
    }

    private fun stopTrip() {
        isTripStarted = false
        handler.removeCallbacks(updateRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        showTripFinishedNotification()
        updateStartButton(false)
    }

    private fun updateStartButton(isStarted: Boolean) {
        if (isStarted) {
            startButton.text = getString(R.string.stop)
            startButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red))
        } else {
            startButton.text = getString(R.string.start)
            startButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))
        }
    }

    private fun updateUI() {
        val elapsedTime = if (startTime > 0) (System.currentTimeMillis() - startTime) / 1000 else 0
        val fare = BASE_FARE + (distance * PER_KILOMETER_RATE) + (elapsedTime / 60 * PER_MINUTE_RATE)

        distanceTextView.text = getString(R.string.distance_format, distance)
        timeTextView.text = getString(R.string.time_format, elapsedTime)
        fareTextView.text = getString(R.string.fare_format, fare)
    }

    private fun showTripFinishedNotification() {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
        val fare = BASE_FARE + (distance * PER_KILOMETER_RATE) + (elapsedTime / 60 * PER_MINUTE_RATE)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "trip_finished_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, getString(R.string.trip_finished), NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.trip_finished))
            .setContentText(getString(R.string.trip_summary_notification, fare, distance, elapsedTime))
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setMapStyle()
        requestLocationPermission()
    }

    private fun setMapStyle() {
        try {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

            val style = if (isNightMode) {
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)
            } else {
                null // Use default map style in day mode
            }
            val success = mMap.setMapStyle(style)
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }
}