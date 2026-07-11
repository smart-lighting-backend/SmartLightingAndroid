package com.smartlighting.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.app.data.model.*
import com.smartlighting.app.data.repository.AlarmRepository
import com.smartlighting.app.data.repository.DashboardRepository
import com.smartlighting.app.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsState(
    val energyTrend: EnergyTrend? = null,
    val healthSummary: HealthSummary? = null,
    val alarmStats: AlarmStats? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val dashboardRepo: DashboardRepository,
    private val deviceRepo: DeviceRepository,
    private val alarmRepo: AlarmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val energy = dashboardRepo.getEnergyTrend()
            val health = deviceRepo.getHealthSummary()
            val alarmStats = alarmRepo.getAlarmStats()
            _state.value = AnalyticsState(
                energyTrend = energy.getOrNull(),
                healthSummary = health.getOrNull(),
                alarmStats = alarmStats.getOrNull(),
                isLoading = false
            )
        }
    }
}
