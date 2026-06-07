package com.meritminder.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.meritminder.app.navigation.AppNavGraph
import com.meritminder.app.notification.NotificationHelper
import com.meritminder.app.ui.theme.MalaTheme
import com.meritminder.app.utils.LanguageManager

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)
        setContent {
            MalaTheme {
                AppNavGraph()
            }
        }
    }
}
