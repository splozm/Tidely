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
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class TideListItem {
    data class SectionHeader(val title: String, val subtitle: String) : TideListItem()
    data class Event(val event: TidalEvent, val isNext: Boolean) : TideListItem()
}

class TideEventAdapter : ListAdapter<TideListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.UK)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TideListItem.SectionHeader -> 0
            is TideListItem.Event -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            SectionViewHolder(inflater.inflate(R.layout.item_section_header, parent, false))
        } else {
            EventViewHolder(inflater.inflate(R.layout.item_tide_event, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is SectionViewHolder && item is TideListItem.SectionHeader) {
            holder.bind(item)
        } else if (holder is EventViewHolder && item is TideListItem.Event) {
            holder.bind(item)
        }
    }

    inner class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvSectionTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvSectionSubtitle)

        fun bind(item: TideListItem.SectionHeader) {
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle.uppercase(Locale.UK)
        }
    }

    inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvEventType: TextView = view.findViewById(R.id.tvEventType)
        private val tvEventLabel: TextView = view.findViewById(R.id.tvEventLabel)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val tvHeight: TextView = view.findViewById(R.id.tvHeight)

        fun bind(item: TideListItem.Event) {
            val event = item.event
            val context = itemView.context

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

            val labelRes = when {
                item.isNext && isHigh -> R.string.next_high
                item.isNext && !isHigh -> R.string.next_low
                !item.isNext && isHigh -> R.string.following_high
                else -> R.string.following_low
            }
            tvEventLabel.text = context.getString(labelRes)
            tvTime.text = timeFormat.format(event.dateTime)
            tvHeight.text = context.getString(R.string.height_format, String.format(Locale.UK, "%.1f", event.height))
        }
    }

    fun submitTidalEvents(events: List<TidalEvent>) {
        if (events.isEmpty()) {
            submitList(emptyList())
            return
        }

        val items = mutableListOf<TideListItem>()
        val calendar = Calendar.getInstance()
        val now = Date()

        // Find the index of the next tide event
        val nextEventIndex = events.indexOfFirst { it.dateTime.after(now) }
        
        val groupedByDay = events.groupBy { event ->
            calendar.time = event.dateTime
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }

        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        todayCalendar.set(Calendar.SECOND, 0)
        todayCalendar.set(Calendar.MILLISECOND, 0)
        val todayMillis = todayCalendar.timeInMillis

        val tomorrowCalendar = Calendar.getInstance()
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, 1)
        tomorrowCalendar.set(Calendar.HOUR_OF_DAY, 0)
        tomorrowCalendar.set(Calendar.MINUTE, 0)
        tomorrowCalendar.set(Calendar.SECOND, 0)
        tomorrowCalendar.set(Calendar.MILLISECOND, 0)
        val tomorrowMillis = tomorrowCalendar.timeInMillis

        groupedByDay.keys.sorted().forEach { dayMillis ->
            val dayEvents = groupedByDay[dayMillis] ?: return@forEach
            val title = when (dayMillis) {
                todayMillis -> "TODAY"
                tomorrowMillis -> "TOMORROW"
                else -> SimpleDateFormat("EEEE", Locale.UK).format(Date(dayMillis)).uppercase()
            }
            val subtitle = SimpleDateFormat("EEE d MMM", Locale.UK).format(Date(dayMillis))
            
            items.add(TideListItem.SectionHeader(title, subtitle))
            dayEvents.forEach { event ->
                items.add(TideListItem.Event(event, event == events.getOrNull(nextEventIndex)))
            }
        }
        submitList(items)
    }

    private class DiffCallback : DiffUtil.ItemCallback<TideListItem>() {
        override fun areItemsTheSame(oldItem: TideListItem, newItem: TideListItem): Boolean {
            return when {
                oldItem is TideListItem.SectionHeader && newItem is TideListItem.SectionHeader -> 
                    oldItem.title == newItem.title
                oldItem is TideListItem.Event && newItem is TideListItem.Event ->
                    oldItem.event.stationId == newItem.event.stationId && 
                    oldItem.event.dateTime == newItem.event.dateTime
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: TideListItem, newItem: TideListItem): Boolean {
            return oldItem == newItem
        }
    }
}
