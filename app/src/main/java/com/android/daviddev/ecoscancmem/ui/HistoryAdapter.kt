package com.android.daviddev.ecoscancmem.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.daviddev.ecoscancmem.R
import com.android.daviddev.ecoscancmem.data.HistoryItem
import com.android.daviddev.ecoscancmem.data.model.EcopointColor
import com.android.daviddev.ecoscancmem.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onClick: (HistoryItem) -> Unit,
    private val onLongClick: (HistoryItem) -> Boolean
) : ListAdapter<HistoryItem, HistoryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            binding.tvItemMaterial.text = item.materialName
            binding.tvItemCode.text = item.recycleCode
            binding.tvItemCo2.text = if (item.co2SavedGrams > 0)
                "−${item.co2SavedGrams} g CO₂" else ""
            binding.tvItemTimestamp.text = formatTimestamp(item.timestamp)

            // Cor do ícone consoante o ecoponto
            val (bgColor, icon) = ecopointStyle(item.ecopointColor)
            binding.ivEcopointColor.setBackgroundResource(bgColor)
            binding.ivEcopointColor.setImageResource(icon)

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener { onLongClick(item) }
        }

        private fun ecopointStyle(color: EcopointColor) = when (color) {
            EcopointColor.YELLOW -> R.drawable.bg_feat_teal to R.drawable.ic_ecopoint
            EcopointColor.BLUE -> R.drawable.bg_feat_blue to R.drawable.ic_ecopoint
            EcopointColor.GREEN -> R.drawable.bg_feat_green to R.drawable.ic_ecopoint
            EcopointColor.RED -> R.drawable.bg_feat_amber to R.drawable.ic_ecopoint
            EcopointColor.NONE -> R.drawable.bg_feat_gray to R.drawable.ic_trash
        }

        private fun formatTimestamp(ts: Long): String {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            return sdf.format(Date(ts))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(a: HistoryItem, b: HistoryItem) = a.id == b.id
        override fun areContentsTheSame(a: HistoryItem, b: HistoryItem) = a == b
    }
}