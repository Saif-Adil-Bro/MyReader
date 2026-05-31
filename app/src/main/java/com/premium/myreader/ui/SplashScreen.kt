package com.premium.myreader.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite // (আইকনটি পরিবর্তন করা হলো)
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit, onNavigateToLogin: () -> Unit) {
    // অ্যানিমেশনের জন্য
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // ১ সেকেন্ড ধরে লোগোটি জুম হয়ে সামনে আসবে
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        // আরও ১ সেকেন্ড অপেক্ষা করবে
        delay(1000)
        
        // চেক করবে ইউজার আগে থেকে লগ-ইন করা কি না
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    // স্প্ল্যাশ স্ক্রিনের ডিজাইন (নিল ব্যাকগ্রাউন্ড ও সাদা লোগো)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Favorite, // (এখানেও পরিবর্তন করা হলো)
                contentDescription = "App Logo",
                tint = Color.White,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value) // অ্যানিমেশন অ্যাপ্লাই করা হলো
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Reader",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
        }
    }
}
