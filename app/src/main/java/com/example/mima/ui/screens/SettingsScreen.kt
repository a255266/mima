package com.example.mima.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.mima.data.DataManager
import com.example.mima.data.SettingsData
//import com.example.mima.data.SettingsData
import com.example.mima.data.WebDavSettings
import com.example.mima.data.WebDavKeys
import com.example.mima.ui.viewmodels.LoginViewModel
import com.example.mima.ui.viewmodels.SettingsViewModel
import com.example.mima.util.throttleClick
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


fun Color.toInt(): Int {
    val alpha = (alpha * 255).toInt() and 0xFF
    val red = (red * 255).toInt()+15 and 0xFF
    val green = (green * 255).toInt()+15 and 0xFF
    val blue = (blue * 255).toInt()+25 and 0xFF
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

//设置页面
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme = isSystemInDarkTheme()

    val settings by viewModel.webDavSettings.collectAsState(
        initial = WebDavSettings("", "", "", "16", "", false, false)
    )

    // Compose 状态变量，绑定输入框
    var server by remember { mutableStateOf(settings.server) }
    var account by remember { mutableStateOf(settings.account) }
    var password by remember { mutableStateOf(settings.password) }
    var passwordLength by remember { mutableStateOf(settings.passwordLength) }
    var decryptKey by remember { mutableStateOf(settings.decryptKey) }
    var foregroundServiceEnabled by remember { mutableStateOf(settings.foregroundServiceEnabled) }
    var allowSystemSettings by remember { mutableStateOf(settings.allowSystemSettings) }

    // 外部数据变化时，同步更新 Compose 状态，避免UI与数据不同步
    LaunchedEffect(settings) {
        if (server != settings.server) server = settings.server
        if (account != settings.account) account = settings.account
        if (password != settings.password) password = settings.password
        if (passwordLength != settings.passwordLength) passwordLength = settings.passwordLength
        if (decryptKey != settings.decryptKey) decryptKey = settings.decryptKey
        if (foregroundServiceEnabled != settings.foregroundServiceEnabled) foregroundServiceEnabled = settings.foregroundServiceEnabled
        if (allowSystemSettings != settings.allowSystemSettings) allowSystemSettings = settings.allowSystemSettings
    }

    val fileSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val encrypted = viewModel.exportData(decryptKey)
                    if (encrypted != null) {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(encrypted.toByteArray())
                        }
                        Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导出失败：数据加密错误", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    try {
                        val encrypted = context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.readText() ?: ""
                        val success = viewModel.importData(decryptKey, encrypted)
                        Toast.makeText(
                            context,
                            if (success) "导入成功" else "导入失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "导入失败：${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CenterAlignedTopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = {
                    throttleClick("back") {
                        navController.popBackStack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "返回")
                }
            }
        )

        SectionTitle("云同步")
        SettingsCard(darkTheme) {

            SettingField(
                label = "WebDav服务器地址",
                value = server,
                onValueChange = {
                    server = it
                    viewModel.updateString(WebDavKeys.SERVER, it)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            SettingField(
                label = "WebDav账号",
                value = account,
                onValueChange = {
                    account = it
                    viewModel.updateString(WebDavKeys.ACCOUNT, it)
                },
            )
            SettingField(
                label = "WebDav密码",
                value = password,
                onValueChange = {
                    password = it
                    viewModel.updateString(WebDavKeys.PASSWORD, it)
                }
            )

//            SettingField("WebDav服务器地址", webDavServer) { webDavServer = it; saveAll() }
//            SettingField("WebDav账号", webDavAccount) { webDavAccount = it; saveAll() }
//            SettingField("WebDav密码", webDavPassword) { webDavPassword = it; saveAll() }

            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://help.jianguoyun.com/?p=2064"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RectangleShape
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("坚果云授权教程", fontSize = 20.sp)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        SectionTitle("本地备份")
        SettingsCard(darkTheme) {
            TextButton(
                onClick = {
                    if (decryptKey.isBlank()) {
                        Toast.makeText(context, "请先配置秘钥", Toast.LENGTH_SHORT).show()
                    } else {
                        fileSaveLauncher.launch("backup_${System.currentTimeMillis()}.json")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RectangleShape
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("导出数据", fontSize = 20.sp)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }

            TextButton(
                onClick = {
                    if (decryptKey.isBlank()) {
                        Toast.makeText(context, "请先配置秘钥", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        filePickerLauncher.launch(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RectangleShape
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("导入数据", fontSize = 20.sp)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }


//            SettingField("加密密钥", decryptKey) { decryptKey = it; saveAll() }
        }

        SectionTitle("参数")
//        SettingField(
//            label = "加密密钥",
//            value = decryptKey,
//            onValueChange = {
//                decryptKey = it
//                viewModel.updateString(WebDavKeys.DECRYPT_KEY, it)
//            }
//        )

        OutlinedTextField(
            value = decryptKey,
            onValueChange = {
                decryptKey = it
                viewModel.updateString(WebDavKeys.DECRYPT_KEY, it)
            },
            label = { Text("加密密钥") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 5.dp,
                ),
//            placeholder = { Text("默认为16位") },
//            singleLine = true,
//            keyboardOptions = KeyboardOptions.Default.copy(
//                keyboardType = KeyboardType.Number
//            )
        )

//        SectionTitle("配置密码生成器")
        OutlinedTextField(
            value = passwordLength,
            onValueChange = {
                passwordLength = it
                viewModel.updateString(WebDavKeys.PASSWORD_LENGTH, it)
            },
            label = { Text("密码生成器") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 5.dp,
                ),
            placeholder = { Text("默认生成16位") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            )
        )


        SectionTitle("权限")
//        var checked by remember { mutableStateOf(false) }
//        var checked1 by remember { mutableStateOf(false) }
        var checked2 by remember { mutableStateOf(false) }
        SettingsCard(darkTheme) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp)
                    .padding(horizontal = 14.dp), // 左右各 16.dp 边距
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "前台服务",
                    fontSize = 20.sp ,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = foregroundServiceEnabled,
                    onCheckedChange = {
                        foregroundServiceEnabled = it
                        viewModel.updateBoolean(WebDavKeys.FOREGROUND_SERVICE, it)
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
                    .padding(horizontal = 14.dp), // 左右各 16.dp 边距
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "修改系统设置",
                    fontSize = 20.sp ,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = allowSystemSettings,
                    onCheckedChange = {
                        allowSystemSettings = it
                        viewModel.updateBoolean(WebDavKeys.ALLOW_SYSTEM_SETTINGS, it)
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom  = 7.dp)
                    .padding(horizontal = 14.dp), // 左右各 16.dp 边距
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "预留一个",
                    fontSize = 20.sp ,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = checked2,
                    onCheckedChange = { checked2 = it }
                )
            }

        }

        //webdav上传下载测试
//        val result by viewModel.result.collectAsState()
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Button(onClick = { viewModel.uploadTestFileWithDataManager("test/backup.json", null) }) {
//                Text("上传测试文件")
//            }
//
//            Button(onClick = { viewModel.downloadTestFileWithDataManager("test/backup.json", null) }) {
//                Text("下载测试文件")
//            }
//
//
//
//            Button(onClick = { viewModel.deleteTestFile() }) {
//                Text("删除测试文件")
//            }
//
//            Text("结果：$result")
//        }

        Spacer(modifier = Modifier.height(100.dp))


    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, modifier = Modifier.padding(start = 26.dp, top = 6.dp), color = Color.Gray)
}

@Composable
fun SettingsCard(darkTheme: Boolean, content: @Composable ColumnScope.() -> Unit) {
//    val bgColor = if (darkTheme) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.surfaceContainerLow
    val hexColor = MaterialTheme.colorScheme.surfaceContainerLow.toInt()
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (darkTheme) Color(hexColor) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column() { content() }
    }
}

@Composable
fun SettingField(label: String, value: String, onValueChange: (String) -> Unit,modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}