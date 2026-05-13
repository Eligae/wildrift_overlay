package com.eligae.wildrift.overlay.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.api.ApiClient
import com.eligae.wildrift.overlay.api.CachePolicy
import com.eligae.wildrift.overlay.api.NormalizedHero
import com.eligae.wildrift.overlay.api.TierAllResponse
import com.eligae.wildrift.overlay.api.TierCache
import com.eligae.wildrift.overlay.update.NewsCheck
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TierActivity : AppCompatActivity() {

    private lateinit var cohortTabs: TabLayout
    private lateinit var laneTabs: TabLayout
    private lateinit var recycler: RecyclerView
    private lateinit var status: TextView
    private lateinit var modeRecommend: TextView
    private lateinit var modeTable: TextView
    private lateinit var btnRefresh: TextView
    private lateinit var sortHeader: View
    private lateinit var sortWin: TextView
    private lateinit var sortPick: TextView
    private lateinit var sortBan: TextView
    private lateinit var cache: TierCache
    private lateinit var prefs: SharedPreferences

    private val recommendAdapter = RecommendAdapter()
    private val tierAdapter = TierAdapter()
    private var allLanes: Map<String, List<NormalizedHero>> = emptyMap()
    private var mode: Mode = Mode.RECOMMEND
    private var cohort: String = "DIAMOND"
    private var sortKey: SortKey = SortKey.STRENGTH
    private var sortDesc: Boolean = false

    private enum class Mode { RECOMMEND, TABLE }
    private enum class SortKey { STRENGTH, WIN, PICK, BAN }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tier)

        cohortTabs = findViewById(R.id.cohort_tabs)
        laneTabs = findViewById(R.id.tab_layout)
        recycler = findViewById(R.id.recycler_tier)
        status = findViewById(R.id.status_text)
        modeRecommend = findViewById(R.id.mode_recommend)
        modeTable = findViewById(R.id.mode_table)
        btnRefresh = findViewById(R.id.btn_refresh)
        sortHeader = findViewById(R.id.sort_header)
        sortWin = findViewById(R.id.sort_win)
        sortPick = findViewById(R.id.sort_pick)
        sortBan = findViewById(R.id.sort_ban)
        cache = TierCache(this)
        prefs = getSharedPreferences("tier_prefs", Context.MODE_PRIVATE)
        cohort = prefs.getString("cohort", "DIAMOND") ?: "DIAMOND"

        recycler.layoutManager = LinearLayoutManager(this)

        COHORTS.forEach { (key, labelRes) ->
            val tab = cohortTabs.newTab().setTag(key)
            val custom = layoutInflater.inflate(R.layout.tab_cohort, cohortTabs, false)
            custom.findViewById<TextView>(R.id.cohort_label).text = getString(labelRes)
            custom.findViewById<ImageView>(R.id.cohort_emblem).load(emblemUrl(key)) {
                crossfade(true)
            }
            tab.customView = custom
            cohortTabs.addTab(tab)
        }
        val startCohortIdx = COHORTS.indexOfFirst { it.first == cohort }.coerceAtLeast(0)
        cohortTabs.getTabAt(startCohortIdx)?.select()
        cohortTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                cohort = tab.tag as? String ?: "DIAMOND"
                prefs.edit().putString("cohort", cohort).apply()
                loadFromCacheOrFetch()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        LANES.forEach { lane ->
            laneTabs.addTab(laneTabs.newTab().setText(lane))
        }
        laneTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = renderCurrent()
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        modeRecommend.setOnClickListener { setMode(Mode.RECOMMEND) }
        modeTable.setOnClickListener { setMode(Mode.TABLE) }
        btnRefresh.setOnClickListener {
            NewsCheck.invalidate(this)
            load(force = true)
        }

        sortWin.setOnClickListener { toggleSort(SortKey.WIN) }
        sortPick.setOnClickListener { toggleSort(SortKey.PICK) }
        sortBan.setOnClickListener { toggleSort(SortKey.BAN) }

        setMode(Mode.RECOMMEND)
        loadFromCacheOrFetch()
    }

    private fun loadFromCacheOrFetch() {
        val cached = cache.load(cohort)
        if (cached != null) {
            applyResponse(cached, fromCache = true)
            if (CachePolicy.isFresh(cached.fetchedAt)) return
        } else {
            allLanes = emptyMap()
            renderCurrent()
        }
        load(force = false)
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        val active = ContextCompat.getColor(this, R.color.lol_gold)
        val inactive = ContextCompat.getColor(this, R.color.lol_ink_soft)
        modeRecommend.setTextColor(if (mode == Mode.RECOMMEND) active else inactive)
        modeTable.setTextColor(if (mode == Mode.TABLE) active else inactive)
        recycler.adapter = if (mode == Mode.RECOMMEND) recommendAdapter else tierAdapter
        sortHeader.visibility = if (mode == Mode.TABLE) View.VISIBLE else View.GONE
        renderCurrent()
    }

    private fun toggleSort(key: SortKey) {
        if (sortKey == key) {
            sortDesc = !sortDesc
        } else {
            sortKey = key
            sortDesc = true // 첫 클릭은 내림차순
        }
        updateSortLabels()
        renderCurrent()
    }

    private fun updateSortLabels() {
        val active = ContextCompat.getColor(this, R.color.lol_gold)
        val inactive = ContextCompat.getColor(this, R.color.lol_ink_soft)
        fun label(name: String, key: SortKey): String {
            if (sortKey != key) return name
            return name + if (sortDesc) " ↓" else " ↑"
        }
        sortWin.text = label(getString(R.string.sort_win_rate), SortKey.WIN)
        sortPick.text = label(getString(R.string.sort_pick_rate), SortKey.PICK)
        sortBan.text = label(getString(R.string.sort_ban_rate), SortKey.BAN)
        sortWin.setTextColor(if (sortKey == SortKey.WIN) active else inactive)
        sortPick.setTextColor(if (sortKey == SortKey.PICK) active else inactive)
        sortBan.setTextColor(if (sortKey == SortKey.BAN) active else inactive)
    }

    private fun load(force: Boolean) {
        if (allLanes.isEmpty() || force) status.text = getString(R.string.tier_loading)
        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getTierAll(cohort)
                cache.save(cohort, resp)
                applyResponse(resp, fromCache = false)
            } catch (e: Exception) {
                if (allLanes.isEmpty()) {
                    status.text = getString(R.string.tier_error, e.message ?: e.javaClass.simpleName)
                }
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
        val lane = LANES[laneTabs.selectedTabPosition.coerceAtLeast(0)]
        val list = allLanes[lane] ?: emptyList()
        // RECOMMEND 모드는 항상 strength 고정 (서버 정렬 그대로).
        if (mode == Mode.RECOMMEND) {
            recommendAdapter.submit(list)
            return
        }
        val sorted = when (sortKey) {
            SortKey.STRENGTH -> list
            SortKey.WIN -> list.sortedBy { it.winRate }.let { if (sortDesc) it.reversed() else it }
            SortKey.PICK -> list.sortedBy { it.pickRate }.let { if (sortDesc) it.reversed() else it }
            SortKey.BAN -> list.sortedBy { it.banRate }.let { if (sortDesc) it.reversed() else it }
        }
        val highlight = when (sortKey) {
            SortKey.PICK -> TierAdapter.HighlightKey.PICK
            SortKey.BAN -> TierAdapter.HighlightKey.BAN
            else -> TierAdapter.HighlightKey.WIN
        }
        tierAdapter.submit(sorted, highlight)
    }

    private fun emblemUrl(cohort: String): String {
        val tier = when (cohort) {
            "DIAMOND" -> "diamond"
            "MASTER" -> "master"
            "GRANDMASTER" -> "grandmaster"
            "CHALLENGER" -> "challenger"
            else -> "diamond"
        }
        return "https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-static-assets/global/default/images/ranked-emblem/emblem-$tier.png"
    }

    companion object {
        private val LANES = listOf("TOP", "JUG", "MID", "ADC", "SUP")
        private val COHORTS = listOf(
            "DIAMOND" to R.string.cohort_diamond,
            "MASTER" to R.string.cohort_master,
            "GRANDMASTER" to R.string.cohort_grandmaster,
            "CHALLENGER" to R.string.cohort_challenger,
        )
    }
}
