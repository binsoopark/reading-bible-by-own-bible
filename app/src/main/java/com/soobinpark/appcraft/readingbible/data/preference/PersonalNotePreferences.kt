package com.soobinpark.appcraft.readingbible.data.preference

import android.content.Context
import com.soobinpark.appcraft.readingbible.domain.model.PersonalNote
import com.soobinpark.appcraft.readingbible.domain.model.PersonalNoteBackupFile
import org.json.JSONArray
import org.json.JSONObject

class PersonalNotePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reading_bible_preferences", Context.MODE_PRIVATE)

    fun getNotes(): List<PersonalNote> {
        val raw = prefs.getString(KEY_PERSONAL_NOTES, null) ?: return emptyList()
        return fromJsonArray(raw)
    }

    fun setNotes(notes: List<PersonalNote>) {
        prefs.edit()
            .putString(KEY_PERSONAL_NOTES, toJsonArray(notes))
            .apply()
    }

    fun exportBackup(notes: List<PersonalNote>): String {
        val file = PersonalNoteBackupFile(notes = notes)
        val notesArray = JSONArray()
        file.notes.forEach { note ->
            notesArray.put(note.toJson())
        }
        return JSONObject()
            .put("format", file.format)
            .put("version", file.version)
            .put("exportedAtMillis", file.exportedAtMillis)
            .put("notes", notesArray)
            .toString(2)
    }

    fun importBackup(raw: String): List<PersonalNote> {
        val root = JSONObject(raw)
        check(root.optString("format") == PersonalNoteBackupFile.FORMAT_ID) {
            "개인메모 백업 파일 형식이 올바르지 않습니다."
        }
        val notesArray = root.optJSONArray("notes") ?: JSONArray()
        return buildList {
            for (index in 0 until notesArray.length()) {
                notesArray.optJSONObject(index)?.let { add(it.toPersonalNote()) }
            }
        }.sortedByDescending { it.updatedAtMillis }
    }

    private fun fromJsonArray(raw: String): List<PersonalNote> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(item.toPersonalNote())
                }
            }
        }.getOrDefault(emptyList())
            .sortedByDescending { it.updatedAtMillis }
    }

    private fun toJsonArray(notes: List<PersonalNote>): String {
        val array = JSONArray()
        notes.sortedByDescending { it.updatedAtMillis }.forEach { note ->
            array.put(note.toJson())
        }
        return array.toString()
    }

    private fun PersonalNote.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("body", body)
            .put("createdAtMillis", createdAtMillis)
            .put("updatedAtMillis", updatedAtMillis)
    }

    private fun JSONObject.toPersonalNote(): PersonalNote {
        return PersonalNote(
            id = optString("id"),
            title = optString("title"),
            body = optString("body"),
            createdAtMillis = optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = optLong("updatedAtMillis", System.currentTimeMillis()),
        )
    }

    private companion object {
        const val KEY_PERSONAL_NOTES = "personal_notes"
    }
}
