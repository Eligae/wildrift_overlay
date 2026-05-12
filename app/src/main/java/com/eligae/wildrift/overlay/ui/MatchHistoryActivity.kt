package com.eligae.wildrift.overlay.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.api.ChampionsCache
import com.eligae.wildrift.overlay.history.MatchHistoryStore
import com.eligae.wildrift.overlay.model.MatchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchHistoryActivity : AppCompatActivity() {

    private lateinit var store: MatchHistoryStore
    private lateinit var adapter: MatchHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_history)

        store = MatchHistoryStore(this)
        val cache = ChampionsCache(this)
        adapter = MatchHistoryAdapter(cache) { record ->
            startActivity(Intent(this, VerifyMatchActivity::class.java).apply {
                putExtra("match_id", record.id)
            })
        }
        findViewById<RecyclerView>(R.id.history_list).apply {
            layoutManager = LinearLayoutManager(this@MatchHistoryActivity)
            adapter = this@MatchHistoryActivity.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val list = store.loadAll()
        adapter.submit(list)

        val emptyView = findViewById<TextView>(R.id.history_empty)
        emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        val today = todayKey()
        val todayList = list.filter { todayKey(it.endedAtMs) == today }
        val w = todayList.count { it.result == MatchResult.WIN }
        val l = todayList.count { it.result == MatchResult.LOSE }
        findViewById<TextView>(R.id.history_summary).text =
            "오늘 ${todayList.size}판 · ${w}승 ${l}패"
    }

    private fun todayKey(ms: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date(ms))
}
