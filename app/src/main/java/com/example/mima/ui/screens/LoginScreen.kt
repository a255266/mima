package com.example.mima.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.mima.R
import com.example.mima.data.DataManager
import com.example.mima.data.LoginData
import com.example.mima.ui.viewmodels.LoginViewModel
import com.example.mima.util.SoundHelper
import com.example.mima.util.throttleClick
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
    id: Long,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val soundHelper = remember(context) { SoundHelper(context) }
    // 监听id变化，加载数据（id>0则编辑，否则新建）
    LaunchedEffect(id) {
        viewModel.initialize(id)
    }




    var isActionInProgress by remember { mutableStateOf(false) }

    val txtField = OutlinedTextFieldDefaults.colors(
        // 关键：自定义禁用状态颜色
        disabledTextColor = MaterialTheme.colorScheme.onSurface, // 保持正常文本色
        disabledBorderColor = MaterialTheme.colorScheme.outline, // 边框色
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, // 标签色
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // 提示文本色
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // 前图标色
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant // 后图标色
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id > 0L) "编辑内容" else "新建内容") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isActionInProgress) {
                            isActionInProgress = true
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "取消")
                    }
                },
                actions = {
                        IconButton(onClick = {
                            throttleClick("save") {
                                viewModel.saveData(
                                    onSuccess = {
                                        coroutineScope.launch {
                                            navController.popBackStack()
                                        }
                                    },
                                    onValidationFailed = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "保存")
                        }
                }
            )
        },
        floatingActionButton = {
            val rotation by animateFloatAsState(
                targetValue = if (uiState.isEdit) 360f else 0f,
                animationSpec = tween(durationMillis = 500)
            )

            FloatingActionButton(
                onClick = {
                    viewModel.vibrationHelper.vibrate(
                        timings = longArrayOf(0, 10, 30, 10, 20, 10, 10, 10),
                        amplitudes = intArrayOf(0, 1, 0, 3, 0, 7, 0, 12)
                    )

                    if (!isActionInProgress) {
                        isActionInProgress = true
                        if (uiState.isEdit) {
                            viewModel.saveData(
                                onSuccess = {
                                    coroutineScope.launch {
                                        isActionInProgress = false
                                        viewModel.toggleEditMode()
                                    }
                                },
                                onValidationFailed = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    isActionInProgress = false // 防止验证失败时按钮卡住
                                }
                            )

                        } else {
                            // 直接切换编辑状态
                            viewModel.toggleEditMode()
                            isActionInProgress = false
                        }
                    }
                },
                modifier = Modifier
                    .padding(30.dp)
                    .size(70.dp),
            ) {
                AnimatedContent(
                    targetState = uiState.isEdit,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(250)) +
                                scaleIn(initialScale = 0.8f) +
                                slideInVertically { -it/2 }).with(
                            fadeOut(animationSpec = tween(250)) +
                                    scaleOut(targetScale = 1.2f)
                        )
                    }
                ) { editMode ->
                    Icon(
                        imageVector = if (editMode) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = if (editMode) "保存" else "编辑",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        },
        modifier = modifier,
        content = { paddingValues ->
            // 固定标签映射，注意和ViewModel标签对应，保持顺序
            val fixedFields = linkedMapOf(
                "项目名称" to uiState.projectName,
                "用户名" to uiState.username,
                "密码" to uiState.password,
                "绑定手机号" to uiState.number,
                "备注" to uiState.notes,
            )

            // 合并固定标签和自定义标签（自定义标签排在后面）
            val allFields = remember(uiState) {
                linkedMapOf<String, String>().apply {
                    putAll(fixedFields)
                    putAll(uiState.customFields)
                }
            }


            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    uiState.currentLoginData?.lastModified?.let { lastModified ->
                        Text(
                            text = "修改日期: ${
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(Date(lastModified))
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(
                    items = allFields.entries.toList(),
                    key = { it.key } // 使用标签名称作为唯一key
                ) { (fieldName, fieldValue) ->
                    // 添加淡入淡出动画状态
                    var visible by remember { mutableStateOf(true) }
                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(durationMillis = 300) // 300毫秒的淡入淡出效果
                    )

                    var deleteButtonScale by remember { mutableStateOf(1f) }
                    val deleteScale by animateFloatAsState(
                        targetValue = deleteButtonScale,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("确认删除") },
                            text = { Text("确定删除标签 '$fieldName'?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        soundHelper.playSound(R.raw.oppo)
                                        visible = false // 👈 1. 开始淡出动画
                                        showDeleteConfirm = false
                                        // 👇 2. 延迟后真正删除
                                        coroutineScope.launch {
                                            delay(300) // 与 AnimatedVisibility 的 exit 动画时长一致

                                            viewModel.removeCustomField(fieldName)
                                            visible = true
                                        }
                                    }
                                ) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }

//                    AnimatedVisibility(
//                        visible = visible,
//                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
//                        exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
//                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(alpha) // 应用透明度动画
                        ) {
                        OutlinedTextField(
                            value = fieldValue,
                            onValueChange = { newValue ->
                                viewModel.updateField(fieldName, newValue)
                            },
                            label = { Text(fieldName) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = txtField,
                            enabled = uiState.isEdit,
                            keyboardOptions = when (fieldName) {
                                "绑定手机号" -> KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                )

                                else -> KeyboardOptions.Default
                            },
                            singleLine = fieldName != "备注",
                            trailingIcon = {
                                val context = LocalContext.current
                                val clipboardManager = LocalClipboardManager.current
                                val scope = rememberCoroutineScope()

                                when {
                                    // 👉 密码标签 + 编辑模式 → 显示刷新按钮
                                    fieldName == "密码" && uiState.isEdit -> {
                                        val rotation by animateFloatAsState(
                                            targetValue = uiState.rotationDegree,
                                            animationSpec = tween(800)
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.vibrationHelper.vibrate()
                                                viewModel.generateRandomPassword()
                                            },
                                            modifier = Modifier.size(50.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "生成随机密码",
                                                modifier = Modifier.rotate(rotation).size(30.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // 👉 所有标签在非编辑状态下显示复制按钮
                                    !uiState.isEdit -> {
                                        var buttonScale by remember { mutableStateOf(1f) }
                                        val scale by animateFloatAsState(
                                            targetValue = buttonScale,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                buttonScale = 0.6f
                                                scope.launch {
                                                    viewModel.vibrationHelper.vibrate(
                                                        timings = longArrayOf(0, 2, 10, 50),
                                                        amplitudes = intArrayOf(0, 3, 0, 5)
                                                    )
                                                    delay(100)
                                                    buttonScale = 1f
                                                }
                                                clipboardManager.setText(AnnotatedString(fieldValue))
                                                Toast.makeText(
                                                    context,
                                                    "已复制",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier
                                                .size(50.dp)
                                                .scale(scale)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.copyblue),
                                                contentDescription = "复制",
                                                modifier = Modifier.size(30.dp),
                                                tint = Color(0xFF1CABF5)
                                            )
                                        }
                                    }

                                    // 👉 编辑状态下的自定义标签 → 显示删除按钮
                                    uiState.isEdit && !fixedFields.containsKey(fieldName) -> {
                                        IconButton(
                                            onClick = {
                                                deleteButtonScale = 0.7f
                                                scope.launch {
                                                    viewModel.vibrationHelper.vibrate(
                                                        timings = longArrayOf(0, 2, 10, 50),
                                                        amplitudes = intArrayOf(0, 3, 0, 5)
                                                    )
                                                    viewModel.onIconButtonClick()
                                                    delay(100)
                                                    deleteButtonScale = 1f
                                                    showDeleteConfirm = true
                                                }
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .scale(deleteScale)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除标签",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }

                        )
                    }
                }
                item {
                    if (uiState.isEdit) {
                        var showAddFieldDialog by remember { mutableStateOf(false) }
                        var newFieldName by remember { mutableStateOf("") }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showAddFieldDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("添加自定义标签")
                        }

                        if (showAddFieldDialog) {
                            AlertDialog(
                                onDismissRequest = { showAddFieldDialog = false },
                                title = { Text("添加新标签") },
                                text = {
                                    OutlinedTextField(
                                        value = newFieldName,
                                        onValueChange = { newFieldName = it },
                                        label = { Text("标签名称") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val trimmed = newFieldName.trim()
                                            val exists = allFields.keys.any { it.equals(trimmed, ignoreCase = true) }
                                            if (trimmed.isBlank()) {
                                                Toast.makeText(context, "标签名称不能为空", Toast.LENGTH_SHORT).show()
                                            } else if (exists) {
                                                Toast.makeText(context, "标签已存在", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.addCustomField(trimmed)
                                                newFieldName = ""
                                                showAddFieldDialog = false
                                            }
                                        }
                                    ) {
                                        Text("添加")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddFieldDialog = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 错误提示弹窗
            uiState.error?.let { errorMsg ->
                AlertDialog(
                    onDismissRequest = { viewModel.hideError() },
                    confirmButton = {
                        TextButton(onClick = { viewModel.hideError() }) {
                            Text("知道了")
                        }
                    },
                    title = { Text("错误") },
                    text = { Text(errorMsg) }
                )
            }
        }
    )
}
