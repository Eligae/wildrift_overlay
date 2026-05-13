package com.eligae.wildrift.overlay.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eligae.wildrift.overlay.R
import com.eligae.wildrift.overlay.api.NormalizedHero

class TierAdapter(
    private var items: List<NormalizedHero> = emptyList(),
) : RecyclerView.Adapter<TierAdapter.VH>() {

    enum class HighlightKey { WIN, PICK, BAN }

    private var highlight: HighlightKey = HighlightKey.WIN

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.rank)
        val avatar: ImageView = view.findViewById(R.id.avatar)
        val name: TextView = view.findViewById(R.id.name)
        val subRates: TextView = view.findViewById(R.id.sub_rates)
        val bigRate: TextView = view.findViewById(R.id.win_rate)
        val bigLabel: TextView = view.findViewById(R.id.highlight_label)

        init {
            avatar.clipToOutline = true
            avatar.outlineProvider = ViewOutlineProvider.BACKGROUND
        }
    }

    fun submit(list: List<NormalizedHero>, highlight: HighlightKey = HighlightKey.WIN) {
        items = list
        this.highlight = highlight
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tier_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val hero = items[position]
        holder.rank.text = (position + 1).toString()
        holder.name.text = hero.displayName

        val w = hero.winRate * 100
        val p = hero.pickRate * 100
        val b = hero.banRate * 100
        when (highlight) {
            HighlightKey.WIN -> {
                holder.bigRate.text = "${w.fmt1()}%"
                holder.bigLabel.setText(R.string.win_label)
                holder.subRates.text = "P ${p.fmt1()}%  ·  B ${b.fmt1()}%"
            }
            HighlightKey.PICK -> {
                holder.bigRate.text = "${p.fmt1()}%"
                holder.bigLabel.setText(R.string.pick_label)
                holder.subRates.text = "W ${w.fmt1()}%  ·  B ${b.fmt1()}%"
            }
            HighlightKey.BAN -> {
                holder.bigRate.text = "${b.fmt1()}%"
                holder.bigLabel.setText(R.string.ban_label)
                holder.subRates.text = "W ${w.fmt1()}%  ·  P ${p.fmt1()}%"
            }
        }

        if (hero.avatar.isNotBlank()) {
            holder.avatar.load(hero.avatar) {
                crossfade(true)
            }
        } else {
            holder.avatar.setImageDrawable(null)
        }

        val rowHighlight = position < 3
        holder.itemView.setBackgroundResource(
            if (rowHighlight) R.drawable.bg_tier_row_top else R.drawable.bg_tier_row
        )
    }

    override fun getItemCount(): Int = items.size

    private fun Double.fmt1(): String = "%.1f".format(this)
}
