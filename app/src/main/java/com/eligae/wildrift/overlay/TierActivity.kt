package com.eligae.wildrift.overlay

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligae.wildrift.overlay.api.ApiClient
import com.eligae.wildrift.overlay.api.NormalizedHero
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TierActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recycler: RecyclerView
    private lateinit var status: TextView
    private val adapter = TierAdapter()
    private var allLanes: Map<String, List<NormalizedHero>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tier)

        tabLayout = findViewById(R.id.tab_layout)
        recycler = findViewById(R.id.recycler_tier)
        status = findViewById(R.id.status_text)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        LANES.forEach { lane ->
            tabLayout.addTab(tabLayout.newTab().setText(lane))
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showLane(LANES[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        load()
    }

    private fun load() {
        status.text = getString(R.string.tier_loading)
        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getTierAll()
                allLanes = resp.lanes
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
                    .format(Date(resp.fetchedAt))
                status.text = getString(R.string.tier_updated, fmt)
                showLane(LANES[tabLayout.selectedTabPosition.coerceAtLeast(0)])
            } catch (e: Exception) {
                status.text = getString(R.string.tier_error, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun showLane(lane: String) {
        adapter.submit(allLanes[lane] ?: emptyList())
    }

    companion object {
        private val LANES = listOf("TOP", "JUG", "MID", "ADC", "SUP")
    }
}
