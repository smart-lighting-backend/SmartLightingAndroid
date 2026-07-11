package com.smartlighting.app.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.app.data.model.VisionEvent
import com.smartlighting.app.data.model.VoiceEvent
import com.smartlighting.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventUiState(
    val visionEvents: List<VisionEvent> = emptyList(),
    val voiceEvents: List<VoiceEvent> = emptyList(),
    val isLoading: Boolean = false,
    val activeTab: Int = 0,
    // Filters
    val deviceIdFilter: String = "",
    val visionTypeFilter: String? = null,
    val voiceTypeFilter: String? = null,
    val voiceSourceFilter: String? = null,
    // Pagination
    val visionPage: Int = 1,
    val visionTotalPages: Int = 1,
    val visionTotal: Long = 0,
    val voicePage: Int = 1,
    val voiceTotalPages: Int = 1,
    val voiceTotal: Long = 0,
    val pageSize: Int = 5
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repo: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventUiState())
    val state: StateFlow<EventUiState> = _state.asStateFlow()

    init { loadVision() }

    fun setPageSize(size: Int) {
        if (size == _state.value.pageSize) return
        _state.value = _state.value.copy(pageSize = size)
        if (_state.value.activeTab == 0) loadVision() else loadVoice()
    }

    fun loadVision() {
        viewModelScope.launch {
            val ps = _state.value.pageSize
            _state.value = _state.value.copy(isLoading = true, activeTab = 0)
            repo.getVisionEvents(1, ps, _state.value.deviceIdFilter.ifBlank { null }, _state.value.visionTypeFilter).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        visionEvents = it.records, isLoading = false,
                        visionPage = it.current.toInt(), visionTotalPages = it.pages.toInt(), visionTotal = it.total
                    )
                },
                onFailure = { _state.value = _state.value.copy(isLoading = false) }
            )
        }
    }

    fun loadVoice() {
        viewModelScope.launch {
            val ps = _state.value.pageSize
            _state.value = _state.value.copy(isLoading = true, activeTab = 1)
            repo.getVoiceEvents(1, ps, _state.value.deviceIdFilter.ifBlank { null }, _state.value.voiceTypeFilter, _state.value.voiceSourceFilter).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        voiceEvents = it.records, isLoading = false,
                        voicePage = it.current.toInt(), voiceTotalPages = it.pages.toInt(), voiceTotal = it.total
                    )
                },
                onFailure = { _state.value = _state.value.copy(isLoading = false) }
            )
        }
    }

    fun goToVisionPage(page: Int) {
        val total = _state.value.visionTotalPages
        if (page < 1 || page > total) return
        viewModelScope.launch {
            val ps = _state.value.pageSize
            _state.value = _state.value.copy(isLoading = true)
            repo.getVisionEvents(page, ps, _state.value.deviceIdFilter.ifBlank { null }, _state.value.visionTypeFilter).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        visionEvents = it.records, isLoading = false,
                        visionPage = it.current.toInt(), visionTotalPages = it.pages.toInt(), visionTotal = it.total
                    )
                },
                onFailure = { _state.value = _state.value.copy(isLoading = false) }
            )
        }
    }

    fun goToVoicePage(page: Int) {
        val total = _state.value.voiceTotalPages
        if (page < 1 || page > total) return
        viewModelScope.launch {
            val ps = _state.value.pageSize
            _state.value = _state.value.copy(isLoading = true)
            repo.getVoiceEvents(page, ps, _state.value.deviceIdFilter.ifBlank { null }, _state.value.voiceTypeFilter, _state.value.voiceSourceFilter).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        voiceEvents = it.records, isLoading = false,
                        voicePage = it.current.toInt(), voiceTotalPages = it.pages.toInt(), voiceTotal = it.total
                    )
                },
                onFailure = { _state.value = _state.value.copy(isLoading = false) }
            )
        }
    }

    fun setDeviceIdFilter(deviceId: String) {
        _state.value = _state.value.copy(deviceIdFilter = deviceId)
        if (_state.value.activeTab == 0) loadVision() else loadVoice()
    }

    fun setVisionTypeFilter(type: String?) {
        _state.value = _state.value.copy(visionTypeFilter = if (type == _state.value.visionTypeFilter) null else type)
        loadVision()
    }

    fun setVoiceTypeFilter(type: String?) {
        _state.value = _state.value.copy(voiceTypeFilter = if (type == _state.value.voiceTypeFilter) null else type)
        loadVoice()
    }

    fun setVoiceSourceFilter(source: String?) {
        _state.value = _state.value.copy(voiceSourceFilter = if (source == _state.value.voiceSourceFilter) null else source)
        loadVoice()
    }

    fun silentRefresh() {
        if (_state.value.activeTab == 0) loadVision() else loadVoice()
    }
}
