package com.eligae.wildrift.overlay.parse

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import kotlin.math.hypot

/**
 * 풀로딩 화면 OCR 결과에서 본인 슬롯 식별.
 *   WR은 본인 닉네임만 금색(노란) 텍스트로 표시 → bbox 내 픽셀 색상 검사로 결정.
 */
object UserSlotDetector {

    private const val TAG = "UserSlotDetector"

    /** 노란 픽셀 비율 임계값 — 인게임 HUD 골드 카운터 등 약한 노란 신호 차단 위해 5%로 보수적 설정. */
    private const val YELLOW_RATIO_THRESHOLD = 0.05

    data class Candidate(val centerX: Float, val centerY: Float)

    /**
     * 노란 닉네임 bbox와 가장 가까운 [picks] 항목의 인덱스 반환. 자동 식별 실패 시 null.
     *
     * @param picks LoadingScreenParser가 이미 추출한 챔피언 픽들 (allies + enemies 혼합).
     * @param blocks OCR text blocks (bbox 있는 것만 의미 있음).
     * @param bitmap 회전된(processed) 비트맵 — bbox 좌표계와 일치해야 함.
     */
    fun findUserPickIndex(
        picks: List<Candidate>,
        blocks: List<Rect?>,
        bitmap: Bitmap,
    ): Int? {
        if (picks.isEmpty()) return null
        var yellowBlock: Rect? = null
        var yellowRatio = 0.0
        for (b in blocks) {
            if (b == null) continue
            val ratio = yellowPixelRatio(bitmap, b)
            if (ratio >= YELLOW_RATIO_THRESHOLD && ratio > yellowRatio) {
                yellowBlock = b
                yellowRatio = ratio
            }
        }
        if (yellowBlock == null) {
            Log.d(TAG, "no yellow nickname block found")
            return null
        }
        val cx = (yellowBlock.left + yellowBlock.right) / 2f
        val cy = (yellowBlock.top + yellowBlock.bottom) / 2f
        val idx = picks.indices.minByOrNull {
            hypot((picks[it].centerX - cx).toDouble(), (picks[it].centerY - cy).toDouble())
        }
        Log.d(TAG, "yellow nickname bbox ratio=$yellowRatio center=($cx,$cy) → pick idx=$idx")
        return idx
    }

    /** bbox 내 노란/금색 픽셀 비율. */
    private fun yellowPixelRatio(bitmap: Bitmap, bbox: Rect): Double {
        val w = bitmap.width
        val h = bitmap.height
        val l = bbox.left.coerceIn(0, w - 1)
        val t = bbox.top.coerceIn(0, h - 1)
        val r = bbox.right.coerceIn(l + 1, w)
        val b = bbox.bottom.coerceIn(t + 1, h)
        val cols = r - l
        val rows = b - t
        if (cols <= 0 || rows <= 0) return 0.0
        // 성능 — bbox 픽셀 모두 검사 대신 stride 샘플링.
        val stride = (maxOf(cols, rows) / 40).coerceAtLeast(1)
        var yellow = 0
        var total = 0
        var y = t
        while (y < b) {
            var x = l
            while (x < r) {
                val p = bitmap.getPixel(x, y)
                if (isYellowPixel(p)) yellow++
                total++
                x += stride
            }
            y += stride
        }
        return if (total > 0) yellow.toDouble() / total else 0.0
    }

    /** RGB → HSV로 변환해 hue 40~55, sat > 0.35, val > 0.55인 픽셀을 노란/금색으로 판정. */
    private fun isYellowPixel(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val bl = color and 0xFF
        val max = maxOf(r, g, bl)
        val min = minOf(r, g, bl)
        val v = max / 255.0
        if (v < 0.55) return false
        val delta = max - min
        val s = if (max == 0) 0.0 else delta.toDouble() / max
        if (s < 0.35) return false
        val h = when {
            delta == 0 -> 0.0
            max == r -> 60.0 * (((g - bl).toDouble() / delta) % 6)
            max == g -> 60.0 * (((bl - r).toDouble() / delta) + 2)
            else -> 60.0 * (((r - g).toDouble() / delta) + 4)
        }
        val hh = if (h < 0) h + 360 else h
        return hh in 38.0..62.0
    }
}
