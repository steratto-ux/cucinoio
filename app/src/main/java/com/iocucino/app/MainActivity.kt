package com.iocucino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iocucino.app.ui.AppRoot
import com.iocucino.app.ui.MainViewModel
import com.iocucino.app.ui.theme.IoCucinoTheme

class MainActivity : ComponentActivity() {

    companion object {
        /** Schermata da aprire all'avvio (usata dal widget). */
        const val EXTRA_START = "start"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val start = intent?.getStringExtra(EXTRA_START)
        setContent {
            IoCucinoTheme {
                val vm: MainViewModel = viewModel()
                AppRoot(vm, startRoute = if (start == "planner") "planner" else "recipes")
            }
        }
    }
}
