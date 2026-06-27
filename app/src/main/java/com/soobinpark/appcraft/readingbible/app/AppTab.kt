package com.soobinpark.appcraft.readingbible.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Reader("읽기", Icons.Outlined.MenuBook),
    Search("검색", Icons.Outlined.Search),
    Records("메모", Icons.Outlined.NoteAlt),
    PersonalNotes("개인메모", Icons.Outlined.EditNote),
    Settings("설정", Icons.Outlined.Settings),
}
