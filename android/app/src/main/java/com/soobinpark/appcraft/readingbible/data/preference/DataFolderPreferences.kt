package com.soobinpark.appcraft.readingbible.data.preference

import android.content.Context
import android.net.Uri
import java.io.File

class DataFolderPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reading_bible_preferences", Context.MODE_PRIVATE)

    fun getTreeUri(): Uri? {
        return prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)
    }

    fun setTreeUri(uri: Uri?) {
        prefs.edit()
            .putString(KEY_TREE_URI, uri?.toString())
            .remove(KEY_LOCAL_ROOT)
            .apply()
    }

    fun getLocalRoot(): File? {
        return prefs.getString(KEY_LOCAL_ROOT, null)
            ?.let(::File)
            ?.takeIf { it.exists() && it.isDirectory }
    }

    fun setLocalRoot(root: File?) {
        prefs.edit()
            .putString(KEY_LOCAL_ROOT, root?.absolutePath)
            .remove(KEY_TREE_URI)
            .apply()
    }

    private companion object {
        const val KEY_TREE_URI = "bible_data_tree_uri"
        const val KEY_LOCAL_ROOT = "bible_data_local_root"
    }
}
