package com.premium.myreader

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
import com.premium.myreader.ui.theme.MyReaderTheme
import com.premium.myreader.ui.HomeScreen
import com.premium.myreader.ui.PdfReaderScreen
import java.io.File
import dagger.hilt.android.AndroidEntryPoint

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
                val auth = FirebaseAuth.getInstance()

                // যদি ইউজার আগে থেকেই লগ-ইন করা থাকে, তবে সরাসরি হোম স্ক্রিনে যাবে
                LaunchedEffect(Unit) {
                    if (auth.currentUser != null) {
                        currentScreen = Screen.Home
                    }
                }

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
                        PdfReaderScreen(file = screen.file, onBackClick = {
                            currentScreen = Screen.Home
                        })
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
    
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Reader", 
            style = MaterialTheme.typography.displaySmall, 
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isSignUpMode) "Create a new account" else "Login to your account",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // ইমেইল ইনপুট ফিল্ড
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // পাসওয়ার্ড ইনপুট ফিল্ড
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // লগ-ইন / সাইন-আপ বাটন
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    if (isSignUpMode) {
                        // নতুন একাউন্ট তৈরি
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Account Created!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        // লগ-ইন করা
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(if (isSignUpMode) "Sign Up" else "Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // মোড চেঞ্জ করার বাটন (Login <-> Sign Up)
        TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
            Text(
                text = if (isSignUpMode) "Already have an account? Login" else "Don't have an account? Sign Up",
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // গেস্ট (Anonymous) লগ-ইন বাটন
        OutlinedButton(
            onClick = {
                isLoading = true
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            Text("Continue as Guest")
        }
    }
}
