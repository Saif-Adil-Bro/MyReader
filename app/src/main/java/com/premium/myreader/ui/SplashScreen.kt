package com.premium.myreader.ui

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.premium.myreader.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit, onNavigateToLogin: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val sharedPreferences = context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE)
    val isBiometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false)

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = { OvershootInterpolator(2f).getInterpolation(it) })
        )
        delay(800L)
        
        if (auth.currentUser != null) {
            if (isBiometricEnabled && activity != null) {
                // 2. Biometric Prompt
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onNavigateToHome()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            onNavigateToLogin() // Error e log in page e niye jabe
                        }
                    })
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("App Lock")
                    .setSubtitle("Use your fingerprint to unlock")
                    .setNegativeButtonText("Cancel")
                    .build()
                    
                biometricPrompt.authenticate(promptInfo)
            } else {
                onNavigateToHome()
            }
        } else {
            onNavigateToLogin()
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = "App Logo", modifier = Modifier.size(130.dp).scale(scale.value))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "My Reader", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.scale(scale.value))
        }
    }
}
