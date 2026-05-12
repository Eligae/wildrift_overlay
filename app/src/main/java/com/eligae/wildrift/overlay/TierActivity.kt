package com.eligae.wildrift.overlay

import android.os.Bundle
import androidx.core.content.ContextCompat
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligae.wildrift.overlay.api.ApiClient
import com.eligae.wildrift.overlay.api.NormalizedHero
import com.eligae.wildrift.overlay.api.TierAllResponse
import com.eligae.wildrift.overlay.api.TierCache
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TierActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recycler: RecyclerView
    private lateinit var status: TextView
    private lateinit var modeRecommend: TextView
    private lateinit var modeTable: TextView
    private lateinit var cache: TierCache

    private val recommendAdapter = RecommendAdapter()
    private val tierAdapter = TierAdapter()
    private var allLanes: Map<String, List<NormalizedHero>> = emptyMap()
    private var mode: Mode = Mode.RECOMMEND

    private enum class Mode { RECOMMEND, TABLE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tier)

        tabLayout = findViewById(R.id.tab_layout)
        recycler = findViewById(R.id.recycler_tier)
        status = findViewById(R.id.status_text)
        modeRecommend = findViewById(R.id.mode_recommend)
        modeTable = findViewById(R.id.mode_table)
        cache = TierCache(this)

        recycler.layoutManager = LinearLayoutManager(this)

        LANES.forEach { lane ->
            tabLayout.addTab(tabLayout.newTab().setText(lane))
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = renderCurrent()
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        modeRecommend.setOnClickListener { setMode(Mode.RECOMMEND) }
        modeTable.setOnClickListener { setMode(Mode.TABLE) }

        setMode(Mode.RECOMMEND)

        // 캐시 즉시 표시 (있으면)
        cache.load()?.let { applyResponse(it, fromCache = true) }
        load()
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        val active = ContextCompat.getColor(this, R.color.lol_gold)
        val inactive = ContextCompat.getColor(this, R.color.lol_ink_soft)
        modeRecommend.setTextColor(if (mode == Mode.RECOMMEND) active else inactive)
        modeTable.setTextColor(if (mode == Mode.TABLE) active else inactive)
        recycler.adapter = if (mode == Mode.RECOMMEND) recommendAdapter else tierAdapter
        renderCurrent()
    }

    private fun load() {
        if (allLanes.isEmpty()) status.text = getString(R.string.tier_loading)
        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getTierAll()
                cache.save(resp)
                applyResponse(resp, fromCache = false)
            } catch (e: Exception) {
                if (allLanes.isEmpty()) {
                    status.text = getString(R.string.tier_error, e.message ?: e.javaClass.simpleName)
                }
                // 캐시가 이미 표시 중이면 status는 그대로 둠
            }
        }
    }

    private fun applyResponse(resp: TierAllResponse, fromCache: Boolean) {
        allLanes = resp.lanes
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(resp.fetchedAt))
        status.text = if (fromCache) "캐시: $fmt" else getString(R.string.tier_updated, fmt)
        renderCurrent()
    }

    private fun renderCurrent() {
        val lane = LANES[tabLayout.selectedTabPosition.coerceAtLeast(0)]
        val list = allLanes[lane] ?: emptyList()
        if (mode == Mode.RECOMMEND) recommendAdapter.submit(list)
        else tierAdapter.submit(list)
    }

    companion object {
        private val LANES = listOf("TOP", "JUG", "MID", "ADC", "SUP")
    }
}
