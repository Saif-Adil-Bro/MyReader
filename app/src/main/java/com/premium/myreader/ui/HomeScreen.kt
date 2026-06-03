package com.premium.myreader.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.premium.myreader.MyReaderApplication
import com.premium.myreader.data.Book
import io.github.jan_tennert.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBookClick: (Book) -> Unit, 
    onProfileClick: () -> Unit, 
    onAddBookClick: () -> Unit,
    onEditBookClick: (Book) -> Unit 
) {
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) } 
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedTabIndex by remember { mutableStateOf(0) } 
    var bookToDelete by remember { mutableStateOf<Book?>(null) } 
    var isAdmin by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sharedPreferences = remember { context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE) }
    var favoriteBookIds by remember { mutableStateOf(sharedPreferences.getStringSet("favorites", emptySet()) ?: emptySet()) }

    // ✨ ফায়ারবেস ফেলে মাত্র ১ লাইনের সুপাবেস প্লাগইন ম্যাজিক ডাটা লোড ✨
    fun loadBooks() {
        coroutineScope.launch {
            try {
                books = MyReaderApplication.supabase.from("books").select().decodeList<Book>()
            } catch (e: Exception) {
                // কোনো এরর আসলে হ্যান্ডেল করার জায়গা
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    fun toggleFavorite(bookId: String) {
        val newFavorites = favoriteBookIds.toMutableSet()
        if (newFavorites.contains(bookId)) newFavorites.remove(bookId) else newFavorites.add(bookId)
        favoriteBookIds = newFavorites
        sharedPreferences.edit().putStringSet("favorites", newFavorites).apply()
    }

    LaunchedEffect(Unit) {
        // ✨ সুপাবেস দিয়ে ইউজার রোল চেক করার এডভান্সড লজিক (আপাতত টেস্ট করার জন্য অ্যাডমিন ট্রু রাখা হয়েছে)
        isAdmin = true 
        loadBooks()
    }

    val categories = listOf("All") + books.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    
    val filteredBooks = books.filter { book ->
        val matchesSearch = book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategory == "All") true else book.category.equals(selectedCategory, ignoreCase = true)
        val matchesTab = when (selectedTabIndex) {
            1 -> favoriteBookIds.contains(book.id) 
            2 -> File(context.cacheDir, "${book.id}.pdf").exists() 
            else -> true 
        }
        matchesSearch && matchesCategory && matchesTab
    }

    // ডিলিট অ্যালার্ট ডায়ালগ
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null }, 
            title = { Text("Delete Book") }, 
            text = { Text("Are you sure you want to delete this book from Supabase?") },
            confirmButton = { 
                TextButton(onClick = { 
                    coroutineScope.launch {
                        try {
                            // সুপাবেস থেকে ডাটা ডিলিট করার প্লাগইন লজিক
                            MyReaderApplication.supabase.from("books").delete {
                                filter { eq("id", bookToDelete!!.id) }
                            }
                            loadBooks()
                        } catch (e: Exception) { }
                        bookToDelete = null
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) } 
            },
            dismissButton = { TextButton(onClick = { bookToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("My Library", color = MaterialTheme.colorScheme.primary) }, 
                actions = { 
                    IconButton(onClick = onProfileClick) { 
                        Icon(Icons.Default.AccountCircle, "Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) 
                    } 
                }
            ) 
        },
        floatingActionButton = { 
            if (isAdmin) {
                FloatingActionButton(onClick = onAddBookClick, containerColor = MaterialTheme.colorScheme.primary) { 
                    Icon(Icons.Default.Add, "Add Book") 
                } 
            } 
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery, 
                onValueChange = { searchQuery = it }, 
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), 
                placeholder = { Text("Search by title or author...") }, 
                leadingIcon = { Icon(Icons.Default.Search, "Search") }, 
                shape = RoundedCornerShape(12.dp), 
                singleLine = true
            )
            
            LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category -> 
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }) 
                }
            }
            
            TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("All Books") })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Favorites") })
                Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("Downloaded") })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = {
                        isRefreshing = true
                        loadBooks()
                    }
                ) {
                    LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
                        items(filteredBooks) { book ->
                            BookCard(
                                book = book, 
                                isFavorite = favoriteBookIds.contains(book.id), 
                                isAdmin = isAdmin, 
                                onFavoriteClick = { toggleFavorite(book.id) }, 
                                onEditClick = { onEditBookClick(book) }, 
                                onDeleteClick = { bookToDelete = book }, 
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(book: Book, isFavorite: Boolean, isAdmin: Boolean, onFavoriteClick: () -> Unit, onEditClick: () -> Unit, onDeleteClick: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE) }
    val lastPage = sharedPreferences.getInt("last_page_${book.id}.pdf", 0)
    val totalPages = sharedPreferences.getInt("total_pages_${book.id}.pdf", 0)
    val progress = if (totalPages > 1) lastPage.toFloat() / (totalPages - 1).toFloat() else 0f
    val percentage = if (totalPages > 1) ((progress * 100).toInt()).coerceIn(0, 100) else 0

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.coverImageUrl, 
                contentDescription = "Book Cover", 
                modifier = Modifier.width(80.dp).height(120.dp).clip(RoundedCornerShape(8.dp)), 
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { 
                Text(text = book.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Author: ${book.author}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = book.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                
                if (totalPages > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$percentage% Completed", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onFavoriteClick) { 
                    Icon(imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (isFavorite) Color.Red else Color.Gray) 
                }
                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF1976D2)) }
                        IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFD32F2F)) }
                    }
                }
            }
        }
    }
}
