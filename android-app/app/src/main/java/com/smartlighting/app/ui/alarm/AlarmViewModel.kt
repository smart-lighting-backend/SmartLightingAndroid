package com.smartlighting.app.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.app.data.model.AlarmRecord
import com.smartlighting.app.data.model.AlarmStats
import com.smartlighting.app.data.model.AlarmTrendItem
import com.smartlighting.app.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmListState(
    val alarms: List<AlarmRecord> = emptyList(),
    val stats: AlarmStats? = null,
    val trend: List<AlarmTrendItem> = emptyList(),
    val isLoading: Boolean = false,
    val statusFilter: String? = null,
    val levelFilter: String? = null,
    val typeFilter: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val total: Long = 0,
    val pageSize: Int = 5,
    val error: String? = null
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repo: AlarmRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmListState())
    val state: StateFlow<AlarmListState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            _state.value = s.copy(isLoading = true)

            val pageResult = repo.getAlarmPage(1, s.pageSize, s.statusFilter, s.levelFilter, s.typeFilter, s.startTime, s.endTime)
            val stats = repo.getAlarmStats()
            val trend = repo.getAlarmTrend(7)

            _state.value = _state.value.copy(
                alarms = pageResult.getOrNull()?.records ?: emptyList(),
                isLoading = false,
                currentPage = pageResult.getOrNull()?.current?.toInt() ?: 1,
                totalPages = pageResult.getOrNull()?.pages?.toInt() ?: 1,
                total = pageResult.getOrNull()?.total ?: 0,
                stats = stats.getOrNull(),
                trend = trend.getOrNull() ?: emptyList(),
                error = if (pageResult.isFailure) pageResult.exceptionOrNull()?.message else null
            )
        }
    }

    fun setPageSize(size: Int) {
        if (size == _state.value.pageSize) return
        _state.value = _state.value.copy(pageSize = size)
        refresh()
    }

    fun goToPage(page: Int) {
        val total = _state.value.totalPages
        if (page < 1 || page > total) return
        viewModelScope.launch {
            val s = _state.value
            _state.value = s.copy(isLoading = true)
            repo.getAlarmPage(page, s.pageSize, s.statusFilter, s.levelFilter, s.typeFilter, s.startTime, s.endTime).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        alarms = it.records, isLoading = false,
                        currentPage = it.current.toInt(), totalPages = it.pages.toInt(), total = it.total
                    )
                },
                onFailure = { _state.value = _state.value.copy(isLoading = false) }
            )
        }
    }

    fun setStatusFilter(status: String?) {
        _state.value = _state.value.copy(statusFilter = if (status == _state.value.statusFilter) null else status)
        refresh()
    }

    fun setLevelFilter(level: String?) {
        _state.value = _state.value.copy(levelFilter = if (level == _state.value.levelFilter) null else level)
        refresh()
    }

    fun setTypeFilter(type: String?) {
        _state.value = _state.value.copy(typeFilter = if (type == _state.value.typeFilter) null else type)
        refresh()
    }

    fun setDateRange(start: String?, end: String?) {
        _state.value = _state.value.copy(startTime = start, endTime = end)
        refresh()
    }

    fun handleAlarm(id: Long) {
        viewModelScope.launch {
            repo.handleAlarm(id)
            refresh()
        }
    }
}
