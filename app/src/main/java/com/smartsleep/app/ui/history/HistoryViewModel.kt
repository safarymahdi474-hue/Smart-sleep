package com.smartsleep.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsleep.app.data.local.SleepSessionEntity
import com.smartsleep.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(repository: SleepRepository) : ViewModel() {
    val history: StateFlow<List<SleepSessionEntity>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
