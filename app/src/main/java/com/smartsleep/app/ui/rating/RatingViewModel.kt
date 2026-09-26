package com.smartsleep.app.ui.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsleep.app.data.repository.SleepRepository
import com.smartsleep.app.domain.model.WakeRating
import kotlinx.coroutines.launch

class RatingViewModel(private val repository: SleepRepository) : ViewModel() {

    fun submit(sessionId: Long, rating: WakeRating, wokeBeforeAlarm: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.recordRating(sessionId, rating, wokeBeforeAlarm)
            onDone()
        }
    }

    fun skip(sessionId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.finishWithoutRating(sessionId)
            onDone()
        }
    }
}
