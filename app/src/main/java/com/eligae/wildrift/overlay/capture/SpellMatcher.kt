package com.eligae.wildrift.overlay.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.eligae.wildrift.overlay.model.Spell

/**
 * 풀로딩 카드의 스펠 아이콘 crop을 7종 reference와 dHash로 매칭.
 * OpenCV 없이 8x8 grayscale difference hash + Hamming distance.
 */
class SpellMatcher(context: Context) {

    private val referenceHashes: Map<Spell, Long> = Spell.entries.associateWith { spell ->
        val drawable = ContextCompat.getDrawable(context, spell.refIconRes) as BitmapDrawable
        dHash(drawable.bitmap)
    }

    /**
     * crop에서 dHash 계산 후 가장 가까운 reference 반환.
     * 풀로딩 카드 스펠은 카드별 anti-aliasing 차이로 같은 스펠도 distance 10~22 발생.
     * threshold 초과 또는 best/2nd 차이가 [marginMin] 미만이면 ambiguous → null.
     */
    fun match(crop: Bitmap, threshold: Int = 14, marginMin: Int = 6): Spell? {
        val hash = dHash(crop)
        var best: Spell? = null
        var bestDist = Int.MAX_VALUE
        var secondDist = Int.MAX_VALUE
        for ((spell, ref) in referenceHashes) {
            val d = hamming(ref, hash)
            if (d < bestDist) {
                secondDist = bestDist
                bestDist = d
                best = spell
            } else if (d < secondDist) {
                secondDist = d
            }
        }
        if (bestDist > threshold) return null
        if (secondDist - bestDist < marginMin) return null
        return best
    }

    companion object {
        /** 8x8 difference hash (Long 64bit). 좌→우 픽셀 밝기 비교 — 회전/scale/색에 robust. */
        fun dHash(src: Bitmap, size: Int = 8): Long {
            val scaled = Bitmap.createScaledBitmap(src, size + 1, size, true)
            var hash = 0L
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val left = gray(scaled.getPixel(x, y))
                    val right = gray(scaled.getPixel(x + 1, y))
                    if (left > right) hash = hash or (1L shl (y * size + x))
                }
            }
            if (scaled !== src) scaled.recycle()
            return hash
        }

        private fun gray(pixel: Int): Int {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            return (r * 299 + g * 587 + b * 114) / 1000
        }

        fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
    }
}
