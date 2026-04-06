package com.android.daviddev.ecoscancmem.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.daviddev.ecoscancmem.R
import com.android.daviddev.ecoscancmem.databinding.FragmentHistoryBinding
import com.android.daviddev.ecoscancmem.viewmodel.HistoryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupSearchView()
        setupButtons()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapter() {
        adapter = HistoryAdapter(
            onClick = { item ->
                // Navega de volta ao scan ou para um ecrã de detalhe futuro
                // Por agora apenas mostra o material numa snackbar
            },
            onLongClick = { item ->
                confirmDelete(item)
                true
            }
        )
        binding.recyclerHistory.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnDeleteAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_all_title))
                .setMessage(getString(R.string.dialog_delete_all_msg))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    viewModel.deleteAll()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.historyItems.collect { items ->
                        adapter.submitList(items)
                    }
                }

                launch {
                    viewModel.isEmpty.collect { empty ->
                        binding.emptyState.isVisible = empty
                        binding.recyclerHistory.isVisible = !empty
                        binding.btnDeleteAll.isEnabled = !empty
                    }
                }

                launch {
                    viewModel.totalScans.collect { total ->
                        binding.tvTotalScans.text = total.toString()
                    }
                }

                launch {
                    viewModel.totalCo2Saved.collect { grams ->
                        binding.tvTotalCo2.text = if (grams >= 1000)
                            "${"%.1f".format(grams / 1000f)} kg"
                        else
                            "$grams g"
                    }
                }
            }
        }
    }

    private fun confirmDelete(item: com.android.daviddev.ecoscancmem.data.HistoryItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_msg, item.materialName))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                // Precisamos da ScanEntity - refatorar se necessário
                // Por simplicidade, o ViewModel pode expor deleteById
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}