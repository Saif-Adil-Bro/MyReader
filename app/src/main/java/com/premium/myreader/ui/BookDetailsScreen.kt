package com.premium.myreader.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.premium.myreader.BuildConfig
import com.premium.myreader.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(book: Book, onBackClick: () -> Unit, onReadClick: (File, String) -> Unit) {
    var isDownloading by remember { mutableStateOf(false) }
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Check out this amazing book!")
                            putExtra(Intent.EXTRA_TEXT, "Hey! I am reading '${book.title}' by ${book.author} on My Reader app.")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Book"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Book")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(model = book.coverImageUrl, contentDescription = "Cover", modifier = Modifier.height(250.dp).width(160.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(24.dp))
            
            Text(book.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Author: ${book.author}", style = MaterialTheme.typography.titleMedium)
            Text("Category: ${book.category}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(32.dp))

            if (isDownloading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = {
                    isDownloading = true
                    coroutineScope.launch {
                        val file = downloadPdfLocally(context, book.fileUrl, book.id)
                        isDownloading = false
                        if (file != null) onReadClick(file, book.title)
                        else Toast.makeText(context, "Failed to download book.", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Read Book")
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    isAiLoading = true
                    coroutineScope.launch {
                        val apiKey = BuildConfig.GEMINI_API_KEY
                        val prompt = "Write a short summary for the book '${book.title}' by ${book.author} in Bengali."
                        aiSummary = generateSummaryDirectly(apiKey, prompt)
                        isAiLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                if (isAiLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onTertiary)
                else Text("✨ Generate AI Summary")
            }

            if (aiSummary != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Summary ✨", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = aiSummary!!)
                    }
                }
            }
        }
    }
}

// 🌐 গুগলের বাগ-ভরা লাইব্রেরি বাইপাস করার জন্য ডিরেক্ট এপিআই কল ফাংশন
suspend fun generateSummaryDirectly(apiKey: String, prompt: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }.toString()

            connection.outputStream.use { it.write(jsonBody.toByteArray()) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                return@withContext jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).getString("text")
            } else {
                val err = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown Error"
                return@withContext "API Error (${connection.responseCode}): $err"
            }
        } catch (e: Exception) {
            return@withContext "Error: ${e.message}"
        }
    }
}

// 📂 পিডিএফ ফাইল লোকালি ডাউনলোড করার ব্যাকগ্রাউন্ড ফাংশন
suspend fun downloadPdfLocally(context: Context, urlString: String, bookId: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "$bookId.pdf")
            if (file.exists() && file.length() > 0) return@withContext file
            val url = URL(urlString)
            val connection = url.openConnection().apply { connect() }
            connection.getInputStream().use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            file
        } catch (e: Exception) {
            null
        }
    }
}
