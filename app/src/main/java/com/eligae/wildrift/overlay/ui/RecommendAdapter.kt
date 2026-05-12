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

class RecommendAdapter(
    private var items: List<NormalizedHero> = emptyList(),
) : RecyclerView.Adapter<RecommendAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val rankBig: TextView = view.findViewById(R.id.rank_big)
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
        items = list.take(MAX_CARDS)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommend_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val hero = items[position]
        holder.rankBig.text = (position + 1).toString()
        holder.name.text = hero.displayName
        holder.winRate.text = "${(hero.winRate * 100).fmt1()}%"
        holder.subRates.text =
            "픽 ${(hero.pickRate * 100).fmt1()}%  ·  밴 ${(hero.banRate * 100).fmt1()}%"

        if (hero.avatar.isNotBlank()) {
            holder.avatar.load(hero.avatar) {
                crossfade(true)
            }
        } else {
            holder.avatar.setImageDrawable(null)
        }

        holder.itemView.setBackgroundResource(
            if (position < 3) R.drawable.bg_recommend_card_top else R.drawable.bg_recommend_card
        )
    }

    override fun getItemCount(): Int = items.size

    private fun Double.fmt1(): String = "%.1f".format(this)

    companion object {
        private const val MAX_CARDS = 5
    }
}
