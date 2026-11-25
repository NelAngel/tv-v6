package com.lygone.smarttvsimulator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val apps = remember(refresh) { loadApps(this) }
            LauncherApp(apps) { app ->
                runCatching { startActivity(app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }

    override fun onResume() { super.onResume(); refresh++ }
}
