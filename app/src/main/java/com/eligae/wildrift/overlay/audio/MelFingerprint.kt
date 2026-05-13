package com.eligae.wildrift.overlay.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 라이브러리 없이 구현한 log-Mel spectrogram + 코사인 매칭.
 *   - frame: 25ms × 10ms hop
 *   - FFT 512 (radix-2 Cooley-Tukey, complex pair)
 *   - 40 mel bands (80~8000Hz)
 *   - L2 정규화: frame별
 *   - 매칭: 슬라이딩 윈도우 평균 dot product
 */
object MelFingerprint {

    const val SAMPLE_RATE = 44_100
    const val FRAME_SIZE = 1024 // 25ms = 1102 samples → power-of-2 가까운 1024
    const val HOP_SIZE = 441    // 10ms
    const val FFT_SIZE = 1024
    const val MEL_BANDS = 40
    private const val MEL_LOW = 80.0
    private const val MEL_HIGH = 8000.0

    private val window: FloatArray = FloatArray(FRAME_SIZE) {
        // Hann window
        (0.5 - 0.5 * cos(2.0 * PI * it / (FRAME_SIZE - 1))).toFloat()
    }

    private val melFilters: Array<FloatArray> = buildMelFilters()

    /**
     * PCM 16bit short[] → [frames × 40] float matrix. 각 row는 L2 정규화돼있음.
     * PCM 길이가 FRAME_SIZE 미만이면 빈 결과.
     */
    fun compute(pcm: ShortArray): Array<FloatArray> {
        if (pcm.size < FRAME_SIZE) return emptyArray()
        val frames = ((pcm.size - FRAME_SIZE) / HOP_SIZE) + 1
        if (frames <= 0) return emptyArray()
        val out = Array(frames) { FloatArray(MEL_BANDS) }
        val frame = FloatArray(FFT_SIZE) // FFT용 (FRAME_SIZE == FFT_SIZE)
        val re = FloatArray(FFT_SIZE)
        val im = FloatArray(FFT_SIZE)
        for (f in 0 until frames) {
            val start = f * HOP_SIZE
            for (i in 0 until FRAME_SIZE) {
                frame[i] = (pcm[start + i].toFloat() / 32768f) * window[i]
            }
            // copy into re, zero im
            for (i in 0 until FFT_SIZE) { re[i] = frame[i]; im[i] = 0f }
            fft(re, im)
            // power spectrum (first FFT_SIZE/2 + 1 bins)
            val nbins = FFT_SIZE / 2 + 1
            val power = FloatArray(nbins)
            for (i in 0 until nbins) power[i] = re[i] * re[i] + im[i] * im[i]
            // apply mel filters + log
            val row = out[f]
            var sumSq = 0.0
            for (m in 0 until MEL_BANDS) {
                var energy = 0f
                val filt = melFilters[m]
                for (k in 0 until nbins) energy += filt[k] * power[k]
                val v = ln((energy + 1e-6f).toDouble()).toFloat()
                row[m] = v
                sumSq += v.toDouble() * v
            }
            // L2 normalize
            val norm = sqrt(sumSq).toFloat()
            if (norm > 1e-6f) for (m in 0 until MEL_BANDS) row[m] /= norm
        }
        return out
    }

    /**
     * 입력 [input] 안에서 [template]과 가장 유사한 substring의 평균 코사인 점수(0~1).
     * input.size >= template.size 가정. 둘 다 frame별 L2 정규화된 mel feature.
     */
    fun bestMatch(input: Array<FloatArray>, template: Array<FloatArray>): Float {
        if (input.size < template.size || template.isEmpty()) return 0f
        var best = -1f
        for (offset in 0..(input.size - template.size)) {
            var sum = 0f
            for (i in 0 until template.size) {
                val a = template[i]
                val b = input[offset + i]
                var dot = 0f
                for (j in 0 until MEL_BANDS) dot += a[j] * b[j]
                sum += dot
            }
            val avg = sum / template.size
            if (avg > best) best = avg
        }
        return best
    }

    // --- internals ---

    private fun buildMelFilters(): Array<FloatArray> {
        val nbins = FFT_SIZE / 2 + 1
        val melLow = hzToMel(MEL_LOW)
        val melHigh = hzToMel(MEL_HIGH)
        val melPoints = DoubleArray(MEL_BANDS + 2) { melLow + (melHigh - melLow) * it / (MEL_BANDS + 1) }
        val hzPoints = melPoints.map { melToHz(it) }
        val binPoints = hzPoints.map { (it * FFT_SIZE / SAMPLE_RATE).toInt().coerceIn(0, nbins - 1) }
        return Array(MEL_BANDS) { m ->
            val filt = FloatArray(nbins)
            val lo = binPoints[m]; val mid = binPoints[m + 1]; val hi = binPoints[m + 2]
            for (k in lo until mid) if (mid > lo) filt[k] = (k - lo).toFloat() / (mid - lo)
            for (k in mid until hi) if (hi > mid) filt[k] = (hi - k).toFloat() / (hi - mid)
            filt
        }
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * Math.log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0)

    /** In-place radix-2 Cooley-Tukey. n = FFT_SIZE = 2^k. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        // bit reversal
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var tmp = re[i]; re[i] = re[j]; re[j] = tmp
                tmp = im[i]; im[i] = im[j]; im[j] = tmp
            }
        }
        var len = 2
        while (len <= n) {
            val half = len / 2
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat(); val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f; var curIm = 0f
                for (k in 0 until half) {
                    val a = i + k; val b = i + k + half
                    val tRe = curRe * re[b] - curIm * im[b]
                    val tIm = curRe * im[b] + curIm * re[b]
                    re[b] = re[a] - tRe; im[b] = im[a] - tIm
                    re[a] += tRe; im[a] += tIm
                    val nRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
