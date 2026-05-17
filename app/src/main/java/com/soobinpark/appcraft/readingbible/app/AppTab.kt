package com.soobinpark.appcraft.readingbible.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Reader("읽기", Icons.Outlined.MenuBook),
    Library("성경", Icons.Outlined.Book),
    Search("검색", Icons.Outlined.Search),
    Records("기록", Icons.Outlined.Bookmarks),
    Settings("설정", Icons.Outlined.Settings),
}
