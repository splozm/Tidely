package com.tidely.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.snackbar.Snackbar
import com.tidely.R
import com.tidely.TidelyApplication
import com.tidely.data.model.TideState
import com.tidely.util.LocationHelper
import com.tidely.util.PreferencesManager

class TidesFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: TideEventAdapter

    private lateinit var tvStationName: TextView
    private lateinit var tvTideState: TextView
    private lateinit var tideChart: LineChart
    private lateinit var rvTideEvents: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.loadCurrentStation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tides, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as TidelyApplication
        val preferencesManager = PreferencesManager(requireContext())
        val locationHelper = LocationHelper(requireContext())

        // Use a Factory to provide dependencies to the ViewModel
        val factory = MainViewModelFactory(
            app.tideRepository,
            preferencesManager,
            locationHelper
        )
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[MainViewModel::class.java]

        setupViews(view)
        setupRecyclerView()
        observeViewModel()
        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        // Reload station when returning to this tab (in case user selected a new station)
        viewModel.loadCurrentStation()
    }

    private fun setupViews(view: View) {
        tvStationName = view.findViewById(R.id.tvStationName)
        tvTideState = view.findViewById(R.id.tvTideState)
        tideChart = view.findViewById(R.id.tideChart)
        rvTideEvents = view.findViewById(R.id.rvTideEvents)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = TideEventAdapter()
        rvTideEvents.layoutManager = LinearLayoutManager(requireContext())
        rvTideEvents.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.selectedStation.observe(viewLifecycleOwner) { station ->
            station?.let {
                tvStationName.text = it.name
            }
        }

        viewModel.tideState.observe(viewLifecycleOwner) { state ->
            tvTideState.text = when (state) {
                TideState.RISING -> getString(R.string.tide_state_rising)
                TideState.FALLING -> getString(R.string.tide_state_falling)
            }
        }

        viewModel.tidalEvents.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
            // Hide chart until we implement it (prevents "No chart data" message)
            // When we have data, still hide it until chart is implemented
            tideChart.visibility = View.GONE
            // TODO: Populate chart with tide curve data, then show it
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun checkLocationPermission() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}
