package com.nanzhufeng.nanfengbazi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: StageTwoViewModel by viewModels {
        val app = application as NanfengBaziApplication
        StageTwoViewModel.Factory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val createSingleCaseDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportCurrentCase {
                        contentResolver.openOutputStream(uri, "w")
                    }
                }
            }
            val openSingleCaseDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    viewModel.previewSingleCase {
                        contentResolver.openInputStream(uri)
                    }
                }
            }
            NanfengBaziApp(
                viewModel = viewModel,
                onCreateSingleCaseDocument = createSingleCaseDocument::launch,
                onOpenSingleCaseDocument = {
                    openSingleCaseDocument.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream"),
                    )
                },
            )
        }
    }
}
