package com.example.mima.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.mima.ui.navigation.AppNavHost
import com.example.mima.ui.theme.MimaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MimaTheme {  // 确保这里包裹了主题
                enableEdgeToEdge()
                Surface(
                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController = rememberNavController())
//                    MyApp()
                }
            }
        }
    }
}

//@Composable
//fun MyApp() {
//    MimaTheme {
//        val navController = rememberNavController()
//        AppNavHost(navController = navController)
//    }
//}