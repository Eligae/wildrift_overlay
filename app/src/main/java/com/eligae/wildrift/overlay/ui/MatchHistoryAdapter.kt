package com.eligae.wildrift.overlay.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
        private val avatars: LinearLayout = itemView.findViewById(R.id.row_avatars)
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

            avatars.removeAllViews()
            val density = itemView.context.resources.displayMetrics.density
            val size = (28 * density).toInt()
            for (name in r.enemies) {
                val iv = ImageView(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginEnd = (4 * density).toInt()
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val avatar = cache.avatarFor(name)
                    if (avatar != null) load(avatar) else setBackgroundColor(Color.DKGRAY)
                }
                avatars.addView(iv)
            }
            itemView.setOnClickListener { onClick(r) }
        }
    }
}
