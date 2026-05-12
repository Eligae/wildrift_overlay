package com.eligae.wildrift.overlay

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
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
    private lateinit var btnCapture: Button
    private lateinit var btnTier: Button
    private lateinit var btnComposition: Button

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore result — 알림은 보조 UX */ }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            btnCapture.postDelayed({ refreshCaptureButton() }, 600)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = OverlayPrefs(this)
        permissionStatus = findViewById(R.id.permission_status)
        btnGrant = findViewById(R.id.btn_grant)
        btnStart = findViewById(R.id.btn_start)
        seekScale = findViewById(R.id.seek_scale)
        scaleValue = findViewById(R.id.scale_value)
        btnCapture = findViewById(R.id.btn_capture)
        btnTier = findViewById(R.id.btn_tier)
        btnComposition = findViewById(R.id.btn_composition)

        btnGrant.setOnClickListener { requestOverlayPermission() }
        btnStart.setOnClickListener { toggleOverlay() }
        btnCapture.setOnClickListener { toggleCapture() }
        btnTier.setOnClickListener { startActivity(Intent(this, TierActivity::class.java)) }
        btnComposition.setOnClickListener { startActivity(Intent(this, CompositionActivity::class.java)) }

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
        refreshCaptureButton()
    }

    private fun refreshCaptureButton() {
        btnCapture.setText(
            if (ScreenCaptureService.isRunning) R.string.capture_stop else R.string.capture_test
        )
    }

    private fun toggleCapture() {
        if (ScreenCaptureService.isRunning) {
            ScreenCaptureService.stop(this)
            btnCapture.postDelayed({ refreshCaptureButton() }, 300)
        } else {
            requestCapture()
        }
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

    private fun requestCapture() {
        val mpm = getSystemService(MediaProjectionManager::class.java)
        captureLauncher.launch(mpm.createScreenCaptureIntent())
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
