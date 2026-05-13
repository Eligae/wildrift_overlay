package com.eligae.wildrift.overlay.audio

import android.content.Context
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream

/**
 * 한 트리거 사운드의 mel-fingerprint + 메타데이터.
 * 디스크 포맷:
 *   magic 4B "WRFP"
 *   version u16 = 1
 *   frames u16
 *   bands u16 (= 40)
 *   labelLen u16 + label utf-8
 *   action u16 (TriggerAction ordinal)
 *   threshold f32
 *   frames × 40 × f32 (little-endian)
 */
data class SoundTemplate(
    val id: String,
    val label: String,
    val action: TriggerAction,
    val threshold: Float,
    val features: Array<FloatArray>, // [frames][40]
    val builtin: Boolean,
) {
    val frames: Int get() = features.size

    fun durationSec(): Float {
        return frames.toFloat() * MelFingerprint.HOP_SIZE / MelFingerprint.SAMPLE_RATE
    }

    companion object {
        private const val TAG = "SoundTemplate"
        private const val MAGIC = 0x57524650.toInt() // "WRFP"

        fun write(file: File, t: SoundTemplate) {
            DataOutputStream(file.outputStream().buffered()).use { o ->
                o.writeInt(MAGIC)
                o.writeShort(1)
                o.writeShort(t.frames)
                o.writeShort(MelFingerprint.MEL_BANDS)
                val lb = t.label.toByteArray(Charsets.UTF_8)
                o.writeShort(lb.size)
                o.write(lb)
                o.writeShort(t.action.ordinal)
                o.writeFloat(t.threshold)
                for (row in t.features) for (v in row) o.writeFloat(v)
            }
        }

        fun read(stream: InputStream, id: String, builtin: Boolean): SoundTemplate? {
            return try {
                DataInputStream(stream.buffered()).use { i ->
                    if (i.readInt() != MAGIC) return null
                    val ver = i.readUnsignedShort()
                    if (ver != 1) return null
                    val frames = i.readUnsignedShort()
                    val bands = i.readUnsignedShort()
                    if (bands != MelFingerprint.MEL_BANDS) return null
                    val labelLen = i.readUnsignedShort()
                    val lb = ByteArray(labelLen).also { i.readFully(it) }
                    val label = String(lb, Charsets.UTF_8)
                    val action = TriggerAction.entries.getOrNull(i.readUnsignedShort())
                        ?: TriggerAction.CUSTOM
                    val threshold = i.readFloat()
                    val feats = Array(frames) { FloatArray(bands) { i.readFloat() } }
                    SoundTemplate(id, label, action, threshold, feats, builtin)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "read $id failed", t); null
            }
        }
    }
}

enum class TriggerAction { MATCH_START, MATCH_WIN, MATCH_LOSE, ULT_READY, CHAT_PING, CUSTOM }

/**
 * builtin + user 템플릿 통합 로더.
 * - assets/sound_triggers/builtin/<id>.npfp  → builtin=true
 * - filesDir/sound_triggers/user/<id>.npfp   → builtin=false
 */
object SoundTemplateRepo {
    private const val TAG = "SoundTemplateRepo"
    private const val ASSETS_DIR = "sound_triggers/builtin"
    private const val USER_DIR = "sound_triggers/user"

    fun userDir(context: Context): File =
        File(context.filesDir, USER_DIR).apply { if (!exists()) mkdirs() }

    fun loadAll(context: Context): List<SoundTemplate> {
        val out = mutableListOf<SoundTemplate>()
        // builtin (assets)
        try {
            context.assets.list(ASSETS_DIR)?.forEach { name ->
                if (!name.endsWith(".npfp")) return@forEach
                val id = name.removeSuffix(".npfp")
                context.assets.open("$ASSETS_DIR/$name").use { s ->
                    SoundTemplate.read(s, id, builtin = true)?.let(out::add)
                }
            }
        } catch (t: Throwable) { Log.w(TAG, "builtin scan failed", t) }
        // user (filesDir)
        userDir(context).listFiles { _, n -> n.endsWith(".npfp") }?.forEach { f ->
            f.inputStream().use { s ->
                SoundTemplate.read(s, f.nameWithoutExtension, builtin = false)?.let(out::add)
            }
        }
        Log.d(TAG, "loaded ${out.size} templates")
        return out
    }

    fun saveUserTemplate(context: Context, t: SoundTemplate): File {
        val f = File(userDir(context), "${t.id}.npfp")
        SoundTemplate.write(f, t)
        return f
    }

    fun deleteUserTemplate(context: Context, id: String): Boolean {
        return File(userDir(context), "$id.npfp").delete()
    }
}
