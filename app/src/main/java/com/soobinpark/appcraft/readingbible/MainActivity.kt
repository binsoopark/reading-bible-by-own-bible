package com.soobinpark.appcraft.readingbible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.soobinpark.appcraft.readingbible.app.ReadingBibleApp
import com.soobinpark.appcraft.readingbible.core.design.ReadingBibleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReadingBibleTheme {
                ReadingBibleApp()
            }
        }
    }
}
