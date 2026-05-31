package com.premium.myreader.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun HomeScreen(
    onBookClick: (File, String) -> Unit, 
    onProfileClick: () -> Unit, 
    onAddBookClick: () -> Unit,
    onEditBookClick: (Book) -> Unit 
) {
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    
    // নতুন: ৩টি ট্যাব ম্যানেজ করার জন্য (0 = All, 1 = Favorites, 2 = Downloaded)
    var selectedTabIndex by remember { mutableStateOf(0) } 
    
    var bookToDelete by remember { mutableStateOf<Book?>(null) } 
    var isAdmin by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val sharedPreferences = remember { context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE) }
    var favoriteBookIds by remember { mutableStateOf(sharedPreferences.getStringSet("favorites", emptySet()) ?: emptySet()) }

    fun toggleFavorite(bookId: String) {
        val newFavorites = favoriteBookIds.toMutableSet()
        if (newFavorites.contains(bookId)) newFavorites.remove(bookId) else newFavorites.add(bookId)
        favoriteBookIds = newFavorites
        sharedPreferences.edit().putStringSet("favorites", newFavorites).apply()
    }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get().addOnSuccessListener { document ->
                if (document != null && document.getString("role") == "admin") {
                    isAdmin = true
                }
            }
        }

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

    val categories = listOf("All") + books.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    // ফিল্টারিং লজিক আপডেট করা হয়েছে
    val filteredBooks = books.filter { book ->
        val matchesSearch = book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategory == "All") true else book.category.equals(selectedCategory, ignoreCase = true)
        
        val matchesTab = when (selectedTabIndex) {
            1 -> favoriteBookIds.contains(book.id) // ফেভারিট চেক
            2 -> File(context.cacheDir, "${book.id}.pdf").exists() // ডাউনলোড বা অফলাইন ফাইল চেক
            else -> true // অল বুকস
        }
        
        matchesSearch && matchesCategory && matchesTab
    }

    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book") },
            text = { Text("Are you sure you want to delete '${bookToDelete?.title}'? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("books").document(bookToDelete!!.id).delete()
                    bookToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Cancel") }
            }
        )
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
                    placeholder = { Text("Search by title or author...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }

                // নতুন: ৩টি ট্যাব (All Books, Favorites, Downloaded)
                TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                    Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("All Books") })
                    Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Favorites") })
                    Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("Downloaded") })
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (filteredBooks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        val emptyText = when (selectedTabIndex) {
                            1 -> if (books.isNotEmpty()) "No favorite books yet." else "No books available."
                            2 -> if (books.isNotEmpty()) "No offline books available. Read a book to download it automatically." else "No books available."
                            else -> "No books found."
                        }
                        Text(emptyText) 
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 8.dp), modifier = Modifier.fillMaxSize()) {
                        items(filteredBooks) { book ->
                            val isFav = favoriteBookIds.contains(book.id)
                            BookCard(
                                book = book, 
                                isFavorite = isFav,
                                isAdmin = isAdmin, 
                                onFavoriteClick = { toggleFavorite(book.id) },
                                onEditClick = { onEditBookClick(book) },     
                                onDeleteClick = { bookToDelete = book },     
                                onClick = {
                                    if (book.fileUrl.isNotEmpty()) {
                                        isDownloading = true
                                        coroutineScope.launch {
                                            val downloadedFile = downloadPdf(context, book.fileUrl, book.id)
                                            isDownloading = false
                                            if (downloadedFile != null) onBookClick(downloadedFile, book.title) 
                                        }
                                    }
                                }
                            )
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
fun BookCard(
    book: Book, 
    isFavorite: Boolean, 
    isAdmin: Boolean, 
    onFavoriteClick: () -> Unit, 
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = book.coverImageUrl, contentDescription = "Book Cover", modifier = Modifier.width(80.dp).height(120.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) { 
                Text(text = book.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Author: ${book.author}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Category: ${book.category}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Icon",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1976D2)) 
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F)) 
                        }
                    }
                }
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
