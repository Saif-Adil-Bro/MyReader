package com.premium.myreader.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun PdfReaderScreen(file: File) {
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }

    DisposableEffect(file) {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor)
        onDispose {
            pdfRenderer?.close()
            fileDescriptor.close()
        }
    }

    pdfRenderer?.let { renderer ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color.White)
        ) {
            items(renderer.pageCount) { index ->
                PdfPage(renderer = renderer, pageIndex = index)
            }
        }
    }
}

@Composable
fun PdfPage(renderer: PdfRenderer, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        val page = renderer.openPage(pageIndex)
        val renderedBitmap = Bitmap.createBitmap(
            page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
        )
        page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap = renderedBitmap
        page.close()
    }

    bitmap?.let { b ->
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = "Page $pageIndex",
            modifier = Modifier.fillMaxSize()
        )
    }
}