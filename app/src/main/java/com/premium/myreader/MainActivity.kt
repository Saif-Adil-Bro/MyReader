package com.premium.myreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.premium.myreader.ui.theme.MyReaderTheme
import com.premium.myreader.ui.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyReaderTheme {
                var showHome by remember { mutableStateOf(false) }

                if (showHome) {
                    HomeScreen() // হোম স্ক্রিন দেখাবে
                } else {
                    LoginScreen(onLoginSuccess = { showHome = true }) // লগ-ইন স্ক্রিন দেখাবে
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
            onClick = onLoginSuccess, // বাটনে ক্লিক করলে হোম স্ক্রিনে যাবে
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Enter App")
        }
    }
}
