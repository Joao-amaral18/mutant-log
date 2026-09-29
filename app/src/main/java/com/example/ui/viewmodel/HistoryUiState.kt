package com.example.ui.viewmodel

import com.example.data.model.HistoryWorkout

data class HistoryUiState(val workouts: List<HistoryWorkout> = emptyList(), val loading: Boolean = true, val error: String? = null)
