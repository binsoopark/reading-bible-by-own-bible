package com.soobinpark.appcraft.readingbible.app

import androidx.annotation.StringRes
import com.soobinpark.appcraft.readingbible.R
import com.soobinpark.appcraft.readingbible.domain.model.ReadingPalette
import com.soobinpark.appcraft.readingbible.domain.model.RecordFilter
import com.soobinpark.appcraft.readingbible.domain.model.RecordSortOrder
import com.soobinpark.appcraft.readingbible.domain.model.VerseHighlight

@StringRes fun RecordFilter.labelRes(): Int = when (this) {
    RecordFilter.Bookmark -> R.string.record_filter_bookmark
    RecordFilter.Highlight -> R.string.record_filter_highlight
    RecordFilter.Read -> R.string.record_filter_read
    RecordFilter.Note -> R.string.record_filter_note
}

@StringRes fun RecordSortOrder.labelRes(): Int = when (this) {
    RecordSortOrder.Recent -> R.string.record_sort_recent
    RecordSortOrder.Bible -> R.string.record_sort_bible
}

@StringRes fun VerseHighlight.labelRes(): Int = when (this) {
    VerseHighlight.None -> R.string.highlight_none
    VerseHighlight.Yellow -> R.string.highlight_yellow
    VerseHighlight.Mint -> R.string.highlight_mint
    VerseHighlight.Blue -> R.string.highlight_blue
    VerseHighlight.Pink -> R.string.highlight_pink
    VerseHighlight.Lavender -> R.string.highlight_lavender
    VerseHighlight.Orange -> R.string.highlight_orange
}

@StringRes fun ReadingPalette.labelRes(): Int = when (this) {
    ReadingPalette.Paper -> R.string.palette_paper
    ReadingPalette.Evening -> R.string.palette_evening
    ReadingPalette.Oled -> R.string.palette_oled
    ReadingPalette.HighContrast -> R.string.palette_high_contrast
    ReadingPalette.WarmLight -> R.string.palette_warm_light
}
