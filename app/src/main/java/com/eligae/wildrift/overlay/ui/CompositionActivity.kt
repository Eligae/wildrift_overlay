package com.eligae.wildrift.overlay.ui

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.eligae.wildrift.overlay.R
import androidx.lifecycle.lifecycleScope
import coil.load
import com.eligae.wildrift.overlay.api.ApiClient
import com.eligae.wildrift.overlay.api.CachePolicy
import com.eligae.wildrift.overlay.api.ChampionEntry
import com.eligae.wildrift.overlay.api.ChampionsCache
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
    private lateinit var cache: ChampionsCache

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

        cache = ChampionsCache(this)
        setMode(Mode.SYNERGY)

        val cached = cache.load()
        if (cached != null) {
            champions = cached.champions
            val fresh = cached.fetchedAt?.let { CachePolicy.isFresh(it) } ?: false
            if (fresh) return  // 캐시 fresh — fetch 생략
        }
        loadChampions()
    }

    private fun loadChampions() {
        if (champions.isEmpty()) status.text = getString(R.string.tier_loading)
        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getChampions()
                champions = resp.champions
                cache.save(resp)
                status.text = ""
            } catch (e: Exception) {
                if (champions.isEmpty()) {
                    status.text = getString(R.string.tier_error, e.message ?: e.javaClass.simpleName)
                }
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

        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
        }
        val edit = EditText(this).apply {
            hint = "챔피언 검색"
        }
        container.addView(edit)

        val nameList = champions.map { it.krName }.toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nameList)
        val listView = ListView(this).apply {
            this.adapter = adapter
        }
        container.addView(
            listView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (400 * density).toInt())
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("챔피언 선택")
            .setView(container)
            .setNegativeButton("취소", null)
            .create()

        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim().orEmpty()
                val filtered = if (q.isBlank()) champions
                else champions.filter { it.krName.contains(q) }
                adapter.clear()
                adapter.addAll(filtered.map { it.krName })
                adapter.notifyDataSetChanged()
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val name = adapter.getItem(position) ?: return@setOnItemClickListener
            val picked = champions.firstOrNull { it.krName == name } ?: return@setOnItemClickListener
            if (mode == Mode.SYNERGY) synergyPicks[index] = picked
            else counterPick = picked
            rebuildPickerRow()
            dialog.dismiss()
        }

        dialog.show()
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
                        resultContainer.addView(
                            buildResultRow(s.krName ?: s.heroId, s.reasons.joinToString(" · "), s.avatar)
                        )
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
                        resultContainer.addView(buildResultRow("(데이터 없음)", "", null))
                    }
                    resp.counters.forEach { c ->
                        resultContainer.addView(buildResultRow(c.krName ?: c.heroId, "", c.avatar))
                    }
                } catch (e: Exception) {
                    status.text = "오류: ${e.message}"
                }
            }
        }
    }

    private fun buildResultRow(title: String, subtitle: String, avatarUrl: String?): View {
        val density = resources.displayMetrics.density
        val pad = (12 * density).toInt()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundResource(R.drawable.bg_tier_row)
        }
        val avatar = ImageView(this).apply {
            val size = (48 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = ContextCompat.getDrawable(this@CompositionActivity, R.drawable.bg_avatar)
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        if (!avatarUrl.isNullOrBlank()) {
            avatar.load(avatarUrl) { crossfade(true) }
        }
        row.addView(avatar)

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = (12 * density).toInt()
            layoutParams = lp
        }
        val name = TextView(this).apply {
            text = title
            setTextColor(ContextCompat.getColor(this@CompositionActivity, R.color.lol_gold_soft))
            textSize = 16f
            typeface = Typeface.SERIF
        }
        textCol.addView(name)
        if (subtitle.isNotBlank()) {
            val sub = TextView(this).apply {
                text = subtitle
                setTextColor(ContextCompat.getColor(this@CompositionActivity, R.color.lol_ink_soft))
                textSize = 12f
            }
            textCol.addView(sub)
        }
        row.addView(textCol)

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topMargin = (6 * density).toInt()
        row.layoutParams = lp
        return row
    }
}
