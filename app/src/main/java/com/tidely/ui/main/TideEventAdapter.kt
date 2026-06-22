package com.tidely.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tidely.R
import com.tidely.data.model.TidalEvent
import com.tidely.data.model.TideType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TideEventAdapter : ListAdapter<TidalEvent, TideEventAdapter.ViewHolder>(DiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.UK)
    private val now = Date()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tide_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventType: TextView = itemView.findViewById(R.id.tvEventType)
        private val tvEventLabel: TextView = itemView.findViewById(R.id.tvEventLabel)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvHeight: TextView = itemView.findViewById(R.id.tvHeight)

        fun bind(event: TidalEvent, position: Int) {
            val context = itemView.context

            // Event type and color
            val isHigh = event.eventType == TideType.HIGH
            tvEventType.text = if (isHigh) {
                context.getString(R.string.tide_high)
            } else {
                context.getString(R.string.tide_low)
            }
            tvEventType.setTextColor(
                context.getColor(
                    if (isHigh) R.color.tide_high else R.color.tide_low
                )
            )

            // Label (Next/Following)
            val isNext = position < 2
            val labelRes = when {
                isNext && isHigh -> R.string.next_high
                isNext && !isHigh -> R.string.next_low
                !isNext && isHigh -> R.string.following_high
                else -> R.string.following_low
            }
            tvEventLabel.text = context.getString(labelRes)

            // Time
            tvTime.text = timeFormat.format(event.dateTime)

            // Height
            tvHeight.text = context.getString(R.string.height_format, event.height.toString())
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TidalEvent>() {
        override fun areItemsTheSame(oldItem: TidalEvent, newItem: TidalEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TidalEvent, newItem: TidalEvent): Boolean {
            return oldItem == newItem
        }
    }
}
