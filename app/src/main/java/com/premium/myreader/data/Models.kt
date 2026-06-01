package com.premium.myreader.data

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "",
    val coverImageUrl: String = "",
    val fileUrl: String = ""
) {
    // স্মার্ট কভার ফটো চেকার
    fun getSafeCover(): String {
        val actualCover = if (fileUrl.contains("image") || fileUrl.contains(".jpg") || fileUrl.contains(".png")) fileUrl else coverImageUrl
        
        return if (actualCover.isBlank() || actualCover.contains(".pdf") || actualCover.contains("book")) {
            "https://cdn-icons-png.flaticon.com/512/1173/1173260.png"
        } else actualCover
    }

    // স্মার্ট পিডিএফ চেকার
    fun getSafePdf(): String {
        return if (coverImageUrl.contains(".pdf") || coverImageUrl.contains("book")) coverImageUrl else fileUrl
    }
}
