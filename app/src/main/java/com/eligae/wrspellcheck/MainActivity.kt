package com.eligae.wrspellcheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: OverlayPrefs
    private lateinit var permissionStatus: TextView
    private lateinit var btnGrant: Button
    private lateinit var btnStart: Button
    private lateinit var seekScale: SeekBar
    private lateinit var scaleValue: TextView

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore result — 알림은 보조 UX */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = OverlayPrefs(this)
        permissionStatus = findViewById(R.id.permission_status)
        btnGrant = findViewById(R.id.btn_grant)
        btnStart = findViewById(R.id.btn_start)
        seekScale = findViewById(R.id.seek_scale)
        scaleValue = findViewById(R.id.scale_value)

        btnGrant.setOnClickListener { requestOverlayPermission() }
        btnStart.setOnClickListener { toggleOverlay() }

        seekScale.progress = ((prefs.scale - SCALE_MIN) / SCALE_STEP).toInt()
        scaleValue.text = formatScale(prefs.scale)
        seekScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                scaleValue.text = formatScale(progressToScale(progress))
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                val newScale = progressToScale(sb?.progress ?: 5)
                if (newScale != prefs.scale) {
                    prefs.scale = newScale
                    if (OverlayService.isRunning) {
                        OverlayService.stop(this@MainActivity)
                        sb?.postDelayed({ OverlayService.start(this@MainActivity) }, 300)
                    }
                }
            }
        })

        requestNotificationPermissionIfNeeded()
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun progressToScale(progress: Int): Float = SCALE_MIN + progress * SCALE_STEP

    private fun formatScale(scale: Float): String = "%.1fx".format(scale)

    companion object {
        private const val SCALE_MIN = 0.5f
        private const val SCALE_STEP = 0.1f
    }
}
