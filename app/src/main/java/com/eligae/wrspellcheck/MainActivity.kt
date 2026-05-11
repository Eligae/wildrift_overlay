package com.eligae.wrspellcheck

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var permissionStatus: TextView
    private lateinit var btnGrant: Button
    private lateinit var btnStart: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionStatus = findViewById(R.id.permission_status)
        btnGrant = findViewById(R.id.btn_grant)
        btnStart = findViewById(R.id.btn_start)

        btnGrant.setOnClickListener { requestOverlayPermission() }
        btnStart.setOnClickListener { toggleOverlay() }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUi()
        refreshStartButton()
    }

    private fun refreshPermissionUi() {
        val granted = Settings.canDrawOverlays(this)
        if (granted) {
            permissionStatus.setText(R.string.permission_granted)
            btnGrant.isEnabled = false
            btnStart.isEnabled = true
        } else {
            permissionStatus.setText(R.string.permission_required)
            btnGrant.isEnabled = true
            btnStart.isEnabled = false
        }
    }

    private fun refreshStartButton() {
        btnStart.setText(
            if (OverlayService.isRunning) R.string.stop_overlay else R.string.start_overlay
        )
    }

    private fun toggleOverlay() {
        if (OverlayService.isRunning) {
            OverlayService.stop(this)
        } else {
            OverlayService.start(this)
        }
        btnStart.postDelayed({ refreshStartButton() }, 300)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
