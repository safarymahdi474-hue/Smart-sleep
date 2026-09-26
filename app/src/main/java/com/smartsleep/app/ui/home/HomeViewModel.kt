package com.smartsleep.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsleep.app.data.local.SleepSessionEntity
import com.smartsleep.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WakeWindowUi(
    val earliestHour: Int = 6,
    val earliestMinute: Int = 30,
    val latestHour: Int = 7,
    val latestMinute: Int = 30
)

class HomeViewModel(private val repository: SleepRepository) : ViewModel() {

    val activeSession: StateFlow<SleepSessionEntity?> = repository.activeSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _window = MutableStateFlow(WakeWindowUi())
    val window: StateFlow<WakeWindowUi> = _window

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setEarliest(hour: Int, minute: Int) {
        _window.value = _window.value.copy(earliestHour = hour, earliestMinute = minute)
    }

    fun setLatest(hour: Int, minute: Int) {
        _window.value = _window.value.copy(latestHour = hour, latestMinute = minute)
    }

    fun startSleep(onStarted: () -> Unit) {
        val w = _window.value
        val earliestTotal = w.earliestHour * 60 + w.earliestMinute
        val latestTotal = w.latestHour * 60 + w.latestMinute
        // A window that wraps past midnight (e.g. 23:50 - 00:30) is valid; only reject a
        // genuinely zero-length window.
        if (earliestTotal == latestTotal) {
            _error.value = "invalid"
            return
        }
        _error.value = null
        viewModelScope.launch {
            repository.startSleepSession(w.earliestHour, w.earliestMinute, w.latestHour, w.latestMinute)
            onStarted()
        }
    }

    fun cancelSleep() {
        viewModelScope.launch { repository.cancelActiveSleep() }
    }
}
