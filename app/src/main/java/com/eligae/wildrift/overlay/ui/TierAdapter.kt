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

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.rank)
        val avatar: ImageView = view.findViewById(R.id.avatar)
        val name: TextView = view.findViewById(R.id.name)
        val subRates: TextView = view.findViewById(R.id.sub_rates)
        val winRate: TextView = view.findViewById(R.id.win_rate)

        init {
            avatar.clipToOutline = true
            avatar.outlineProvider = ViewOutlineProvider.BACKGROUND
        }
    }

    fun submit(list: List<NormalizedHero>) {
        items = list
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
        holder.winRate.text = "${(hero.winRate * 100).fmt1()}%"
        holder.subRates.text =
            "P ${(hero.pickRate * 100).fmt1()}%  ·  B ${(hero.banRate * 100).fmt1()}%"

        if (hero.avatar.isNotBlank()) {
            holder.avatar.load(hero.avatar) {
                crossfade(true)
            }
        } else {
            holder.avatar.setImageDrawable(null)
        }

        val highlight = position < 3
        holder.itemView.setBackgroundResource(
            if (highlight) R.drawable.bg_tier_row_top else R.drawable.bg_tier_row
        )
    }

    override fun getItemCount(): Int = items.size

    private fun Double.fmt1(): String = "%.1f".format(this)
}
