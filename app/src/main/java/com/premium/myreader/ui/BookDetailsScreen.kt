package com.premium.myreader.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.premium.myreader.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(book: Book, onBackClick: () -> Unit, onReadClick: (File, String) -> Unit) {
    var isDownloading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    // নতুন: নির্দিষ্ট বই শেয়ার করার আইকন
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Check out this amazing book!")
                            putExtra(Intent.EXTRA_TEXT, "Hey! I am reading '${book.title}' by ${book.author} on My Reader app. Download the app to read it for free: https://play.google.com/store/apps/details?id=${context.packageName}")
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = "Cover",
                modifier = Modifier.height(250.dp).width(160.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(24.dp))
            Text(book.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("By ${book.author}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text(text = book.category, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(48.dp))

            if (isDownloading) {
                CircularProgressIndicator()
                Text("Preparing Book...", modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        isDownloading = true
                        coroutineScope.launch {
                            val file = downloadPdfLocally(context, book.fileUrl, book.id)
                            isDownloading = false
                            if (file != null) onReadClick(file, book.title)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Read Now", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

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
