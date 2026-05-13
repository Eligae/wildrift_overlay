package com.eligae.wildrift.overlay.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.api.ChampionsCache
import com.eligae.wildrift.overlay.history.MatchRecord
import com.eligae.wildrift.overlay.model.MatchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchHistoryAdapter(
    private val cache: ChampionsCache,
    private val onClick: (MatchRecord) -> Unit,
) : RecyclerView.Adapter<MatchHistoryAdapter.VH>() {

    private var items: List<MatchRecord> = emptyList()

    fun submit(list: List<MatchRecord>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_match_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val result: TextView = itemView.findViewById(R.id.row_result)
        private val enemyRow: LinearLayout = itemView.findViewById(R.id.row_enemies)
        private val allyRow: LinearLayout = itemView.findViewById(R.id.row_allies)
        private val time: TextView = itemView.findViewById(R.id.row_time)

        fun bind(r: MatchRecord) {
            when (r.result) {
                MatchResult.WIN -> {
                    result.text = "W"
                    result.setTextColor(Color.parseColor("#C89B3C"))
                }
                MatchResult.LOSE -> {
                    result.text = "L"
                    result.setTextColor(Color.parseColor("#0AC8B9"))
                }
                MatchResult.UNKNOWN -> {
                    result.text = "?"
                    result.setTextColor(Color.parseColor("#6B6557"))
                }
            }
            time.text = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA).format(Date(r.endedAtMs)) +
                if (r.userVerified) " ✓" else ""

            renderAvatarRow(enemyRow, r.enemies, userSlot = null)
            renderAvatarRow(allyRow, r.allies, userSlot = r.userSlot)
            itemView.setOnClickListener { onClick(r) }
        }

        private fun renderAvatarRow(row: LinearLayout, names: List<String>, userSlot: Int?) {
            row.removeAllViews()
            val density = itemView.context.resources.displayMetrics.density
            val gap = (2 * density).toInt()
            val cellHeight = (30 * density).toInt()
            for (i in 0 until 5) {
                val name = names.getOrNull(i) ?: ""
                val cell = android.widget.FrameLayout(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, cellHeight, 1f).apply {
                        if (i < 4) marginEnd = gap
                    }
                }
                val iv = ImageView(itemView.context).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val avatar = if (name.isNotBlank()) cache.avatarFor(name) else null
                    if (avatar != null) load(avatar)
                    else background = ContextCompat.getDrawable(
                        itemView.context, R.drawable.bg_avatar_empty,
                    )
                }
                cell.addView(iv)
                if (userSlot != null && i == userSlot) {
                    // 동일 크기 유지하면서 금색 테두리만 오버레이 (행 어긋남 방지).
                    val overlay = View(itemView.context).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                        background = ContextCompat.getDrawable(
                            itemView.context, R.drawable.bg_user_slot,
                        )
                    }
                    cell.addView(overlay)
                }
                row.addView(cell)
            }
        }
    }
}
