package com.soobinpark.appcraft.readingbible.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.soobinpark.appcraft.readingbible.R

enum class AppTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Reader(R.string.tab_reader, Icons.AutoMirrored.Outlined.MenuBook),
    Search(R.string.tab_search, Icons.Outlined.Search),
    Records(R.string.tab_records, Icons.Outlined.NoteAlt),
    PersonalNotes(R.string.tab_personal_notes, Icons.Outlined.EditNote),
    Settings(R.string.tab_settings, Icons.Outlined.Settings),
}
