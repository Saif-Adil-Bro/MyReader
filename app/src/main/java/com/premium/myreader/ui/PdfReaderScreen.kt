package com.premium.myreader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(file: File, title: String, onBackClick: () -> Unit) { 
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("MyReaderPrefs", Context.MODE_PRIVATE) }
    val bookKey = "last_page_${file.name}" 
    
    val savedPage = remember { sharedPreferences.getInt(bookKey, 0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = savedPage)
    
    DisposableEffect(file) {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor)
        onDispose {
            pdfRenderer?.close()
            fileDescriptor.close()
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        sharedPreferences.edit().putInt(bookKey, listState.firstVisibleItemIndex).apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background 
    ) { paddingValues ->
        pdfRenderer?.let { renderer ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(renderer.pageCount) { index ->
                    PdfPage(renderer = renderer, pageIndex = index)
                    Spacer(modifier = Modifier.height(24.dp))
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
    
    // জুম করার জন্য State
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(pageIndex) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) { 
            try {
                val page = renderer.openPage(pageIndex)
                val renderScale = 2.5f // রেজ্যুলেশন আরও ক্লিয়ার করা হলো জুমের জন্য
                val renderedBitmap = Bitmap.createBitmap((page.width * renderScale).toInt(), (page.height * renderScale).toInt(), Bitmap.Config.ARGB_8888)
                renderedBitmap.eraseColor(android.graphics.Color.WHITE) 
                page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap = renderedBitmap
                page.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clipToBounds() // জুম করলে যেন বক্সের বাইরে না যায়
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // সর্বোচ্চ ৩ গুণ বড় করা যাবে
                    scale = (scale * zoom).coerceIn(1f, 3f) 
                    val maxX = (size.width * (scale - 1)) / 2
                    val maxY = (size.height * (scale - 1)) / 2
                    offset = if (scale > 1f) {
                        Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    } else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { b ->
            Image(
                bitmap = b.asImageBitmap(), 
                contentDescription = "Page $pageIndex", 
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale, 
                        scaleY = scale,
                        translationX = offset.x, 
                        translationY = offset.y
                    )
            )
        } ?: CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) 
    }
}
