package com.example.mima.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mima.data.DataManager
import com.example.mima.data.LoginData
import com.example.mima.data.SettingsData
import com.example.mima.util.VibrationHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val dataManager: DataManager,
    val vibrationHelper: VibrationHelper,
    private val settingsData: SettingsData
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

//    var isEditMode by mutableStateOf(false)
//        private set

    var loginData by mutableStateOf<LoginData?>(null)
        private set

    val passwordLengthIntFlow: Flow<Int> = settingsData.passwordLength
        .map { it.toIntOrNull()?.coerceAtLeast(1) ?: 16 }

    private var passwordLength: Int = 16  // 默认值

    init {
        viewModelScope.launch {
            passwordLengthIntFlow.collect {
                passwordLength = it
            }
        }
    }

    // 固定字段更新方法
    fun updateField(field: String, value: String) {
        _uiState.update {
            when (field) {
                "项目名称" -> it.copy(projectName = value)
                "用户名" -> it.copy(username = value)
                "密码" -> it.copy(password = value)
                "绑定手机号" -> it.copy(number = value)
                "备注" -> it.copy(notes = value)
                else -> it.copy(customFields = it.customFields + (field to value))
            }
        }
    }


    fun toggleEditMode() {
        _uiState.update { it.copy(isEdit = !it.isEdit) }
    }

    fun setEditMode(isEdit: Boolean) {
        _uiState.update { it.copy(isEdit = isEdit) }
    }

    fun onIconButtonClick() {
        vibrationHelper.vibrate()
    }

    fun generateRandomPassword() {
        val newPassword = generatePassword()
        _uiState.update {
            it.copy(
                password = newPassword,
                rotationDegree = it.rotationDegree + 360f
            )
        }
    }

    private fun generatePassword(): String {
        val upperCase = ('A'..'Z').toList()
        val lowerCase = ('a'..'z').toList()
        val numbers = ('0'..'9').toList()
        val specialChars = listOf('!','@','#','$','%','^','&','*')

        //随机生成密码
        val allChars = upperCase + lowerCase + numbers + specialChars





        return (1..passwordLength) //
            .map { Random.nextInt(allChars.size) }
            .map(allChars::get)
            .joinToString("")
    }

    fun addCustomField(name: String) {
        _uiState.update { state ->
            state.copy(customFields = state.customFields + (name to ""))
        }
    }

    fun removeCustomField(name: String) {
        _uiState.update { state ->
            state.copy(customFields = state.customFields - name)
        }
    }

    fun hideError() {
        _uiState.update { it.copy(error = null) }
    }

    fun initialize(id: Long) {
        if (id > 0L) {
            loadData(id) // 会默认进入阅读模式 isEdit = false
        } else {
            // 新建模式，字段为空，默认编辑模式
            _uiState.update {
                LoginUiState(isEdit = true)
            }
        }
    }


    fun loadData(projectId: Long) {
        viewModelScope.launch {
            try {
                dataManager.getMainDataById(projectId)?.let { data ->
                    _uiState.update {
                        it.copy(
                            projectName = data.projectname,
                            username = data.username,
                            password = data.password,
                            number = data.number,
                            notes = data.notes,
                            currentLoginData = data,
                            customFields = parseCustomFields(data.customFieldJson),
                            isEdit = false,
                            rotationDegree = 0f,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "加载失败: ${e.message}") }
            }
        }
    }

    fun saveData(onSuccess: () -> Unit, onValidationFailed: (String) -> Unit) {
        val projectName = _uiState.value.projectName.trim()
        if (projectName.isEmpty()) {
            onValidationFailed("项目名不能为空")
            return
        }

//        val start = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                val operationType = if (_uiState.value.currentLoginData == null) {
                    DataManager.OperationType.CREATE
                } else {
                    DataManager.OperationType.UPDATE
                }

                val data = LoginData(
                    id = _uiState.value.currentLoginData?.id ?: 0L,
                    projectname = projectName,
                    username = _uiState.value.username,
                    password = _uiState.value.password,
                    number = _uiState.value.number,
                    notes = _uiState.value.notes,
                    customFieldJson = Gson().toJson(_uiState.value.customFields)
                )

                val savedId = dataManager.saveData( // 返回 Long?
                    data = data,
                    dbType = DataManager.DatabaseType.MAIN,
                    operationType = operationType
                )

                if (savedId != null && savedId > 0) {
                    val updated = dataManager.getMainDataById(savedId)
                    _uiState.update {
                        it.copy(currentLoginData = updated)
                    }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(error = "保存失败") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存错误: ${e.message}") }
            }
        }
    }



    private fun parseCustomFields(json: String?): Map<String, String> {
        return try {
            json?.let {
                Gson().fromJson(it, object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

data class LoginUiState(
    val projectName: String = "",
    val username: String = "",
    val password: String = "",
    val number: String = "",
    val notes: String = "",
    val customFields: Map<String, String> = emptyMap(),
    val isEdit: Boolean = false,
    val rotationDegree: Float = 0f,
    val currentLoginData: LoginData? = null,
    val error: String? = null
)
