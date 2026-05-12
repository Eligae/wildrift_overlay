package com.eligae.wildrift.overlay

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.eligae.wildrift.overlay.api.ApiClient
import com.eligae.wildrift.overlay.api.ChampionEntry
import kotlinx.coroutines.launch

class CompositionActivity : AppCompatActivity() {

    private lateinit var modeSynergy: TextView
    private lateinit var modeCounter: TextView
    private lateinit var hint: TextView
    private lateinit var pickerRow: LinearLayout
    private lateinit var btnRun: Button
    private lateinit var status: TextView
    private lateinit var resultContainer: LinearLayout

    private enum class Mode { SYNERGY, COUNTER }

    private var mode: Mode = Mode.SYNERGY
    private var champions: List<ChampionEntry> = emptyList()
    private val synergyPicks: Array<ChampionEntry?> = arrayOfNulls(4)
    private var counterPick: ChampionEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composition)

        modeSynergy = findViewById(R.id.mode_synergy)
        modeCounter = findViewById(R.id.mode_counter)
        hint = findViewById(R.id.comp_hint)
        pickerRow = findViewById(R.id.picker_row)
        btnRun = findViewById(R.id.btn_run)
        status = findViewById(R.id.status)
        resultContainer = findViewById(R.id.result_container)

        modeSynergy.setOnClickListener { setMode(Mode.SYNERGY) }
        modeCounter.setOnClickListener { setMode(Mode.COUNTER) }
        btnRun.setOnClickListener { run() }

        setMode(Mode.SYNERGY)
        loadChampions()
    }

    private fun loadChampions() {
        status.text = getString(R.string.tier_loading)
        lifecycleScope.launch {
            try {
                champions = ApiClient.api.getChampions().champions
                status.text = ""
            } catch (e: Exception) {
                status.text = getString(R.string.tier_error, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        val active = ContextCompat.getColor(this, R.color.lol_gold)
        val inactive = ContextCompat.getColor(this, R.color.lol_ink_soft)
        modeSynergy.setTextColor(if (mode == Mode.SYNERGY) active else inactive)
        modeCounter.setTextColor(if (mode == Mode.COUNTER) active else inactive)
        hint.text = getString(
            if (mode == Mode.SYNERGY) R.string.comp_synergy_hint else R.string.comp_counter_hint
        )
        rebuildPickerRow()
        resultContainer.removeAllViews()
    }

    private fun rebuildPickerRow() {
        pickerRow.removeAllViews()
        val labels: List<Pair<String, ChampionEntry?>> = if (mode == Mode.SYNERGY) {
            (0 until 4).map { i -> "팀원 ${i + 1}" to synergyPicks[i] }
        } else {
            listOf("적 챔피언" to counterPick)
        }
        labels.forEachIndexed { idx, (label, pick) ->
            pickerRow.addView(buildPickerButton(label, pick, idx))
        }
    }

    private fun buildPickerButton(label: String, pick: ChampionEntry?, index: Int): View {
        val btn = Button(this).apply {
            text = pick?.let { "$label: ${it.krName}" } ?: "$label 선택"
            setOnClickListener { openPicker(index) }
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topMargin = (4 * resources.displayMetrics.density).toInt()
        btn.layoutParams = lp
        return btn
    }

    private fun openPicker(index: Int) {
        if (champions.isEmpty()) {
            status.text = "챔피언 목록 로딩 중…"
            return
        }
        val names = champions.map { it.krName }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("챔피언 선택")
            .setItems(names) { _, which ->
                val picked = champions[which]
                if (mode == Mode.SYNERGY) synergyPicks[index] = picked
                else counterPick = picked
                rebuildPickerRow()
            }
            .show()
    }

    private fun run() {
        resultContainer.removeAllViews()
        status.text = "분석 중…"
        if (mode == Mode.SYNERGY) {
            val team = synergyPicks.filterNotNull().map { it.heroId }
            if (team.size < 2) {
                status.text = "최소 2명 이상 선택"
                return
            }
            lifecycleScope.launch {
                try {
                    val resp = ApiClient.api.getSynergy(team.joinToString(","))
                    status.text = "${resp.suggestions.size}명 추천"
                    resp.suggestions.forEach { s ->
                        resultContainer.addView(buildResultRow(s.krName ?: s.heroId, s.reasons.joinToString(" · ")))
                    }
                } catch (e: Exception) {
                    status.text = "오류: ${e.message}"
                }
            }
        } else {
            val enemy = counterPick
            if (enemy == null) {
                status.text = "적 챔피언 선택 필요"
                return
            }
            lifecycleScope.launch {
                try {
                    val resp = ApiClient.api.getCounter(enemy.heroId)
                    status.text = resp.note ?: ""
                    if (resp.counters.isEmpty()) {
                        resultContainer.addView(buildResultRow("(데이터 없음)", ""))
                    }
                    resp.counters.forEach { c ->
                        resultContainer.addView(buildResultRow(c.krName ?: c.heroId, ""))
                    }
                } catch (e: Exception) {
                    status.text = "오류: ${e.message}"
                }
            }
        }
    }

    private fun buildResultRow(title: String, subtitle: String): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            setBackgroundResource(R.drawable.bg_tier_row)
        }
        val name = TextView(this).apply {
            text = title
            setTextColor(ContextCompat.getColor(this@CompositionActivity, R.color.lol_gold_soft))
            textSize = 16f
            typeface = android.graphics.Typeface.SERIF
            gravity = Gravity.START
        }
        container.addView(name)
        if (subtitle.isNotBlank()) {
            val sub = TextView(this).apply {
                text = subtitle
                setTextColor(ContextCompat.getColor(this@CompositionActivity, R.color.lol_ink_soft))
                textSize = 12f
            }
            container.addView(sub)
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topMargin = (6 * resources.displayMetrics.density).toInt()
        container.layoutParams = lp
        return container
    }
}
