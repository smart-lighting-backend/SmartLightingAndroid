package com.smartlighting.app.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.app.data.model.*
import com.smartlighting.app.data.repository.DashboardRepository
import com.smartlighting.app.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListState(
    val devices: List<Device> = emptyList(),
    val districts: List<DistrictStatus> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val keyword: String = "",
    val areaFilter: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val total: Long = 0,
    val error: String? = null
) {
    val hasMore: Boolean get() = currentPage < totalPages
}

data class DeviceDetailState(
    val device: Device? = null,
    val telemetry: LatestTelemetry? = null,
    val health: DeviceHealth? = null,
    val perception: Map<String, Any?>? = null,
    val controlHistory: List<ControlHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class MapPickerState(
    val mapDevices: List<DeviceMapLocation> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val repo: DeviceRepository,
    private val dashboardRepo: DashboardRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(DeviceListState())
    val listState: StateFlow<DeviceListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(DeviceDetailState())
    val detailState: StateFlow<DeviceDetailState> = _detailState.asStateFlow()

    private val _mapPickerState = MutableStateFlow(MapPickerState())
    val mapPickerState: StateFlow<MapPickerState> = _mapPickerState.asStateFlow()

    private companion object {
        const val PAGE_SIZE = 20
    }

    init {
        loadDistricts()
    }

    fun loadDevices(keyword: String = "") {
        viewModelScope.launch {
            val area = _listState.value.areaFilter
            _listState.value = _listState.value.copy(isLoading = true, keyword = keyword, currentPage = 0, totalPages = 0)
            repo.getDevicePage(1, PAGE_SIZE, keyword.ifBlank { null }, area).fold(
                onSuccess = {
                    _listState.value = _listState.value.copy(
                        devices = it.records,
                        isLoading = false,
                        currentPage = it.current.toInt(),
                        totalPages = it.pages.toInt(),
                        total = it.total
                    )
                },
                onFailure = { _listState.value = _listState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    fun setAreaFilter(area: String?) {
        val current = _listState.value.areaFilter
        _listState.value = _listState.value.copy(areaFilter = if (area == current) null else area)
        loadDevices(_listState.value.keyword)
    }

    fun refresh() {
        loadDevices(_listState.value.keyword)
        loadDistricts()
    }

    fun loadMore() {
        val state = _listState.value
        if (state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1
            repo.getDevicePage(nextPage, PAGE_SIZE, state.keyword.ifBlank { null }, state.areaFilter).fold(
                onSuccess = {
                    _listState.value = _listState.value.copy(
                        devices = state.devices + it.records,
                        isLoadingMore = false,
                        currentPage = it.current.toInt(),
                        totalPages = it.pages.toInt(),
                        total = it.total
                    )
                },
                onFailure = { _listState.value = _listState.value.copy(isLoadingMore = false) }
            )
        }
    }

    private fun loadDistricts() {
        viewModelScope.launch {
            dashboardRepo.getDistricts().fold(
                onSuccess = { _listState.value = _listState.value.copy(districts = it) },
                onFailure = { /* silently ignore */ }
            )
        }
    }

    fun loadDeviceDetail(deviceId: String, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _detailState.value = _detailState.value.copy(isLoading = true)
            val device = repo.getDeviceDetail(deviceId)
            val telemetry = repo.getLatestTelemetry(deviceId)
            val health = repo.getDeviceHealth(deviceId)
            val perception = repo.getDevicePerception(deviceId)
            val history = repo.getControlHistory(deviceId)
            _detailState.value = DeviceDetailState(
                device = device.getOrNull(),
                telemetry = telemetry.getOrNull(),
                health = health.getOrNull(),
                perception = perception.getOrNull(),
                controlHistory = history.getOrNull()?.records ?: emptyList(),
                isLoading = false
            )
        }
    }

    fun loadMapLocations() {
        viewModelScope.launch {
            _mapPickerState.value = _mapPickerState.value.copy(isLoading = true)
            val locations = repo.getMapLocations()
            _mapPickerState.value = _mapPickerState.value.copy(
                mapDevices = locations.getOrNull() ?: emptyList(),
                isLoading = false
            )
        }
    }

    fun createDevice(
        deviceId: String,
        name: String,
        area: String,
        lng: String = "",
        lat: String = "",
        factorySerial: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val location = if (lng.isNotBlank() && lat.isNotBlank()) "$lng,$lat" else null
            val req = CreateDeviceRequest(
                deviceId = deviceId,
                name = name.ifBlank { null },
                area = area.ifBlank { null },
                location = location,
                factorySerial = factorySerial.ifBlank { null }
            )
            repo.createDevice(req).fold(
                onSuccess = { loadDevices(); onResult(true, null) },
                onFailure = { e -> onResult(false, e.message) }
            )
        }
    }

    fun updateDevice(
        deviceId: String,
        name: String,
        area: String,
        lng: String,
        lat: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>()
            if (name.isNotBlank()) updates["name"] = name
            if (area.isNotBlank()) updates["area"] = area
            if (lng.isNotBlank() && lat.isNotBlank()) updates["location"] = "$lng,$lat"
            repo.updateDevice(deviceId, updates).fold(
                onSuccess = { loadDevices(); onResult(true) },
                onFailure = { onResult(false) }
            )
        }
    }

    fun controlDevice(deviceId: String, action: String, brightness: Int? = null) {
        viewModelScope.launch {
            // Optimistic update: immediately reflect the new state in UI
            val current = _detailState.value.device
            if (current != null && action != "RESTART") {
                val newLatestData = buildString {
                    append("{")
                    append("\"action\":\"$action\"")
                    if (brightness != null) append(",\"brightness\":$brightness")
                    else if (action == "ON") append(",\"brightness\":100")
                    else if (action == "OFF") append(",\"brightness\":0")
                    append(",\"controlSource\":\"MANUAL\"")
                    append("}")
                }
                _detailState.value = _detailState.value.copy(
                    device = current.copy(latestData = newLatestData)
                )
            }
            // Send command to backend, then silently refresh
            repo.controlDevice(deviceId, action, brightness)
            loadDeviceDetail(deviceId, silent = true)
        }
    }

    fun unlockDevice(deviceId: String) {
        viewModelScope.launch {
            repo.unlockDevice(deviceId)
            loadDeviceDetail(deviceId, silent = true)
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repo.deleteDevice(deviceId)
            loadDevices()
        }
    }

    // ── Batch Operations ──

    fun batchCreateDevices(
        rows: List<ImportRow>,
        onResult: (Boolean, BatchCreateResult?, String?) -> Unit
    ) {
        viewModelScope.launch {
            val requests = rows.filter { it.valid }.map { row ->
                CreateDeviceRequest(
                    deviceId = row.deviceId,
                    name = row.name.ifBlank { null },
                    area = row.area.ifBlank { null },
                    location = row.location,
                    factorySerial = row.factorySerial.ifBlank { null }
                )
            }
            repo.batchCreateDevices(requests).fold(
                onSuccess = { loadDevices(); onResult(true, it, null) },
                onFailure = { e -> onResult(false, null, e.message) }
            )
        }
    }

    fun batchControl(ids: List<Long>, action: String, onResult: (Boolean, BatchOperationResult?) -> Unit) {
        viewModelScope.launch {
            val result = when (action) {
                "ON" -> repo.batchTurnOn(ids)
                "OFF" -> repo.batchTurnOff(ids)
                "ENABLE" -> repo.batchEnable(ids)
                "DISABLE" -> repo.batchDisable(ids)
                else -> return@launch
            }
            result.fold(
                onSuccess = { loadDevices(); onResult(true, it) },
                onFailure = { onResult(false, null) }
            )
        }
    }
}
