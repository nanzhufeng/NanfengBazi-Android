package com.nanzhufeng.nanfengbazi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: StageTwoViewModel by viewModels {
        val app = application as NanfengBaziApplication
        StageTwoViewModel.Factory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NanfengBaziApp(viewModel = viewModel)
        }
    }
}
