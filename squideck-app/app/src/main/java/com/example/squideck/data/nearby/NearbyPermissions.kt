package com.example.squideck.data.nearby

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** Runtime permissions Nearby Connections needs on this Android version (Google's Nearby setup guide). */
object NearbyPermissions {
    val required: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            else -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun granted(context: Context): Boolean =
        required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * Android 11 and older treat Bluetooth scanning as a location scan, so Nearby also needs the phone's Location
     * switch on; otherwise it fails with MISSING_PERMISSION_ACCESS_COARSE_LOCATION even with the permission granted.
     */
    fun locationSwitchOff(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            context.getSystemService(LocationManager::class.java)?.let { !LocationManagerCompat.isLocationEnabled(it) } == true
}
