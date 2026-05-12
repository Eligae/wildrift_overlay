package com.eligae.wildrift.overlay

import android.content.Context
import com.eligae.wildrift.overlay.model.Spell
import com.eligae.wildrift.overlay.model.SlotState

class OverlayPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("overlay_state", Context.MODE_PRIVATE)

    var overlayX: Int
        get() = prefs.getInt("overlay_x", 32)
        set(value) = prefs.edit().putInt("overlay_x", value).apply()

    var overlayY: Int
        get() = prefs.getInt("overlay_y", 200)
        set(value) = prefs.edit().putInt("overlay_y", value).apply()

    var collapseLevel: Int
        get() = prefs.getInt("collapse_level", 0)
        set(value) = prefs.edit().putInt("collapse_level", value.coerceIn(0, 2)).apply()

    var scale: Float
        get() = prefs.getFloat("scale", 1.0f)
        set(value) = prefs.edit().putFloat("scale", value.coerceIn(0.5f, 1.5f)).apply()

    /** 오버레이 배경 알파 (0.2 = 매우 투명 ~ 1.0 = 불투명 검정). 기본 0.8. */
    var bgAlpha: Float
        get() = prefs.getFloat("bg_alpha", 0.8f)
        set(value) = prefs.edit().putFloat("bg_alpha", value.coerceIn(0.2f, 1.0f)).apply()

    /**
     * OCR ROI 비율 (회전된 frame 기준 0~1).
     * 기본 (0,0,1,1) = 전체 화면. 사용자 캘리브레이션 시 좁혀짐.
     */
    var roiLeft: Float
        get() = prefs.getFloat("roi_left", 0f)
        set(v) = prefs.edit().putFloat("roi_left", v.coerceIn(0f, 1f)).apply()
    var roiTop: Float
        get() = prefs.getFloat("roi_top", 0f)
        set(v) = prefs.edit().putFloat("roi_top", v.coerceIn(0f, 1f)).apply()
    var roiRight: Float
        get() = prefs.getFloat("roi_right", 1f)
        set(v) = prefs.edit().putFloat("roi_right", v.coerceIn(0f, 1f)).apply()
    var roiBottom: Float
        get() = prefs.getFloat("roi_bottom", 1f)
        set(v) = prefs.edit().putFloat("roi_bottom", v.coerceIn(0f, 1f)).apply()

    val hasCustomRoi: Boolean
        get() = roiRight > roiLeft && roiBottom > roiTop &&
            (roiRight - roiLeft < 0.99f || roiBottom - roiTop < 0.99f)

    /**
     * 우리 팀 anchor — 픽 직후 5명만 보여주는 화면에서 추출한 챔피언 5명.
     * 10명 로딩 화면에서 이 5명을 빼면 적팀이 확정. 30분 지나면 stale.
     */
    var allyAnchor: List<String>
        get() = prefs.getStringSet("ally_anchor", emptySet())?.toList() ?: emptyList()
        set(value) = prefs.edit().putStringSet("ally_anchor", value.toSet()).apply()

    var allyAnchorAtMs: Long
        get() = prefs.getLong("ally_anchor_at_ms", 0L)
        set(value) = prefs.edit().putLong("ally_anchor_at_ms", value).apply()

    fun freshAllyAnchor(maxAgeMs: Long = 30 * 60 * 1000L): List<String>? {
        val ts = allyAnchorAtMs
        if (ts == 0L) return null
        if (System.currentTimeMillis() - ts > maxAgeMs) return null
        return allyAnchor.takeIf { it.isNotEmpty() }
    }

    fun loadSlot(index: Int): SlotState {
        val k = "slot_$index"
        return SlotState(
            index = index,
            championName = prefs.getString("${k}_champion", null)?.takeIf { it.isNotBlank() },
            spell1 = Spell.entries.getOrElse(prefs.getInt("${k}_spell1", 0)) { Spell.FLASH },
            spell2 = Spell.entries.getOrElse(prefs.getInt("${k}_spell2", 1)) { Spell.IGNITE },
            spell1ReadyAtEpochMs = prefs.getLong("${k}_spell1_ready", -1L).takeIf { it > 0 },
            spell2ReadyAtEpochMs = prefs.getLong("${k}_spell2_ready", -1L).takeIf { it > 0 },
        )
    }

    fun saveSlot(state: SlotState) {
        val k = "slot_${state.index}"
        prefs.edit().apply {
            putString("${k}_champion", state.championName ?: "")
            putInt("${k}_spell1", state.spell1.ordinal)
            putInt("${k}_spell2", state.spell2.ordinal)
            putLong("${k}_spell1_ready", state.spell1ReadyAtEpochMs ?: -1L)
            putLong("${k}_spell2_ready", state.spell2ReadyAtEpochMs ?: -1L)
        }.apply()
    }

    /** 챔피언명만 별도 갱신 (broadcast에서 호출). */
    fun setSlotChampion(index: Int, name: String?) {
        prefs.edit().putString("slot_${index}_champion", name ?: "").apply()
    }
}
