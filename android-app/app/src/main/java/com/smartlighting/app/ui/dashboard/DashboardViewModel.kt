package com.smartlighting.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.app.data.model.DashboardStats
import com.smartlighting.app.data.model.DeviceMapLocation
import com.smartlighting.app.data.model.DistrictStatus
import com.smartlighting.app.data.model.EnergyTrend
import com.smartlighting.app.data.repository.DashboardRepository
import com.smartlighting.app.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val energyTrend: EnergyTrend? = null,
    val districts: List<DistrictStatus> = emptyList(),
    val mapDevices: List<DeviceMapLocation> = emptyList(),
    val selectedArea: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val stats = repo.getStats()
            val trend = repo.getEnergyTrend()
            val districts = repo.getDistricts()
            val mapDevices = deviceRepo.getMapLocations()

            _uiState.value = _uiState.value.copy(
                stats = stats.getOrNull(),
                energyTrend = trend.getOrNull(),
                districts = districts.getOrNull() ?: emptyList(),
                mapDevices = mapDevices.getOrNull() ?: emptyList(),
                isLoading = false,
                error = if (stats.isFailure) stats.exceptionOrNull()?.message else null
            )
        }
    }

    fun selectArea(area: String) {
        _uiState.value = _uiState.value.copy(
            selectedArea = if (area == _uiState.value.selectedArea) "" else area
        )
    }
}
