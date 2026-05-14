package com.eligae.wildrift.overlay.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.api.ChampionSkinsSync
import com.eligae.wildrift.overlay.update.NewsCheck
import com.eligae.wildrift.overlay.update.VersionCheck
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var btnGrant: MaterialButton
    private lateinit var btnStart: Button
    private lateinit var btnCapture: Button
    private lateinit var btnTier: Button
    private lateinit var btnComposition: Button
    private lateinit var btnCalibration: Button
    private lateinit var btnMatchHistory: Button
    private lateinit var updateBanner: LinearLayout
    private lateinit var updateBannerText: TextView
    private lateinit var updateBannerOpen: TextView
    private lateinit var updateBannerClose: TextView
    private lateinit var newsBanner: LinearLayout
    private lateinit var newsBannerText: TextView
    private lateinit var newsBannerOpen: TextView
    private lateinit var newsBannerClose: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGrant = findViewById(R.id.btn_grant)
        btnStart = findViewById(R.id.btn_start)
        btnCapture = findViewById(R.id.btn_capture)
        btnTier = findViewById(R.id.btn_tier)
        btnComposition = findViewById(R.id.btn_composition)
        btnCalibration = findViewById(R.id.btn_calibration)
        btnMatchHistory = findViewById(R.id.btn_match_history)
        updateBanner = findViewById(R.id.update_banner)
        updateBannerText = findViewById(R.id.update_banner_text)
        updateBannerOpen = findViewById(R.id.update_banner_open)
        updateBannerClose = findViewById(R.id.update_banner_close)
        newsBanner = findViewById(R.id.news_banner)
        newsBannerText = findViewById(R.id.news_banner_text)
        newsBannerOpen = findViewById(R.id.news_banner_open)
        newsBannerClose = findViewById(R.id.news_banner_close)

        val wip = View.OnClickListener {
            Toast.makeText(this, R.string.wip_toast, Toast.LENGTH_SHORT).show()
        }
        btnGrant.setOnClickListener(wip)
        btnStart.setOnClickListener(wip)
        btnCapture.setOnClickListener(wip)
        btnComposition.setOnClickListener(wip)
        btnCalibration.setOnClickListener(wip)
        btnMatchHistory.setOnClickListener(wip)
        btnTier.setOnClickListener { startActivity(Intent(this, TierActivity::class.java)) }

        checkUpdate()
        checkNews()
        lifecycleScope.launch { ChampionSkinsSync.syncIfChanged(this@MainActivity) }
    }

    private fun checkNews() {
        lifecycleScope.launch {
            val news = NewsCheck.checkLatest(this@MainActivity) ?: return@launch
            newsBannerText.text = "${getString(R.string.news_banner_prefix)}: ${news.title}"
            newsBannerOpen.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(news.url)))
                NewsCheck.markSeen(this@MainActivity, news.url)
                newsBanner.visibility = View.GONE
            }
            newsBannerClose.setOnClickListener {
                NewsCheck.markSeen(this@MainActivity, news.url)
                newsBanner.visibility = View.GONE
            }
            newsBanner.visibility = View.VISIBLE
        }
    }

    private fun checkUpdate() {
        lifecycleScope.launch {
            val result = VersionCheck.check(this@MainActivity) ?: return@launch
            updateBannerText.text = getString(R.string.update_available, result.newTag)
            updateBannerOpen.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.pageUrl)))
            }
            updateBannerClose.setOnClickListener {
                updateBanner.visibility = View.GONE
                VersionCheck.dismiss(this@MainActivity, result.newTag)
            }
            updateBanner.visibility = View.VISIBLE
        }
    }
}
