package com.soobinpark.appcraft.readingbible.domain.model

import java.util.UUID

data class PersonalNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    companion object {
        fun create(title: String = "", body: String = "") = PersonalNote(
            title = title,
            body = body,
        )
    }
}

data class PersonalNoteBackupFile(
    val format: String = FORMAT_ID,
    val version: Int = CURRENT_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val notes: List<PersonalNote> = emptyList(),
) {
    companion object {
        const val FORMAT_ID = "reading-my-bible-personal-notes"
        const val CURRENT_VERSION = 1
    }
}
