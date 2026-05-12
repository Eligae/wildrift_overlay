package com.eligae.wildrift.overlay.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.eligae.wildrift.overlay.parse.ChatParser
import com.eligae.wildrift.overlay.parse.LoadingScreenParser
import com.eligae.wildrift.overlay.prefs.OverlayPrefs
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File

/**
 * ML Kit OCR 호출 + ROI crop + ChatParser/LoadingScreenParser 매칭 + anchor 갱신 + broadcast 발신.
 * 캡처 인프라(Session)와 분리되어 bitmap 입력만 받으면 동작.
 */
internal class OcrProcessor(
    private val context: Context,
    /** broadcast action — Service.companion에 박힌 상수를 재사용. */
    private val actionLoadingDetected: String,
    private val extraEnemies: String,
    /** OCR 1회가 끝나면 호출 (성공/실패 무관). */
    private val onDone: () -> Unit,
) {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    fun close() {
        try { recognizer.close() } catch (_: Throwable) {}
    }

    fun process(portraitBitmap: Bitmap) {
        val prefs = OverlayPrefs(context.applicationContext)
        val (scaled, rotationDegrees) = prepare(portraitBitmap, prefs)
        val input = InputImage.fromBitmap(scaled, rotationDegrees)
        recognizer.process(input)
            .addOnSuccessListener { result ->
                if (result.textBlocks.isNotEmpty()) {
                    handleResult(result, scaled, prefs)
                    saveBitmap(scaled)
                } else {
                    Log.d(TAG, "OCR empty")
                }
                scaled.recycle()
                onDone()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
                scaled.recycle()
                onDone()
            }
    }

    /** ROI가 있으면 회전 + crop, 없으면 portrait 그대로 + rotationDegrees=90. */
    private fun prepare(bitmap: Bitmap, prefs: OverlayPrefs): Pair<Bitmap, Int> {
        return if (prefs.hasCustomRoi) {
            val rotated = BitmapUtils.rotate90(bitmap)
            bitmap.recycle()
            val cropped = BitmapUtils.cropByRatio(
                rotated,
                prefs.roiLeft, prefs.roiTop, prefs.roiRight, prefs.roiBottom,
            )
            rotated.recycle()
            cropped to 0
        } else {
            bitmap to 90
        }
    }

    private fun handleResult(
        result: com.google.mlkit.vision.text.Text,
        scaled: Bitmap,
        prefs: OverlayPrefs,
    ) {
        val n = result.textBlocks.size
        Log.d(TAG, "OCR ok: $n blocks, chars=${result.text.length}, frame=${scaled.width}x${scaled.height}, roi=${prefs.hasCustomRoi}")
        for (block in result.textBlocks) {
            val bb = block.boundingBox
            val bbStr = if (bb != null) "[${bb.left},${bb.top},${bb.right},${bb.bottom}]" else "[null]"
            Log.d(TAG, "BLOCK $bbStr ${block.text.replace("\n", " | ")}")
        }

        val blockTexts = result.textBlocks.map { it.text }
        for (m in ChatParser.parse(blockTexts)) {
            Log.d(TAG, "CHAT MATCH: ${m.champion} → ${m.spell.name}")
        }

        val locs = result.textBlocks.mapNotNull { tb ->
            val box = tb.boundingBox ?: return@mapNotNull null
            LoadingScreenParser.TextLoc(
                tb.text,
                (box.left + box.right) / 2f,
                (box.top + box.bottom) / 2f,
            )
        }
        val rotatedFrameHeight = scaled.width
        val anchor = prefs.freshAllyAnchor()
        val teams = LoadingScreenParser.parseTeams(locs, rotatedFrameHeight, anchor)

        maybeSaveAllyAnchor(teams.picks, prefs)
        broadcastEnemiesIfPass(teams, prefs, anchor != null)
    }

    private fun maybeSaveAllyAnchor(
        picks: List<LoadingScreenParser.Pick>,
        prefs: OverlayPrefs,
    ) {
        if (picks.size != 5) return
        val canonical = picks.map { it.canonical }
        if (canonical.toSet() != prefs.allyAnchor.toSet()) {
            prefs.allyAnchor = canonical
            prefs.allyAnchorAtMs = System.currentTimeMillis()
            Log.d(TAG, "ALLY ANCHOR SAVED: $canonical")
        }
    }

    private fun broadcastEnemiesIfPass(
        teams: LoadingScreenParser.Teams,
        prefs: OverlayPrefs,
        anchorActive: Boolean,
    ) {
        val pass = if (anchorActive) {
            teams.enemies.size >= 3
        } else {
            teams.enemies.size + teams.allies.size >= 6 && teams.enemies.size >= 3
        }
        if (!pass) return

        Log.d(TAG, "LOADING ENEMIES (TOP→SUP): ${teams.enemies}${if (anchorActive) " [anchor]" else ""}")
        Log.d(TAG, "LOADING ALLIES  (TOP→SUP): ${teams.allies}")
        teams.enemies.forEachIndexed { i, name ->
            if (i + 1 <= 5) prefs.setSlotChampion(i + 1, name)
        }
        for (i in (teams.enemies.size + 1)..5) {
            prefs.setSlotChampion(i, null)
        }
        val bi = Intent(actionLoadingDetected).apply {
            setPackage(context.packageName)
            putStringArrayListExtra(extraEnemies, ArrayList(teams.enemies))
        }
        context.sendBroadcast(bi)
    }

    private fun saveBitmap(bitmap: Bitmap) {
        try {
            val dir = context.getExternalFilesDir(null)
            val file = File(dir, "capture_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 80, it) }
            Log.d(TAG, "Saved: ${file.name} (${bitmap.width}x${bitmap.height})")
        } catch (t: Throwable) {
            Log.e(TAG, "Save failed", t)
        }
    }

    companion object {
        private const val TAG = "WRCapture"
    }
}
