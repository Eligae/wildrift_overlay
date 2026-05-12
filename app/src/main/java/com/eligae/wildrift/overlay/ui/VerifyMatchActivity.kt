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
import coil.load
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.api.ChampionsCache
import com.eligae.wildrift.overlay.history.MatchHistoryStore
import com.eligae.wildrift.overlay.history.MatchRecord
import com.eligae.wildrift.overlay.model.MatchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 종료 감지 직후 — 매핑이 맞는지 확인. 각 슬롯 탭하면 챔피언 picker로 교체 가능.
 * "맞아요" 버튼 → userVerified=true 저장 후 종료. "나중에" → 그대로 둠 (verified=false).
 */
class VerifyMatchActivity : AppCompatActivity() {

    private lateinit var store: MatchHistoryStore
    private lateinit var championsCache: ChampionsCache
    private var record: MatchRecord? = null
    private var enemies: MutableList<String?> = MutableList(5) { null }
    private var allies: MutableList<String?> = MutableList(5) { null }
    private lateinit var enemyRow: LinearLayout
    private lateinit var allyRow: LinearLayout

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
        renderRow(enemyRow, enemies) { idx -> pickFor(enemies, idx, enemyRow) }
        renderRow(allyRow, allies) { idx -> pickFor(allies, idx, allyRow) }

        findViewById<Button>(R.id.btn_verify_ok).setOnClickListener {
            val updated = r.copy(
                enemies = enemies.mapIndexedNotNull { _, n -> n },
                allies = allies.mapIndexedNotNull { _, n -> n },
                userVerified = true,
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
    }

    private fun renderRow(row: LinearLayout, data: List<String?>, onTap: (Int) -> Unit) {
        row.removeAllViews()
        val density = resources.displayMetrics.density
        for (i in 0 until 5) {
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
                setOnClickListener { onTap(i) }
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
                text = data[i] ?: "?"
                textSize = 11f
                setTextColor(android.graphics.Color.parseColor("#E8E3D6"))
                gravity = Gravity.CENTER
            }
            cell.addView(img)
            cell.addView(label)
            row.addView(cell)
        }
    }

    private fun pickFor(target: MutableList<String?>, idx: Int, row: LinearLayout) {
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
                // 정확 매칭 우선, 없으면 prefix 매칭 첫 후보, 그것도 없으면 입력값 그대로 저장.
                val resolved = when {
                    typed.isEmpty() -> null
                    typed in names -> typed
                    else -> names.firstOrNull { it.startsWith(typed) }
                        ?: names.firstOrNull { it.contains(typed) }
                        ?: typed
                }
                target[idx] = resolved
                renderRow(row, target) { i -> pickFor(target, i, row) }
            }
            .setNeutralButton("비우기") { _, _ ->
                target[idx] = null
                renderRow(row, target) { i -> pickFor(target, i, row) }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
