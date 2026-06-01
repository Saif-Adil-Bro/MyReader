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
        // যদি fileUrl-এর ভেতর ইমেজ সংক্রান্ত কিছু থাকে, তার মানে লিংক সোয়াপ হয়েছে!
        val actualCover = if (fileUrl.contains("image") || fileUrl.contains(".jpg") || fileUrl.contains(".png")) fileUrl else coverImageUrl
        
        // যদি কভার ফটো একদমই না থাকে বা সেখানে ভুল করে পিডিএফ লিংক থাকে, তবে অটো ডিফল্ট কভার!
        return if (actualCover.isBlank() || actualCover.contains(".pdf") || actualCover.contains("book")) {
            "https://cdn-icons-png.flaticon.com/512/1173/1173260.png" // চমৎকার একটি ডিফল্ট বইয়ের আইকন
        } else actualCover
    }

    // স্মার্ট পিডিএফ চেকার
    fun getSafePdf(): String {
        // যদি coverImageUrl-এর ভেতর পিডিএফ বা বইয়ের ফোল্ডার থাকে, তার মানে লিংক সোয়াপ হয়েছে!
        return if (coverImageUrl.contains(".pdf") || coverImageUrl.contains("book")) coverImageUrl else fileUrl
    }
}
