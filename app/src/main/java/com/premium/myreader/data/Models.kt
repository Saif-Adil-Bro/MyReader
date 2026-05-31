package com.premium.myreader.data

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val fileUrl: String = "",
    val fileFormat: String = "PDF",
    val isPremium: Boolean = false
)

data class UserProfile(
    val uid: String = "",
    val name: String = "Guest User",
    val email: String = "",
    val readingStreakDays: Int = 0,
    val role: String = "user"
)