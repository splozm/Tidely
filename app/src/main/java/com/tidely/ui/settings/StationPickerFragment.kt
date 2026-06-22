package com.tidely.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.tidely.R
import com.tidely.TidelyApplication
import com.tidely.util.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StationPickerFragment : Fragment() {

    private lateinit var viewModel: StationPickerViewModel
    private lateinit var adapter: StationAdapter

    private lateinit var etSearch: TextInputEditText
    private lateinit var rvStations: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_station_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as TidelyApplication
        val preferencesManager = PreferencesManager(requireContext())

        val factory = StationPickerViewModelFactory(app.tideRepository, preferencesManager)
        viewModel = ViewModelProvider(this, factory)[StationPickerViewModel::class.java]

        setupViews(view)
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupViews(view: View) {
        etSearch = view.findViewById(R.id.etSearch)
        rvStations = view.findViewById(R.id.rvStations)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = StationAdapter { station ->
            viewModel.selectStation(station)
            // Navigate back to tides
            requireActivity().supportFragmentManager.popBackStack()
        }
        rvStations.layoutManager = LinearLayoutManager(requireContext())
        rvStations.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce
                    viewModel.searchStations(s?.toString() ?: "")
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.filteredStations.observe(viewLifecycleOwner) { stations ->
            adapter.submitList(stations)
        }

        viewModel.selectedStationId.observe(viewLifecycleOwner) { selectedId ->
            adapter.setSelectedStationId(selectedId)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}
