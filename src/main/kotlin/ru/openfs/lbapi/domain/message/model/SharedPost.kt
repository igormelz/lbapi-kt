package ru.openfs.lbapi.domain.message.model

data class SharedPost(
    val postedAt: String,
    val subject: String,
    val text: String,
)
