package com.premium.myreader.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(file: File, onBackClick: () -> Unit) { 
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    
    DisposableEffect(file) {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor)
        onDispose {
            pdfRenderer?.close()
            fileDescriptor.close()
        }
    }

    // প্রিমিয়াম UI এর জন্য Scaffold ব্যবহার
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.nameWithoutExtension, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { // ব্যাক বাটন
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFECEFF1) // হালকা ছাই রঙের ব্যাকগ্রাউন্ড (যাতে পাতাগুলো ভাসে)
    ) { paddingValues ->
        pdfRenderer?.let { renderer ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(renderer.pageCount) { index ->
                    PdfPage(renderer = renderer, pageIndex = index)
                    Spacer(modifier = Modifier.height(24.dp)) // প্রতি পৃষ্ঠার মাঝে সুন্দর গ্যাপ
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PdfPage(renderer: PdfRenderer, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // স্ক্রল করে চলে গেলে মেমোরি ক্লিয়ার করার কোড (OOM Error থেকে বাঁচতে)
    DisposableEffect(pageIndex) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) { // ব্যাকগ্রাউন্ড থ্রেডে রেন্ডার হবে (ল্যাগ হবে না)
            try {
                val page = renderer.openPage(pageIndex)
                val scale = 2f // রেজ্যুলেশন স্কেল
                val renderedBitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                renderedBitmap.eraseColor(android.graphics.Color.WHITE) // ব্যাকগ্রাউন্ড সাদা করা
                page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                bitmap = renderedBitmap
                page.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // পাতার প্রফেশনাল ডিজাইন (Shadow ও Loading অ্যানিমেশন)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f) // স্ট্যান্ডার্ড বইয়ের শেপ
            .shadow(8.dp, RoundedCornerShape(8.dp)) // পাতার নিচে হালকা শ্যাডো
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { b ->
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "Page $pageIndex",
                modifier = Modifier.fillMaxSize()
            )
        } ?: CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // লোড হওয়ার সময় স্পিনার দেখাবে
    }
}
