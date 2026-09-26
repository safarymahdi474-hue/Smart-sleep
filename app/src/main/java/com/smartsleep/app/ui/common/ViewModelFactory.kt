package com.smartsleep.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.smartsleep.app.data.repository.SleepRepository

class GenericViewModelFactory(
    private val repository: SleepRepository,
    private val creator: (SleepRepository) -> ViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator(repository) as T
}
