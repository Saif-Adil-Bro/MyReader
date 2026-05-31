package com.premium.myreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.premium.myreader.ui.theme.MyReaderTheme
import com.premium.myreader.ui.HomeScreen
import com.premium.myreader.ui.PdfReaderScreen // <-- এই লাইনটি ঠিক করা হয়েছে
import java.io.File
import dagger.hilt.android.AndroidEntryPoint

// স্ক্রিনগুলো ম্যানেজ করার জন্য State
sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    data class Reader(val file: File) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyReaderTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

                when (val screen = currentScreen) {
                    is Screen.Login -> {
                        LoginScreen(onLoginSuccess = { currentScreen = Screen.Home })
                    }
                    is Screen.Home -> {
                        HomeScreen(onBookClick = { downloadedFile ->
                            currentScreen = Screen.Reader(downloadedFile)
                        })
                    }
                    is Screen.Reader -> {
                        BackHandler {
                            currentScreen = Screen.Home
                        }
                        // এখানে onBackClick যুক্ত করা হলো
                        PdfReaderScreen(file = screen.file, onBackClick = {
                            currentScreen = Screen.Home 
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("My Reader", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onLoginSuccess,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Enter App")
        }
    }
}
