package com.premium.myreader

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.premium.myreader.data.Book
import com.premium.myreader.ui.*
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
class MainActivity : FragmentActivity() {
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = Firebase.analytics
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("my_reader_channel", "Book Notifications", android.app.NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")

        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE) }
            var isDarkMode by remember { mutableStateOf(sharedPreferences.getBoolean("dark_mode", false)) }
            
            MaterialTheme(colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                    when (val screen = currentScreen) {
                        is Screen.Splash -> SplashScreen(onNavigateToHome = { currentScreen = Screen.Home }, onNavigateToLogin = { currentScreen = Screen.Login })
                        is Screen.Login -> LoginScreen(onLoginSuccess = { currentScreen = Screen.Home })
                        is Screen.Home -> HomeScreen(onBookClick = { book -> currentScreen = Screen.Details(book) }, onProfileClick = { currentScreen = Screen.Profile }, onAddBookClick = { currentScreen = Screen.AddBook }, onEditBookClick = { book -> currentScreen = Screen.EditBook(book) })
                        is Screen.AddBook -> { BackHandler { currentScreen = Screen.Home }; AddBookScreen(onBackClick = { currentScreen = Screen.Home }) }
                        is Screen.EditBook -> { BackHandler { currentScreen = Screen.Home }; EditBookScreen(book = screen.book, onBackClick = { currentScreen = Screen.Home }) }
                        is Screen.Details -> { BackHandler { currentScreen = Screen.Home }; BookDetailsScreen(book = screen.book, onBackClick = { currentScreen = Screen.Home }, onReadClick = { file, title -> currentScreen = Screen.Reader(file, title) }) }
                        is Screen.Profile -> { BackHandler { currentScreen = Screen.Home }; ProfileScreen(isDarkMode = isDarkMode, onThemeToggle = { isDark -> isDarkMode = isDark; sharedPreferences.edit().putBoolean("dark_mode", isDark).apply() }, onBackClick = { currentScreen = Screen.Home }, onLogout = { currentScreen = Screen.Login }) }
                        is Screen.Reader -> { BackHandler { currentScreen = Screen.Details(Book(id = screen.file.name.replace(".pdf",""), title = screen.title, author = "Unknown")) }; PdfReaderScreen(file = screen.file, title = screen.title, onBackClick = { currentScreen = Screen.Home }) }
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
    var isGoogleLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("799165643788-q9odul85a99kcbe7hho917vmv19dn8fr.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)!!
            auth.signInWithCredential(GoogleAuthProvider.getCredential(account.idToken, null)).addOnCompleteListener { task ->
                isGoogleLoading = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    db.collection("users").document(user!!.uid).set(hashMapOf("email" to user.email, "name" to user.displayName, "role" to "user")).addOnCompleteListener { onLoginSuccess() }
                } else Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) { isGoogleLoading = false; Toast.makeText(context, "Google Sign-In Cancelled", Toast.LENGTH_SHORT).show() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("My Reader", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, "Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, "Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { /* Email Login Logic */ }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
            Text(if (isSignUpMode) "Sign Up" else "Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // ✨ নতুন ম্যাজিক: Google Sign-in বাটন ✨
        Button(
            onClick = {
                isGoogleLoading = true
                googleSignInClient.signOut().addOnCompleteListener { launcher.launch(googleSignInClient.signInIntent) }
            }, 
            modifier = Modifier.fillMaxWidth().height(50.dp), 
            shape = RoundedCornerShape(12.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = !isGoogleLoading
        ) {
            if (isGoogleLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(24.dp)) else Text("Sign in with Google")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { isSignUpMode = !isSignUpMode }) { Text(text = if (isSignUpMode) "Already have an account? Login" else "Don't have an account? Sign Up", color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = { onLoginSuccess() }) { Text("Continue as Guest", color = Color.Gray) }
    }
}
