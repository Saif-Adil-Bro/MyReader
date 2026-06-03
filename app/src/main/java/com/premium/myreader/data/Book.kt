package com.premium.myreader.data

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val coverImageUrl: String,
    val fileUrl: String
)