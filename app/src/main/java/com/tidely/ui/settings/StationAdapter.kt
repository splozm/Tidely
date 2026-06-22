package com.tidely.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tidely.R
import com.tidely.data.model.Station

class StationAdapter(
    private val onStationClick: (Station) -> Unit
) : ListAdapter<Station, StationAdapter.ViewHolder>(DiffCallback()) {

    private var selectedStationId: String? = null

    fun setSelectedStationId(stationId: String?) {
        val oldSelectedId = selectedStationId
        selectedStationId = stationId

        // Notify items that changed
        currentList.indexOfFirst { it.id == oldSelectedId }.takeIf { it != -1 }
            ?.let { notifyItemChanged(it) }
        currentList.indexOfFirst { it.id == stationId }.takeIf { it != -1 }
            ?.let { notifyItemChanged(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStationName: TextView = itemView.findViewById(R.id.tvStationName)
        private val tvCountry: TextView = itemView.findViewById(R.id.tvCountry)
        private val tvSelected: TextView = itemView.findViewById(R.id.tvSelected)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStationClick(getItem(position))
                }
            }
        }

        fun bind(station: Station) {
            tvStationName.text = station.name
            tvCountry.text = station.country

            val isSelected = station.id == selectedStationId
            tvSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem == newItem
        }
    }
}
