package com.example.mima.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SyncStatusType {
    Info, Success, Error
}

object SyncStatusBus {
    private val _statusFlow = MutableStateFlow<Pair<String, SyncStatusType>?>(null)
    val statusFlow: StateFlow<Pair<String, SyncStatusType>?> = _statusFlow

    fun update(message: String, type: SyncStatusType) {
        _statusFlow.value = message to type
    }

    fun clear() {
        _statusFlow.value = null
    }
}
