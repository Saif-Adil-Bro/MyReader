package com.premium.myreader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.ai.client.generativeai.GenerativeModel
import com.premium.myreader.data.Book
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(book: Book, onBackClick: () -> Unit, onReadClick: (File, String) -> Unit) {
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AQ.Ab8RN" + "6IqFByF7RT" + "urD2bzLokhYd9" + "Jx8y0Agjz6fsoI" + "axcBLG4g"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✨ কভার ফটো (যেটা আমি ভুল করে বাদ দিয়েছিলাম!) ✨
            AsyncImage(
                model = book.getSafeCover(),
                contentDescription = "Book Cover",
                modifier = Modifier.height(200.dp).width(140.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = book.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = "Author: ${book.author}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Category: ${book.category}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onReadClick(File(book.getSafePdf()), book.title) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Read Book")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // এআই সামারি বাটন
            Button(
                onClick = {
                    isAiLoading = true
                    coroutineScope.launch {
                        try {
                            val prompt = "Write a short, engaging, and spoiler-free summary for the book '${book.title}' by ${book.author} in Bengali language. Format it nicely."
                            val response = generativeModel.generateContent(prompt)
                            aiSummary = response.text
                        } catch (e: Exception) {
                            aiSummary = "দুঃখিত, সামারি জেনারেট করতে সমস্যা হয়েছে: ${e.message}"
                        } finally {
                            isAiLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onTertiary)
                } else {
                    Text("✨ Generate AI Summary")
                }
            }

            if (aiSummary != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Summary ✨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = aiSummary!!)
                    }
                }
            }
        }
    }
}
