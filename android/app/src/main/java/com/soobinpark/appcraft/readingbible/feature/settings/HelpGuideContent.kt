package com.soobinpark.appcraft.readingbible.feature.settings

import androidx.annotation.StringRes
import com.soobinpark.appcraft.readingbible.R

data class HelpItem(@StringRes val titleRes: Int, @StringRes val detailRes: Int)
data class HelpSection(@StringRes val titleRes: Int, val items: List<HelpItem>)

object HelpGuideContent {
    val sections = listOf(
        HelpSection(R.string.help_section_tabs, listOf(
            HelpItem(R.string.tab_reader, R.string.help_tab_reader_detail), HelpItem(R.string.tab_search, R.string.help_tab_search_detail),
            HelpItem(R.string.tab_records, R.string.help_tab_records_detail), HelpItem(R.string.tab_personal_notes, R.string.help_tab_personal_notes_detail),
            HelpItem(R.string.tab_settings, R.string.help_tab_settings_detail),
        )),
        HelpSection(R.string.help_section_reader_basic, listOf(
            HelpItem(R.string.help_book_chapter_title, R.string.help_book_chapter_detail), HelpItem(R.string.help_prev_next_title, R.string.help_prev_next_detail),
            HelpItem(R.string.help_version_compare_title, R.string.help_version_compare_detail),
        )),
        HelpSection(R.string.help_section_reader_gestures, listOf(
            HelpItem(R.string.help_swipe_title, R.string.help_swipe_detail), HelpItem(R.string.help_pinch_title, R.string.help_pinch_detail),
            HelpItem(R.string.help_keyboard_title, R.string.help_keyboard_detail),
        )),
        HelpSection(R.string.help_section_reader_actions, listOf(
            HelpItem(R.string.record_filter_bookmark, R.string.help_bookmark_detail), HelpItem(R.string.help_range_highlight_title, R.string.help_range_highlight_detail),
            HelpItem(R.string.help_read_check_title, R.string.help_read_check_detail), HelpItem(R.string.help_verse_note_title, R.string.help_verse_note_detail),
            HelpItem(R.string.help_note_icon_title, R.string.help_note_icon_detail),
        )),
        HelpSection(R.string.help_section_reader_selection, listOf(
            HelpItem(R.string.help_selection_start_title, R.string.help_selection_start_detail), HelpItem(R.string.help_selection_toggle_title, R.string.help_selection_toggle_detail),
            HelpItem(R.string.help_copy_share_title, R.string.help_copy_share_reader_detail),
        )),
        HelpSection(R.string.help_section_search, listOf(
            HelpItem(R.string.help_search_title, R.string.help_search_detail), HelpItem(R.string.help_more_results_title, R.string.help_more_results_detail),
            HelpItem(R.string.help_open_reader_title, R.string.help_open_reader_detail),
        )),
        HelpSection(R.string.help_section_records, listOf(
            HelpItem(R.string.help_record_types_title, R.string.help_record_types_detail), HelpItem(R.string.help_highlight_color_title, R.string.help_highlight_color_detail),
            HelpItem(R.string.help_verse_note_title, R.string.help_verse_note_records_detail), HelpItem(R.string.help_copy_share_title, R.string.help_copy_share_records_detail),
        )),
        HelpSection(R.string.help_section_personal_notes, listOf(
            HelpItem(R.string.help_add_note_title, R.string.help_add_note_detail), HelpItem(R.string.help_edit_delete_title, R.string.help_edit_delete_detail),
            HelpItem(R.string.action_backup, R.string.help_backup_detail), HelpItem(R.string.action_restore, R.string.help_restore_detail),
        )),
        HelpSection(R.string.help_section_settings, listOf(
            HelpItem(R.string.help_bible_data_title, R.string.help_bible_data_detail), HelpItem(R.string.help_reading_style_title, R.string.help_reading_style_detail),
            HelpItem(R.string.help_record_backup_title, R.string.help_record_backup_detail),
        )),
    )
}
