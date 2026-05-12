package com.eligae.wildrift.overlay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.eligae.wildrift.overlay.prefs.OverlayPrefs
import java.io.File

/**
 * 사용자 ROI 캘리브레이션. 가장 최근 캡처 PNG를 회전 표시 + 4 슬라이더로 사각형 박음.
 * PNG가 없으면 "캡처 먼저" 안내.
 */
class CalibrationActivity : AppCompatActivity() {

    private lateinit var prefs: OverlayPrefs
    private lateinit var imageView: ImageView
    private lateinit var roiView: RoiOverlayView
    private lateinit var status: TextView
    private lateinit var seekL: SeekBar
    private lateinit var seekT: SeekBar
    private lateinit var seekR: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var valL: TextView
    private lateinit var valT: TextView
    private lateinit var valR: TextView
    private lateinit var valB: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        prefs = OverlayPrefs(this)
        imageView = findViewById(R.id.calib_image)
        roiView = findViewById(R.id.calib_roi)
        status = findViewById(R.id.calib_status)
        seekL = findViewById(R.id.seek_left)
        seekT = findViewById(R.id.seek_top)
        seekR = findViewById(R.id.seek_right)
        seekB = findViewById(R.id.seek_bottom)
        valL = findViewById(R.id.val_left)
        valT = findViewById(R.id.val_top)
        valR = findViewById(R.id.val_right)
        valB = findViewById(R.id.val_bottom)

        loadLatestCapture()
        initSliders()

        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            setRoi(0f, 0f, 1f, 1f)
        }
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            prefs.roiLeft = seekL.progress / 100f
            prefs.roiTop = seekT.progress / 100f
            prefs.roiRight = seekR.progress / 100f
            prefs.roiBottom = seekB.progress / 100f
            status.text = getString(R.string.calib_saved)
            finish()
        }
    }

    private fun loadLatestCapture() {
        val dir = getExternalFilesDir(null)
        val pngs = dir?.listFiles { _, name -> name.startsWith("capture_") && name.endsWith(".png") }
        val latest = pngs?.maxByOrNull { it.lastModified() }
        if (latest == null) {
            status.text = getString(R.string.calib_no_capture)
            return
        }
        val portrait = BitmapFactory.decodeFile(latest.absolutePath)
        if (portrait == null) {
            status.text = getString(R.string.calib_no_capture)
            return
        }
        // 회전 frame에서 ROI를 박으므로 90도 시계 회전된 비트맵을 표시.
        val rotated = rotate(portrait, 90f)
        portrait.recycle()
        imageView.setImageBitmap(rotated)
        status.text = "${latest.name} · ${rotated.width}×${rotated.height} (rotated)"
    }

    private fun rotate(src: Bitmap, deg: Float): Bitmap {
        val m = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun initSliders() {
        val l = (prefs.roiLeft * 100).toInt()
        val t = (prefs.roiTop * 100).toInt()
        val r = (prefs.roiRight * 100).toInt()
        val b = (prefs.roiBottom * 100).toInt()
        setRoi(l / 100f, t / 100f, r / 100f, b / 100f)

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                refreshFromSeekBars()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        }
        seekL.setOnSeekBarChangeListener(listener)
        seekT.setOnSeekBarChangeListener(listener)
        seekR.setOnSeekBarChangeListener(listener)
        seekB.setOnSeekBarChangeListener(listener)
    }

    private fun refreshFromSeekBars() {
        val l = (seekL.progress / 100f).coerceIn(0f, 1f)
        val t = (seekT.progress / 100f).coerceIn(0f, 1f)
        val r = (seekR.progress / 100f).coerceIn(0f, 1f)
        val b = (seekB.progress / 100f).coerceIn(0f, 1f)
        applyRoi(l, t, r, b)
    }

    private fun setRoi(l: Float, t: Float, r: Float, b: Float) {
        seekL.progress = (l * 100).toInt()
        seekT.progress = (t * 100).toInt()
        seekR.progress = (r * 100).toInt()
        seekB.progress = (b * 100).toInt()
        applyRoi(l, t, r, b)
    }

    private fun applyRoi(l: Float, t: Float, r: Float, b: Float) {
        roiView.roi = RectF(l, t, r, b)
        valL.text = "%d%%".format((l * 100).toInt())
        valT.text = "%d%%".format((t * 100).toInt())
        valR.text = "%d%%".format((r * 100).toInt())
        valB.text = "%d%%".format((b * 100).toInt())
    }
}
