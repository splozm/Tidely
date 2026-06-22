package com.tidely.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.tidely.R
import com.tidely.util.PreferencesManager

class SettingsFragment : Fragment() {

    private lateinit var preferencesManager: PreferencesManager

    private lateinit var switchGps: SwitchMaterial
    private lateinit var layoutStationOverride: View
    private lateinit var tvStationName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        setupViews(view)
        loadSettings()
        setupListeners()
    }

    private fun setupViews(view: View) {
        switchGps = view.findViewById(R.id.switchGps)
        layoutStationOverride = view.findViewById(R.id.layoutStationOverride)
        tvStationName = view.findViewById(R.id.tvStationName)
    }

    private fun loadSettings() {
        switchGps.isChecked = preferencesManager.useGps
        tvStationName.text = preferencesManager.selectedStationName
            ?: getString(R.string.label_choose_station)
    }

    private fun setupListeners() {
        switchGps.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.useGps = isChecked
        }

        layoutStationOverride.setOnClickListener {
            // Navigate to station picker
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.container, StationPickerFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh station name in case it changed
        tvStationName.text = preferencesManager.selectedStationName
            ?: getString(R.string.label_choose_station)
    }
}
