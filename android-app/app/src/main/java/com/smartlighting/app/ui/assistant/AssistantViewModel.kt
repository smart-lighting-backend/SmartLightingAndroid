package com.smartlighting.app.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlighting.app.data.model.Device
import com.smartlighting.app.data.repository.DeviceRepository
import com.smartlighting.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val content: String,
    val isUser: Boolean
)

data class AssistantState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage("你好！我是智慧路灯智能助手，有什么可以帮助你的？", false)
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val devices: List<Device> = emptyList(),
    val selectedDeviceId: String = "",
    val showDevicePicker: Boolean = false
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repo: EventRepository,
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantState())
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    init { loadDevices() }

    private fun loadDevices() {
        viewModelScope.launch {
            deviceRepo.getDevicePage(1, 200, null).fold(
                onSuccess = { _state.value = _state.value.copy(devices = it.records) },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun onInputChange(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun selectDevice(deviceId: String) {
        _state.value = _state.value.copy(selectedDeviceId = deviceId)
    }

    fun showDevicePicker(show: Boolean) {
        _state.value = _state.value.copy(showDevicePicker = show)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isLoading) return

        val msgs = _state.value.messages + ChatMessage(text, true)
        _state.value = _state.value.copy(messages = msgs, inputText = "", isLoading = true)

        viewModelScope.launch {
            repo.chat(text).fold(
                onSuccess = { resp ->
                    val reply = resp.content.ifBlank { "已收到您的消息" }
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + ChatMessage(reply, false),
                        isLoading = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + ChatMessage("请求失败，请重试", false),
                        isLoading = false
                    )
                }
            )
        }
    }

    fun runDiagnose() {
        val deviceId = _state.value.selectedDeviceId
        if (deviceId.isBlank() || _state.value.isLoading) return

        val msgs = _state.value.messages + ChatMessage("诊断设备: $deviceId", true)
        _state.value = _state.value.copy(messages = msgs, isLoading = true)

        viewModelScope.launch {
            repo.diagnose(deviceId, null).fold(
                onSuccess = { resp ->
                    val reply = resp.content.ifBlank { "诊断完成，设备状态正常。" }
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + ChatMessage(reply, false),
                        isLoading = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + ChatMessage("诊断请求失败，请检查服务状态", false),
                        isLoading = false
                    )
                }
            )
        }
    }
}
