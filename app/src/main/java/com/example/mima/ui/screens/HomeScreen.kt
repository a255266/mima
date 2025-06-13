package com.example.mima.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.mima.ui.components.ScrollableList
import com.example.mima.ui.components.TopAppBarWithSearch
import com.example.mima.ui.viewmodels.HomeViewModel
import androidx.activity.compose.BackHandler
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun rememberFabVisibleState(
    listState: LazyListState,
    thresholdPx: Int = 30,
    ): State<Boolean> {
    val density = LocalDensity.current
    val threshold = with(density) { thresholdPx.dp.toPx() }

    var previousScrollOffset by remember { mutableStateOf(0f) }
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset + listState.firstVisibleItemIndex * 1000 }
            .collect { currentOffset ->
                val delta = currentOffset - previousScrollOffset
                previousScrollOffset = currentOffset.toFloat()

                if (delta > threshold && isVisible) {
                    isVisible = false // 向下滑动超过阈值 → 隐藏
                } else if (delta < -threshold && !isVisible) {
                    isVisible = true // 向上滑动超过阈值 → 显示
                }
            }
    }

    return rememberUpdatedState(isVisible)
}


//首页
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {


    val loginItems = viewModel.loginDataPagingFlow.collectAsLazyPagingItems()
//    var isSearchActive by remember { mutableStateOf(false) }
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val listState = rememberLazyListState()

    BackHandler(enabled = isSearchActive) {
        viewModel.setSearchActive(false)
        viewModel.setSearchQuery("")
    }



    val isExpand by rememberFabVisibleState(listState)


    Scaffold(
        topBar = {
            TopAppBarWithSearch(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchTextChange,
                isSearchActive = isSearchActive,
                onActiveChange = {
                    viewModel.onButtonClick()
                    viewModel.setSearchActive(it) },
                onSettingsClick = {
                    viewModel.onButtonClick()
                    navController.navigate("settings") },
                onVibrateClick = viewModel::onButtonClick
            )
        },
        floatingActionButton = {
            AddButton(
                isExpand = isExpand,
                onClick = {
                    viewModel.onButtonClick()
                    navController.navigate("login/0") // 跳转到新增编辑页
                },
                modifier = Modifier
                    .padding(30.dp)
                    .size(70.dp),
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ScrollableList(
                state = listState,
                loginItems = loginItems,
                navController = navController,
                onDelete = viewModel::deleteItem,
                onVibrateClick = viewModel::onButtonClick
            )
        }
    }
}


@Composable
fun AddButton(isExpand: Boolean, modifier: Modifier = Modifier,onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isExpand,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = scaleIn(initialScale = 0.1f),
            exit = scaleOut(targetScale = 0.1f)
        ) {
            FloatingActionButton(
                onClick = onClick,
                modifier = modifier,
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Filled.Add, "新增内容"
                )
            }
        }
    }
}
