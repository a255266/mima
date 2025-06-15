package com.example.mima.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mima.R
import com.example.mima.data.LoginData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.example.mima.util.SoundHelper
import com.example.mima.util.VibrationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlin.reflect.KProperty0
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.foundation.gestures.ScrollableDefaults
import kotlin.math.abs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Velocity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.LocalOverscrollFactory
import com.example.mima.data.SyncStatusBus
import com.example.mima.data.SyncStatusType


// 定义性能监控工具
//class RecompositionTracker(private val tag: String) {
//    private var startTime = 0L
//
//    fun start() {
//        startTime = System.nanoTime()
//    }
//
//    fun end() {
//        val duration = (System.nanoTime() - startTime) / 1_000_000
//        if (duration > 16) { // 超过16ms（60fps一帧时间）才警告
//            Log.w("Perf", "$tag 重组耗时: ${duration}ms")
//        }
//    }
//}

// 创建Context
//val LocalRecompositionTracker = staticCompositionLocalOf<RecompositionTracker?> { null }

@OptIn(ExperimentalFoundationApi::class)


@Composable
fun ScrollableList(
    state: LazyListState,
    loginItems: LazyPagingItems<LoginData>,
    navController: NavController,
    onDelete: (LoginData) -> Unit,
    onVibrateClick: () -> Unit
) {
//    val tracker = LocalRecompositionTracker.current
//    tracker?.start()


    val iconImgkey = painterResource(R.drawable.imgkey)
    val scope = rememberCoroutineScope()

    var itemToDelete by remember { mutableStateOf<LoginData?>(null) }
    var deletingItemId by remember { mutableStateOf<Long?>(null) }
    if (itemToDelete != null) {
        DeleteConfirmationDialog(
            item = itemToDelete!!,
            onDismiss = { itemToDelete = null },
            onConfirm = { item ->
                deletingItemId = item.id
                scope.launch {
                    delay(1000)
                    onDelete(item)
                    deletingItemId = null
                }
                itemToDelete = null
            }
        )
    }


    // 🔍 提前获取分页状态
    val isAppending = loginItems.loadState.append is LoadState.Loading
    val appendError = loginItems.loadState.append is LoadState.Error
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Column {
            SyncStatusBanner()
            LazyColumn(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),

                contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
            ) {
                items(
                    count = loginItems.itemCount,
                    key = { index -> loginItems[index]?.id ?: index }
                ) { index ->
                    val data = loginItems[index]
                    if (data != null) {
                        ListItemWithAnimation(
                            data = data,
                            index = index,
                            totalItems = loginItems.itemCount,
                            iconImgkey = iconImgkey,
                            navController = navController,
                            isDeleting = (deletingItemId == data.id),
                            onLongClick = {
                                onVibrateClick()
                                itemToDelete = data
                            }
                        )
                    }
                }

                // 分页加载指示器
                if (isAppending) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .heightIn(min = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (appendError) {
                    item {
                        Text(
                            text = "加载失败，请重试",
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}




@Composable
private fun DeleteConfirmationDialog(
    item: LoginData,
    onDismiss: () -> Unit,
    onConfirm: (LoginData) -> Unit
) {
    val context = LocalContext.current
    val soundHelper = remember(context) { SoundHelper(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除「${item.projectname}」吗？") },
        confirmButton = {
            TextButton(onClick = {
                soundHelper.playSound(R.raw.oppo)
                onConfirm(item) }) {
                Text("删除", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ListItemWithAnimation(
    data: LoginData,
    index: Int,
    totalItems: Int,
    iconImgkey: Painter,
    navController: NavController,
    isDeleting: Boolean,
    onLongClick: () -> Unit
) {

    val startTime = remember { System.nanoTime() }

    DisposableEffect(Unit) {
        onDispose {
            val duration = (System.nanoTime() - startTime) / 1_000_000
            if (duration > 16) {
                Log.w("Perf", "Compose时间过长: id=${data.id}, duration=${duration}ms")
            }
        }
    }

    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            delay(800)
            isVisible = false
        }
    }

    val shouldShow = remember(isVisible, isDeleting) {
        isVisible && !isDeleting
    }

    val shape = when {
        totalItems == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        index == totalItems - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(0.dp)
    }
    val coroutineScope = rememberCoroutineScope()

    val modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
        .padding(horizontal = 16.dp)
        .clip(shape) // 先裁剪
        .then(
            if (index != totalItems - 1) Modifier.drawBehind {
                val startX = 50.dp.toPx()
                drawLine(
                    color = Color.Gray.copy(0.5f),
                    start = Offset(startX, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
            } else Modifier
        )
        .combinedClickable(
            onClick = {
                coroutineScope.launch {
                    delay(100L)
                    navController.navigate("login/${data.id}")
                }
            },
            onLongClick = onLongClick
        )


    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ListItem(
            modifier = modifier,
            headlineContent = { Text(data.projectname) },
            supportingContent = { Text(data.username) },
            leadingContent = {
                Icon(
                    painter = iconImgkey,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Gray.copy(alpha = 0.1f)
            )
        )
    }
}


@Composable
fun SyncStatusBanner() {
    Log.d("Recomposition", "SyncStatusBanner")
    val syncStatus by SyncStatusBus.statusFlow.collectAsState()

    // 用一个状态存储当前显示的消息，保证动画期间内容不变
    var currentStatus by remember { mutableStateOf<Pair<String, SyncStatusType>?>(null) }

    // 控制动画显隐
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(syncStatus) {
        if (syncStatus != null) {
            currentStatus = syncStatus
            visible = true
            // 只有非 Info 状态才自动隐藏
            if (syncStatus!!.second != SyncStatusType.Info) {
                delay(5000)
                visible = false
                delay(1000) // 给动画时间
                SyncStatusBus.clear()
                currentStatus = null
            }
        } else {
            if (!visible) {
                currentStatus = null
            }
        }
    }


    AnimatedVisibility(
        visible = visible,
        enter =fadeIn(animationSpec = tween(durationMillis = 1000)),
        exit =fadeOut(animationSpec = tween(durationMillis = 1000))
    ) {
        syncStatus?.let { (message, type) ->
            val textColor = when (type) {
                SyncStatusType.Info -> Color(0xFF2196F3)      // 蓝色
                SyncStatusType.Success -> Color(0xFF4CAF50)   // 绿色
                SyncStatusType.Error -> Color(0xFFF44336)     // 红色
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f) // 让文字占满剩余空间，避免挤压
                )
                if (type == SyncStatusType.Info) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        color = textColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

