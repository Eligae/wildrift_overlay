package com.eligae.wildrift.overlay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eligae.wildrift.overlay.api.NormalizedHero

class TierAdapter(
    private var items: List<NormalizedHero> = emptyList(),
) : RecyclerView.Adapter<TierAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.rank)
        val name: TextView = view.findViewById(R.id.name)
        val rates: TextView = view.findViewById(R.id.rates)
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
        holder.rates.text =
            "W ${(hero.winRate * 100).fmt1()}%  P ${(hero.pickRate * 100).fmt1()}%  B ${(hero.banRate * 100).fmt1()}%"
    }

    override fun getItemCount(): Int = items.size

    private fun Double.fmt1(): String = "%.1f".format(this)
}
