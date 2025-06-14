package com.example.mima.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mima.data.DataManager
import com.example.mima.data.LoginData
import com.example.mima.util.VibrationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import androidx.paging.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@OptIn(
    kotlinx.coroutines.FlowPreview::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val vibrationHelper: VibrationHelper,
) : ViewModel() {

    // 搜索关键字（与 Paging Flow 绑定）
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    init {
        dataManager.startAutoBackupOnDatabaseChange(viewModelScope)
        viewModelScope.launch {
            dataManager.performSyncIfNeeded()
        }
        Log.d("AutoSync", "触发监控")
    }


    private val _refreshFinishedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshFinishedEvent = _refreshFinishedEvent.asSharedFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refreshAndSync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                Log.d("HomeViewModel", "开始同步")
                dataManager.performSyncIfNeeded()
                Log.d("HomeViewModel", "同步完成")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "同步失败", e)
            } finally {
                delay(500)
                _isRefreshing.value = false
                Log.d("HomeViewModel", "finally 设置 _isRefreshing = false")
                _refreshFinishedEvent.emit(Unit) // 通知监听者
            }
        }
    }



    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Paging Flow：根据搜索动态生成
    val loginDataPagingFlow: Flow<PagingData<LoginData>> =
        searchQuery
            .flatMapLatest { query ->
                dataManager.getLoginDataPagingFlow(query)
            }
            .cachedIn(viewModelScope)



    fun onSearchTextChange(query: String) {
        _searchQuery.value = query
    }

    fun onButtonClick() {
        vibrationHelper.vibrate()
    }

    fun deleteItem(data: LoginData) {
        viewModelScope.launch {
            dataManager.moveToRecycleBin(data)
        }
    }
}