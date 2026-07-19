package com.soobinpark.appcraft.readingbible.domain.model

data class ReadingProgress(
    val versionCode: String?,
    val comparisonVersionCode: String?,
    val hasSavedComparisonVersion: Boolean,
    val bookIndex: Int,
    val chapter: Int,
)
