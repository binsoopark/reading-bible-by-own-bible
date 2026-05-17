package com.soobinpark.appcraft.readingbible.data.preference

import android.content.Context
import android.net.Uri

class DataFolderPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reading_bible_preferences", Context.MODE_PRIVATE)

    fun getTreeUri(): Uri? {
        return prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)
    }

    fun setTreeUri(uri: Uri?) {
        prefs.edit()
            .putString(KEY_TREE_URI, uri?.toString())
            .apply()
    }

    private companion object {
        const val KEY_TREE_URI = "bible_data_tree_uri"
    }
}
