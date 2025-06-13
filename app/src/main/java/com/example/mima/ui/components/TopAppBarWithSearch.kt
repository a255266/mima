package com.example.mima.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mima.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.delay



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithSearch(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onVibrateClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            onSearchQueryChange("")
            keyboardController?.hide()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isSearchActive) {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = { keyboardController?.hide() },
                active = false,
                onActiveChange = {},
                leadingIcon = {
                    IconButton(onClick = {
                        onVibrateClick()
                        onActiveChange(false)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onVibrateClick()
                            onSearchQueryChange("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                placeholder = { Text("搜索项目、用户名等...") },
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {}
        } else {
            TopAppBar(
                title = { Text("密码库", fontSize = 25.sp) },
                actions = {
                    IconButton(onClick = { onActiveChange(true) }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    }
}
