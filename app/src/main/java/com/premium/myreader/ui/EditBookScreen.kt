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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(book: Book, onBackClick: () -> Unit) {
    // আগের ডাটাগুলো টেক্সটবক্সে আগে থেকেই লেখা থাকবে
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var category by remember { mutableStateOf(book.category) }
    var coverUrl by remember { mutableStateOf(book.coverImageUrl) }
    var pdfUrl by remember { mutableStateOf(book.fileUrl) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Book") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Book Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = author, onValueChange = { author = it },
                label = { Text("Author Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = coverUrl, onValueChange = { coverUrl = it },
                label = { Text("Cover Image URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pdfUrl, onValueChange = { pdfUrl = it },
                label = { Text("PDF / File URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotEmpty() && author.isNotEmpty() && pdfUrl.isNotEmpty()) {
                        isLoading = true
                        val bookData = mapOf(
                            "title" to title,
                            "author" to author,
                            "category" to category,
                            "coverImageUrl" to coverUrl,
                            "fileUrl" to pdfUrl
                        )
                        // ফায়ারবেসে বইয়ের ডাটা আপডেট করা হচ্ছে
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
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Book")
                }
            }
        }
    }
}