package com.soobinpark.appcraft.readingbible.domain.model

data class ReadingProgress(
    val versionCode: String?,
    val bookIndex: Int,
    val chapter: Int,
)
