package com.eligae.wildrift.overlay.ui

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.floating.RoiOverlayView
import com.eligae.wildrift.overlay.prefs.OverlayPrefs

/**
 * ROI 캘리브레이션 — 최근 캡처 PNG 또는 갤러리 이미지를 가로 풀스크린으로 띄우고
 * 빨간 사각형을 드래그·리사이즈해서 관심 영역(채팅 등) 지정. landscape 강제.
 */
class CalibrationActivity : AppCompatActivity() {

    private lateinit var prefs: OverlayPrefs
    private lateinit var imageView: ImageView
    private lateinit var roiView: RoiOverlayView
    private lateinit var status: TextView
    private var currentBitmap: Bitmap? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) loadFromUri(uri) else status.text = "선택 취소됨"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_calibration)

        prefs = OverlayPrefs(this)
        imageView = findViewById(R.id.calib_image)
        roiView = findViewById(R.id.calib_roi)
        status = findViewById(R.id.calib_status)

        loadLatestCapture()
        roiView.roi = RectF(prefs.roiLeft, prefs.roiTop, prefs.roiRight, prefs.roiBottom)
        roiView.listener = { r ->
            status.text = "ROI %d%% %d%% → %d%% %d%%".format(
                (r.left * 100).toInt(),
                (r.top * 100).toInt(),
                (r.right * 100).toInt(),
                (r.bottom * 100).toInt(),
            )
        }

        findViewById<Button>(R.id.btn_pick).setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }
        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            roiView.roi = RectF(0f, 0f, 1f, 1f)
        }
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val r = roiView.roi
            prefs.roiLeft = r.left
            prefs.roiTop = r.top
            prefs.roiRight = r.right
            prefs.roiBottom = r.bottom
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
        val bmp = BitmapFactory.decodeFile(latest.absolutePath)
        if (bmp == null) {
            status.text = getString(R.string.calib_no_capture)
            return
        }
        // OcrProcessor.prepare()가 이미 rotate90 후 저장 → landscape 그대로 사용.
        showBitmap(bmp, label = "${latest.name} · 캡처")
    }

    private fun loadFromUri(uri: Uri) {
        val bmp = try {
            contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (t: Throwable) {
            status.text = "이미지 로드 실패: ${t.message}"
            return
        }
        if (bmp == null) {
            status.text = "이미지 디코드 실패"
            return
        }
        // 가로 이미지로 가정 (게임 스크린샷). 세로면 사용자가 다시 선택.
        val display = if (bmp.width >= bmp.height) bmp else rotate(bmp, 90f).also { bmp.recycle() }
        showBitmap(display, label = "갤러리 · ${display.width}×${display.height}")
    }

    private fun showBitmap(bmp: Bitmap, label: String) {
        currentBitmap?.recycle()
        currentBitmap = bmp
        imageView.setImageBitmap(bmp)
        status.text = label
    }

    private fun rotate(src: Bitmap, deg: Float): Bitmap {
        val m = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}
