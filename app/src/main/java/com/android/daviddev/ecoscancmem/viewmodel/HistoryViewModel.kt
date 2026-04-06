package com.android.daviddev.ecoscancmem.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.daviddev.ecoscancmem.data.HistoryItem
import com.android.daviddev.ecoscancmem.data.ScanRepository
import com.android.daviddev.ecoscancmem.data.db.EcoScanDatabase
import com.android.daviddev.ecoscancmem.data.db.ScanEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScanRepository(
        EcoScanDatabase.getInstance(application)
    )

    // Filtro de pesquisa
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Lista filtrada (combina DB + query)
    val historyItems: StateFlow<List<HistoryItem>> =
        combine(repository.allScans, _searchQuery) { scans, query ->
            scans
                .map { with(repository) { it.toDisplayModel() } }
                .filter { item ->
                    query.isBlank() ||
                            item.materialName.contains(query, ignoreCase = true) ||
                            item.recycleCode.contains(query, ignoreCase = true)
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Estatísticas para o header
    val totalScans: StateFlow<Int> = repository.totalScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalCo2Saved: StateFlow<Int> = repository.totalCo2Saved
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isEmpty: StateFlow<Boolean> = historyItems
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Ações
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun delete(entity: ScanEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }
}