package com.example.focuslock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        val etHours = findViewById<EditText>(R.id.etHours)
        val btnStart = findViewById<Button>(R.id.btnStartLock)

        btnStart.setOnClickListener {
            checkPermissionsAndStart(etHours.text.toString().toIntOrNull() ?: 0)
        }
    }

    private fun checkPermissionsAndStart(hours: Int) {
        if (hours <= 0) {
            Toast.makeText(this, "Please enter valid hours", Toast.LENGTH_SHORT).show()
            return
        }

        // Overlay Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        // Device Admin Check
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lock features requires Admin access.")
            startActivity(intent)
            return
        }

        // Save Lock End Time
        val durationMs = hours * 3600 * 1000L
        val endTime = System.currentTimeMillis() + durationMs

        val prefs = getSharedPreferences("LockPrefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("endTime", endTime).apply()

        // Start Lock Service
        val serviceIntent = Intent(this, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        finish()
    }
}
