package com.example.mima.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mima.ui.screens.LoginScreen
import com.example.mima.ui.screens.SettingsScreen
import androidx.compose.material3.Text
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.mima.ui.screens.HomeScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(navController: NavHostController) {
    AnimatedNavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { enterTransitionSpec(initialState, targetState) },
        exitTransition = {
            exitTransitionSpec(initialState, targetState) ?: ExitTransition.None
        },
        popEnterTransition = { enterTransitionSpec(initialState, targetState, isPop = true) },
        popExitTransition = {
            exitTransitionSpec(initialState, targetState, isPop = true) ?: ExitTransition.None
        }
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }

        composable(
            "login/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            LoginScreen(navController, id)
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun enterTransitionSpec(
    from: NavBackStackEntry,
    to: NavBackStackEntry,
    isPop: Boolean = false
): EnterTransition {
    val fromRoute = from.destination.route
    val toRoute = to.destination.route

    return when {
        fromRoute == "settings" || toRoute == "settings" -> EnterTransition.None
        isPop -> slideInHorizontally(
            initialOffsetX = { -(it * 0.2f).toInt() }, // 返回时从左边滑入
            animationSpec = tween(400)
        ) + fadeIn(animationSpec = tween(200)) // 加入淡入动画
        else -> slideInHorizontally(
            initialOffsetX = { (it * 0.2f).toInt() },  // 正常进入从右边滑入
            animationSpec = tween(400)
        ) + fadeIn(animationSpec = tween(200)) // 加入淡入动画
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun exitTransitionSpec(
    from: NavBackStackEntry,
    to: NavBackStackEntry,
    isPop: Boolean = false
): ExitTransition? {
    val fromRoute = from.destination.route ?: ""
    val toRoute = to.destination.route ?: ""

    return when {
        fromRoute == "settings" && toRoute == "home" -> null // 无动画
        fromRoute == "home" && toRoute == "settings" -> null
        isPop -> slideOutHorizontally(
            targetOffsetX = { (it * 0.2f).toInt() }, // 返回时向右滑出
            animationSpec = tween(400)
        ) + fadeOut(animationSpec = tween(200)) // 加入淡出动画
        else -> slideOutHorizontally(
            targetOffsetX = { -(it * 0.2f).toInt() }, // 正常向左滑出
            animationSpec = tween(400)
        ) + fadeOut(animationSpec = tween(200)) // 加入淡出动画
    }
}