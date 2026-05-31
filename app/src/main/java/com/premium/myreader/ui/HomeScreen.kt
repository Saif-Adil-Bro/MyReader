package com.premium.myreader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.premium.myreader.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onBookClick: (File, String) -> Unit, onProfileClick: () -> Unit, onAddBookClick: () -> Unit) { // নতুন: String যুক্ত করা হয়েছে
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val auth = FirebaseAuth.getInstance()
    val isAdmin = auth.currentUser?.email?.equals("rafuse2024@gmail.com", ignoreCase = true) == true

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("books").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) {
                isLoading = false
                return@addSnapshotListener
            }
            val list = mutableListOf<Book>()
            for (document in snapshot.documents) {
                val book = Book(
                    id = document.id,
                    title = document.getString("title") ?: "Unknown Title",
                    author = document.getString("author") ?: "Unknown Author",
                    category = document.getString("category") ?: "Uncategorized",
                    coverImageUrl = document.getString("coverImageUrl") ?: "",
                    fileUrl = document.getString("fileUrl") ?: ""
                )
                list.add(book)
            }
            books = list
            isLoading = false
        }
    }

    val filteredBooks = books.filter { book ->
        book.title.contains(searchQuery, ignoreCase = true) || 
        book.author.contains(searchQuery, ignoreCase = true) ||
        book.category.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library", color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = onAddBookClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(Icons.Default.Add, contentDescription = "Add Book")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search by title, author or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (filteredBooks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (books.isEmpty()) "No books available." else "No books found matching your search.") }
                } else {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 8.dp), modifier = Modifier.fillMaxSize()) {
                        items(filteredBooks) { book ->
                            BookCard(book = book, onClick = {
                                if (book.fileUrl.isNotEmpty()) {
                                    isDownloading = true
                                    coroutineScope.launch {
                                        val downloadedFile = downloadPdf(context, book.fileUrl, book.id)
                                        isDownloading = false
                                        // নতুন: ফাইলের সাথে বইয়ের নামও পাঠানো হচ্ছে
                                        if (downloadedFile != null) onBookClick(downloadedFile, book.title) 
                                    }
                                }
                            })
                        }
                    }
                }
            }

            if (isDownloading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(elevation = CardDefaults.cardElevation(8.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Downloading Book...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(book: Book, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp)) {
            AsyncImage(model = book.coverImageUrl, contentDescription = "Book Cover", modifier = Modifier.width(80.dp).height(120.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = book.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Author: ${book.author}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Category: ${book.category}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

suspend fun downloadPdf(context: android.content.Context, urlString: String, bookId: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "$bookId.pdf")
            if (file.exists() && file.length() > 0) return@withContext file
            val url = URL(urlString)
            val connection = url.openConnection().apply { connect() }
            connection.getInputStream().use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
