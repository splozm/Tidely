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
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import com.tidely.R
import com.tidely.TidelyApplication
import com.tidely.data.model.TidalEvent
import com.tidely.data.model.TideState
import com.tidely.util.LocationHelper
import com.tidely.util.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TidesFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: TideEventAdapter

    private lateinit var tvStationName: TextView
    private lateinit var tvTideState: TextView
    private lateinit var tideStateBadge: View
    private lateinit var tvChartHeaderLeft: TextView
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

        val factory = MainViewModelFactory(
            app.tideRepository,
            preferencesManager,
            locationHelper
        )
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[MainViewModel::class.java]

        setupViews(view)
        setupRecyclerView()
        setupChart()
        observeViewModel()
        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCurrentStation()
    }

    private fun setupViews(view: View) {
        tvStationName = view.findViewById(R.id.tvStationName)
        tvTideState = view.findViewById(R.id.tvTideState)
        tideStateBadge = view.findViewById(R.id.tideStateBadge)
        tvChartHeaderLeft = view.findViewById(R.id.tvChartHeaderLeft)
        tideChart = view.findViewById(R.id.tideChart)
        rvTideEvents = view.findViewById(R.id.rvTideEvents)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = TideEventAdapter()
        rvTideEvents.layoutManager = LinearLayoutManager(requireContext())
        rvTideEvents.adapter = adapter
    }

    private fun setupChart() {
        tideChart.apply {
            setTouchEnabled(false)
            setDragEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            description.isEnabled = false
            legend.isEnabled = false
            minOffset = 0f
            
            xAxis.apply {
                isEnabled = true
                setDrawLabels(false)
                setDrawAxisLine(false)
                setDrawGridLines(false)
            }
            axisLeft.apply {
                isEnabled = true
                setDrawLabels(false)
                setDrawAxisLine(false)
                setDrawGridLines(false)
                axisMinimum = -1f // Extra space at bottom
            }
            axisRight.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.selectedStation.observe(viewLifecycleOwner) { station ->
            station?.let {
                tvStationName.text = it.name
            }
        }

        viewModel.tideState.observe(viewLifecycleOwner) { state ->
            tideStateBadge.visibility = View.VISIBLE
            tvTideState.text = when (state) {
                TideState.RISING -> getString(R.string.tide_state_rising)
                TideState.FALLING -> getString(R.string.tide_state_falling)
            }
        }

        viewModel.tidalEvents.observe(viewLifecycleOwner) { events ->
            adapter.submitTidalEvents(events)
            updateHeader()
            updateChart(events)
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

    private fun updateHeader() {
        val sdf = SimpleDateFormat("EEE d MMM", Locale.UK)
        val today = sdf.format(Date()).uppercase(Locale.UK)
        tvChartHeaderLeft.text = getString(R.string.today_header_format, today)
    }

    private fun updateChart(events: List<TidalEvent>) {
        if (events.isEmpty()) return

        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        // Window: Full current day (00:00 to 24:00) to match markers
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimeMillis = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTimeMillis = calendar.timeInMillis

        // Filter events for the full day window, plus some context for smooth curve
        val margin = 12 * 60 * 60 * 1000L // 12 hours margin
        val displayEvents = events.filter { it.dateTime.time in (startTimeMillis - margin)..(endTimeMillis + margin) }
            .sortedBy { it.dateTime.time }

        if (displayEvents.size < 2) return

        val entries = mutableListOf<Entry>()
        displayEvents.forEach { event ->
            entries.add(Entry(event.dateTime.time.toFloat(), event.height.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Tide").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawCircles(false)
            setDrawValues(false)
            color = ContextCompat.getColor(requireContext(), R.color.ocean_teal)
            lineWidth = 3f
            setDrawFilled(false)
        }

        val lineData = LineData(dataSet)
        
        // Add intersection point dot
        val nowF = now.toFloat()
        val before = entries.lastOrNull { it.x <= nowF }
        val after = entries.firstOrNull { it.x > nowF }
        
        if (before != null && after != null) {
            val t = (nowF - before.x) / (after.x - before.x)
            val interpolatedHeight = before.y + t * (after.y - before.y)
            
            val dotDataSet = LineDataSet(listOf(Entry(nowF, interpolatedHeight)), "NowDot").apply {
                setDrawCircles(true)
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                circleRadius = 5f
                setDrawCircleHole(false)
                setDrawValues(false)
            }
            lineData.addDataSet(dotDataSet)
        }

        tideChart.data = lineData
        tideChart.xAxis.apply {
            axisMinimum = startTimeMillis.toFloat()
            axisMaximum = endTimeMillis.toFloat()
        }

        // Horizontal MSL line (Average of visible peaks/troughs)
        tideChart.axisLeft.removeAllLimitLines()
        val avgHeight = displayEvents.map { it.height }.average().toFloat()
        val mslLine = LimitLine(avgHeight).apply {
            lineColor = ContextCompat.getColor(requireContext(), R.color.border_light)
            lineWidth = 1f
        }
        tideChart.axisLeft.addLimitLine(mslLine)

        // Vertical current time indicator
        tideChart.xAxis.removeAllLimitLines()
        val nowLine = LimitLine(now.toFloat()).apply {
            lineColor = ContextCompat.getColor(requireContext(), R.color.text_tertiary)
            lineWidth = 1.2f
            enableDashedLine(12f, 10f, 0f)
        }
        tideChart.xAxis.addLimitLine(nowLine)
        
        tideChart.invalidate()
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
