package com.premium.myreader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.premium.myreader.data.Book
import com.premium.myreader.ui.HomeScreen
import com.premium.myreader.ui.PdfReaderScreen
import com.premium.myreader.ui.ProfileScreen
import com.premium.myreader.ui.AddBookScreen
import com.premium.myreader.ui.EditBookScreen
import com.premium.myreader.ui.BookDetailsScreen
import com.premium.myreader.ui.SplashScreen
import java.io.File
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Home : Screen()
    object Profile : Screen() 
    object AddBook : Screen()
    data class EditBook(val book: Book) : Screen() 
    data class Details(val book: Book) : Screen() 
    data class Reader(val file: File, val title: String) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ফিক্স ১: অ্যাপ ওপেন হওয়ার সাথে সাথেই নোটিফিকেশন চ্যানেল তৈরি করা হচ্ছে
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "my_reader_channel"
            val channelName = "Book Notifications"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")

        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE) }
            var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("dark_mode", false)) }

            val colors = if (isDarkMode) darkColorScheme() else lightColorScheme()
            
            MaterialTheme(colorScheme = colors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }

                    when (val screen = currentScreen) {
                        is Screen.Splash -> SplashScreen(onNavigateToHome = { currentScreen = Screen.Home }, onNavigateToLogin = { currentScreen = Screen.Login })
                        is Screen.Login -> LoginScreen(onLoginSuccess = { currentScreen = Screen.Home })
                        is Screen.Home -> {
                            HomeScreen(
                                onBookClick = { book -> currentScreen = Screen.Details(book) }, 
                                onProfileClick = { currentScreen = Screen.Profile },
                                onAddBookClick = { currentScreen = Screen.AddBook },
                                onEditBookClick = { book -> currentScreen = Screen.EditBook(book) } 
                            )
                        }
                        is Screen.AddBook -> { BackHandler { currentScreen = Screen.Home }; AddBookScreen(onBackClick = { currentScreen = Screen.Home }) }
                        is Screen.EditBook -> { BackHandler { currentScreen = Screen.Home }; EditBookScreen(book = screen.book, onBackClick = { currentScreen = Screen.Home }) }
                        is Screen.Details -> {
                            BackHandler { currentScreen = Screen.Home }
                            BookDetailsScreen(
                                book = screen.book,
                                onBackClick = { currentScreen = Screen.Home },
                                onReadClick = { file, title -> currentScreen = Screen.Reader(file, title) }
                            )
                        }
                        is Screen.Profile -> {
                            BackHandler { currentScreen = Screen.Home }
                            ProfileScreen(isDarkMode = isDarkMode, onThemeToggle = { isDark -> isDarkMode = isDark; sharedPreferences.edit().putBoolean("dark_mode", isDark).apply() }, onBackClick = { currentScreen = Screen.Home }, onLogout = { currentScreen = Screen.Login })
                        }
                        is Screen.Reader -> { 
                            BackHandler { 
                                currentScreen = Screen.Details(
                                    Book(
                                        id = screen.file.name.replace(".pdf",""), 
                                        title = screen.title, 
                                        author = "Unknown", 
                                        category = "Downloaded", 
                                        coverImageUrl = "", 
                                        fileUrl = ""
                                    )
                                ) 
                            }
                            PdfReaderScreen(file = screen.file, title = screen.title, onBackClick = { currentScreen = Screen.Home }) 
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) } 
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("My Reader", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(text = if (isSignUpMode) "Create a new account" else "Login to your account", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, "Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, "Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))

        if (!isSignUpMode) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showResetDialog = true }) { Text("Forgot Password?", color = MaterialTheme.colorScheme.primary) }
            }
        } else { Spacer(modifier = Modifier.height(24.dp)) }

        Button(onClick = {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                isLoading = true
                if (isSignUpMode) {
                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            db.collection("users").document(auth.currentUser!!.uid).set(hashMapOf("email" to email, "role" to "user")).addOnCompleteListener {
                                isLoading = false; Toast.makeText(context, "Account Created!", Toast.LENGTH_SHORT).show(); onLoginSuccess()
                            }
                        } else { isLoading = false; Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show() }
                    }
                } else {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        isLoading = false; if (task.isSuccessful) onLoginSuccess() else Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
            } else Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), enabled = !isLoading) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) else Text(if (isSignUpMode) "Sign Up" else "Login")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { isSignUpMode = !isSignUpMode }) { Text(text = if (isSignUpMode) "Already have an account? Login" else "Don't have an account? Sign Up", color = MaterialTheme.colorScheme.secondary) }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = {
            isLoading = true; auth.signInAnonymously().addOnCompleteListener { task -> isLoading = false; if (task.isSuccessful) onLoginSuccess() else Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show() }
        }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), enabled = !isLoading) { Text("Continue as Guest") }
    }

    if (showResetDialog) {
        var resetEmail by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password") },
            text = { OutlinedTextField(value = resetEmail, onValueChange = { resetEmail = it }, label = { Text("Enter your email") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    if (resetEmail.isNotEmpty()) {
                        auth.sendPasswordResetEmail(resetEmail).addOnCompleteListener { task ->
                            if (task.isSuccessful) Toast.makeText(context, "Reset link sent!", Toast.LENGTH_LONG).show()
                            else Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                        }
                        showResetDialog = false
                    }
                }) { Text("Send Reset Link") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }
}
