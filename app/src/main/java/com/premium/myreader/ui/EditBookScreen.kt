package com.premium.myreader.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.premium.myreader.data.Book
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(book: Book, onBackClick: () -> Unit) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var category by remember { mutableStateOf(book.category) }
    var coverUrl by remember { mutableStateOf(book.getSafeCover()) }
    var pdfUrl by remember { mutableStateOf(book.getSafePdf()) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    fun getDirectLink(link: String): String {
        if (link.contains("drive.google.com")) {
            val regex = "d/([a-zA-Z0-9_-]+)".toRegex()
            val match = regex.find(link)
            if (match != null) {
                val fileId = match.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }
        }
        return link 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Book") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Book Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = coverUrl, onValueChange = { coverUrl = it }, label = { Text("Cover Image URL (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = pdfUrl, onValueChange = { pdfUrl = it }, label = { Text("PDF / File URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotEmpty() && author.isNotEmpty() && pdfUrl.isNotEmpty()) {
                        isLoading = true
                        
                        val finalPdfUrl = getDirectLink(pdfUrl)
                        
                        // ফিক্স: কভার লিংকেও কনভার্টার অ্যাড করা হলো
                        val finalCoverUrl = if (coverUrl.isBlank()) {
                            val encodedTitle = URLEncoder.encode(title, "UTF-8")
                            "https://ui-avatars.com/api/?name=$encodedTitle&background=random&color=fff&size=512"
                        } else {
                            getDirectLink(coverUrl)
                        }

                        val bookData = mapOf(
                            "title" to title,
                            "author" to author,
                            "category" to category,
                            "coverImageUrl" to finalCoverUrl,
                            "fileUrl" to finalPdfUrl
                        )
                        
                        db.collection("books").document(book.id).update(bookData)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Book Updated Successfully!", Toast.LENGTH_SHORT).show()
                                onBackClick() 
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Please fill title, author and PDF URL", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("Update Book")
            }
        }
    }
}