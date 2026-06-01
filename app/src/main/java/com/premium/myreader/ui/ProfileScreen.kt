package com.premium.myreader.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit, 
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val userEmail = user?.email ?: "Guest User"
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE) }
    
    var isBiometricEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("biometric_enabled", false)) }
    
    val historyList = remember {
        sharedPreferences.all.filterKeys { it.startsWith("last_page_") }
            .mapNotNull { (key, value) ->
                val bookId = key.removePrefix("last_page_").removeSuffix(".pdf")
                val lastPage = value as? Int ?: 0
                val totalPages = sharedPreferences.getInt("total_pages_${bookId}.pdf", 0)
                if (lastPage > 0 && totalPages > 1) {
                    val percent = ((lastPage.toFloat() / (totalPages - 1)) * 100).toInt().coerceIn(0, 100)
                    Pair(bookId, percent)
                } else null
            }.sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Welcome!", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = userEmail, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Dark Mode", style = MaterialTheme.typography.titleMedium)
                            Switch(checked = isDarkMode, onCheckedChange = onThemeToggle)
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "App Lock (Fingerprint)", style = MaterialTheme.typography.titleMedium)
                            }
                            Switch(checked = isBiometricEnabled, onCheckedChange = { isBiometricEnabled = it; sharedPreferences.edit().putBoolean("biometric_enabled", it).apply() })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (historyList.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Reading History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            historyList.forEach { (bookId, percent) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Book Data: $bookId", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.weight(1f))
                                    Text(text = "$percent%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(progress = percent / 100f, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "My Reader App")
                                putExtra(Intent.EXTRA_TEXT, "Read amazing books for free on My Reader app! Download now: https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Share App with Friends", style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            val uri = Uri.parse("market://details?id=${context.packageName}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))) }
                        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rate", tint = Color(0xFFFFB300))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Rate Us on Play Store", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { auth.signOut(); onLogout() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Logout")
                }
            }
        }
    }
}
