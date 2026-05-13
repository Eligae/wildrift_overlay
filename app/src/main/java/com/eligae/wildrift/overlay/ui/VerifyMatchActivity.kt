package com.eligae.wildrift.overlay.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.api.ApiClient
import com.eligae.wildrift.overlay.api.ChampionsCache
import com.eligae.wildrift.overlay.history.MatchHistoryStore
import com.eligae.wildrift.overlay.history.MatchRecord
import com.eligae.wildrift.overlay.model.MatchResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 종료 감지 직후 / 전적 리스트 탭 — 매핑 확인 + 본인 챔피언 마킹 + 회고 추천.
 *   - 길게 누름 (long-press): allies 셀에서 본인 슬롯 토글.
 *   - userSlot이 잡히면 시너지(우리팀 4명) + 카운터(적 같은 라인 1명) 추천 표시.
 */
class VerifyMatchActivity : AppCompatActivity() {

    private lateinit var store: MatchHistoryStore
    private lateinit var championsCache: ChampionsCache
    private var record: MatchRecord? = null
    private var enemies: MutableList<String?> = MutableList(5) { null }
    private var allies: MutableList<String?> = MutableList(5) { null }
    private var userSlot: Int? = null
    private lateinit var enemyRow: LinearLayout
    private lateinit var allyRow: LinearLayout
    private lateinit var coachingSection: LinearLayout
    private lateinit var coachingStatus: TextView
    private lateinit var synergyResults: LinearLayout
    private lateinit var counterResults: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_match)

        store = MatchHistoryStore(this)
        championsCache = ChampionsCache(this)
        val id = intent.getLongExtra("match_id", 0L)
        val r = store.find(id)
        if (r == null) {
            Toast.makeText(this, "전적을 찾을 수 없어요", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        record = r
        fillFromRecord(r)

        findViewById<TextView>(R.id.verify_result).text = when (r.result) {
            MatchResult.WIN -> "승리"
            MatchResult.LOSE -> "패배"
            MatchResult.UNKNOWN -> "결과 미상"
        }
        findViewById<TextView>(R.id.verify_time).text = SimpleDateFormat(
            "yyyy-MM-dd HH:mm", Locale.KOREA,
        ).format(Date(r.endedAtMs))

        enemyRow = findViewById(R.id.enemy_row)
        allyRow = findViewById(R.id.ally_row)
        coachingSection = findViewById(R.id.coaching_section)
        coachingStatus = findViewById(R.id.coaching_status)
        synergyResults = findViewById(R.id.synergy_results)
        counterResults = findViewById(R.id.counter_results)

        renderRow(enemyRow, enemies, isAlly = false)
        renderRow(allyRow, allies, isAlly = true)
        renderCoaching()

        findViewById<Button>(R.id.btn_verify_ok).setOnClickListener {
            val updated = r.copy(
                enemies = enemies.mapIndexedNotNull { _, n -> n },
                allies = allies.mapIndexedNotNull { _, n -> n },
                userVerified = true,
                userSlot = userSlot,
            )
            store.update(r.id) { updated }
            Toast.makeText(this, "저장됨", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btn_verify_cancel).setOnClickListener { finish() }
    }

    private fun fillFromRecord(r: MatchRecord) {
        for (i in 0 until 5) {
            enemies[i] = r.enemies.getOrNull(i)
            allies[i] = r.allies.getOrNull(i)
        }
        userSlot = r.userSlot?.takeIf { it in 0..4 }
    }

    private fun renderRow(row: LinearLayout, data: List<String?>, isAlly: Boolean) {
        row.removeAllViews()
        val density = resources.displayMetrics.density
        for (i in 0 until 5) {
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
                setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
                if (isAlly && userSlot == i) {
                    background = ContextCompat.getDrawable(this@VerifyMatchActivity, R.drawable.bg_user_slot)
                }
                setOnClickListener { pickFor(if (isAlly) allies else enemies, i, row, isAlly) }
                if (isAlly) {
                    setOnLongClickListener {
                        userSlot = if (userSlot == i) null else i
                        renderRow(allyRow, allies, isAlly = true)
                        renderCoaching()
                        true
                    }
                }
            }
            val img = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = null
                val empty = data[i] == null
                if (empty) {
                    setBackgroundResource(android.R.color.darker_gray)
                } else {
                    val avatar = championsCache.avatarFor(data[i]!!)
                    if (avatar != null) load(avatar)
                    else setBackgroundResource(android.R.color.darker_gray)
                }
            }
            val label = TextView(this).apply {
                text = if (isAlly && userSlot == i) "★ ${data[i] ?: "?"}" else (data[i] ?: "?")
                textSize = 11f
                setTextColor(
                    if (isAlly && userSlot == i)
                        ContextCompat.getColor(this@VerifyMatchActivity, R.color.lol_gold)
                    else android.graphics.Color.parseColor("#E8E3D6")
                )
                gravity = Gravity.CENTER
            }
            cell.addView(img)
            cell.addView(label)
            row.addView(cell)
        }
    }

    private fun pickFor(target: MutableList<String?>, idx: Int, row: LinearLayout, isAlly: Boolean) {
        val names = championsCache.load()?.champions?.map { it.krName }?.sorted() ?: emptyList()
        if (names.isEmpty()) {
            Toast.makeText(this, "챔피언 목록이 비어요 — 티어 화면에서 먼저 새로고침해 주세요", Toast.LENGTH_SHORT).show()
            return
        }
        val input = AutoCompleteTextView(this).apply {
            setAdapter(
                ArrayAdapter(
                    this@VerifyMatchActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names,
                ),
            )
            threshold = 1
            hint = "예: 트위 → 트위스티드 페이트 / 트위치"
            setText(target[idx] ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle("챔피언 선택")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val typed = input.text.toString().trim()
                val resolved = when {
                    typed.isEmpty() -> null
                    typed in names -> typed
                    else -> names.firstOrNull { it.startsWith(typed) }
                        ?: names.firstOrNull { it.contains(typed) }
                        ?: typed
                }
                target[idx] = resolved
                renderRow(row, target, isAlly)
                if (isAlly) renderCoaching()
            }
            .setNeutralButton("비우기") { _, _ ->
                target[idx] = null
                renderRow(row, target, isAlly)
                if (isAlly) renderCoaching()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun renderCoaching() {
        val slot = userSlot
        synergyResults.removeAllViews()
        counterResults.removeAllViews()
        if (slot == null || slot !in 0..4) {
            coachingSection.visibility = android.view.View.GONE
            return
        }
        coachingSection.visibility = android.view.View.VISIBLE
        val userChamp = allies.getOrNull(slot)
        coachingStatus.text = "내 픽: ${userChamp ?: "(비어있음)"}"

        val allyHeroIds = allies.filterIndexed { i, _ -> i != slot }
            .mapNotNull { it?.let { championsCache.heroIdFor(it) } }
        val enemyKr = enemies.getOrNull(slot)
        val enemyHeroId = enemyKr?.let { championsCache.heroIdFor(it) }

        if (allyHeroIds.size >= 2) {
            val laneFilter = slotLaneLabel(slot)
            lifecycleScope.launch {
                try {
                    val resp = ApiClient.api.getSynergy(allyHeroIds.joinToString(","), laneFilter)
                    fillResults(synergyResults, resp.suggestions.take(5).map {
                        Triple(it.krName ?: it.heroId, it.reasons.joinToString(" · "), it.avatar)
                    }, highlightKr = userChamp)
                } catch (e: Exception) {
                    addResultText(synergyResults, "시너지 추천 실패: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        } else {
            addResultText(synergyResults, "우리 팀 픽이 2명 이상 필요")
        }

        if (enemyHeroId != null) {
            lifecycleScope.launch {
                try {
                    val resp = ApiClient.api.getCounter(enemyHeroId)
                    val items = resp.counters.take(5).map { Triple(it.krName ?: it.heroId, "", it.avatar) }
                    if (items.isEmpty()) {
                        addResultText(counterResults, resp.note ?: "(카운터 데이터 없음)")
                    } else {
                        fillResults(counterResults, items, highlightKr = userChamp)
                    }
                } catch (e: Exception) {
                    addResultText(counterResults, "카운터 추천 실패: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        } else {
            addResultText(counterResults, "적 같은 라인 (${slotLaneLabel(slot)}) 슬롯 비어있음")
        }
    }

    private fun fillResults(
        container: LinearLayout,
        items: List<Triple<String, String, String?>>,
        highlightKr: String?,
    ) {
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val pad = (8 * density).toInt()
        for ((name, subtitle, avatar) in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(pad, pad, pad, pad)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                lp.topMargin = (4 * density).toInt()
                layoutParams = lp
                background = ContextCompat.getDrawable(this@VerifyMatchActivity, R.drawable.bg_tier_row)
            }
            val img = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt())
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = ContextCompat.getDrawable(this@VerifyMatchActivity, R.drawable.bg_avatar)
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            }
            if (!avatar.isNullOrBlank()) img.load(avatar) { crossfade(true) }
            row.addView(img)
            val text = TextView(this).apply {
                val highlighted = name == highlightKr
                val prefix = if (highlighted) "✓ " else ""
                text = if (subtitle.isBlank()) "$prefix$name" else "$prefix$name · $subtitle"
                textSize = 13f
                setTextColor(
                    ContextCompat.getColor(
                        this@VerifyMatchActivity,
                        if (highlighted) R.color.lol_gold else R.color.lol_ink,
                    )
                )
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginStart = (12 * density).toInt()
                layoutParams = lp
            }
            row.addView(text)
            container.addView(row)
        }
    }

    private fun addResultText(container: LinearLayout, msg: String) {
        container.removeAllViews()
        val tv = TextView(this).apply {
            text = msg
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@VerifyMatchActivity, R.color.lol_ink_dim))
        }
        container.addView(tv)
    }

    private fun slotLaneLabel(slot: Int): String = when (slot) {
        0 -> "TOP"; 1 -> "JUG"; 2 -> "MID"; 3 -> "ADC"; 4 -> "SUP"; else -> "?"
    }
}
