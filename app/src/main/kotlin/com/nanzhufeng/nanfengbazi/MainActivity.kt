package com.nanzhufeng.nanfengbazi

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val viewModel: StageTwoViewModel by viewModels {
        val app = application as NanfengBaziApplication
        StageTwoViewModel.Factory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var lastSingleCaseUri by remember { mutableStateOf<Uri?>(null) }
            var lastFullBackupUri by remember { mutableStateOf<Uri?>(null) }
            val createSingleCaseDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportCurrentCase {
                        contentResolver.openOutputStream(uri, "w")
                    }
                } else {
                    viewModel.clearPendingSingleCaseExport()
                }
            }
            val openSingleCaseDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    lastSingleCaseUri = uri
                    viewModel.previewSingleCase {
                        contentResolver.openInputStream(uri)
                    }
                }
            }
            val createFullBackupDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/zip"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportFullBackup {
                        contentResolver.openOutputStream(uri, "w")
                    }
                } else {
                    viewModel.clearPendingFullBackupExport()
                }
            }
            val createEncryptedFullBackupDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportFullBackup {
                        contentResolver.openOutputStream(uri, "w")
                    }
                } else {
                    viewModel.clearPendingFullBackupExport()
                }
            }
            val openFullBackupDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    lastFullBackupUri = uri
                    viewModel.previewFullBackup {
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
                onRetryPasswordSingleCaseDocument = { password ->
                    val uri = lastSingleCaseUri
                    if (uri != null) {
                        viewModel.previewSingleCaseWithPassword(password) {
                            contentResolver.openInputStream(uri)
                        }
                    } else {
                        password.fill('\u0000')
                    }
                },
                onCreateFullBackupDocument = createFullBackupDocument::launch,
                onCreateEncryptedFullBackupDocument =
                    createEncryptedFullBackupDocument::launch,
                onOpenFullBackupDocument = {
                    openFullBackupDocument.launch(
                        arrayOf("application/zip", "application/octet-stream"),
                    )
                },
                onRetryPasswordFullBackupDocument = { password ->
                    val uri = lastFullBackupUri
                    viewModel.previewFullBackupWithPassword(password) {
                        uri?.let(contentResolver::openInputStream)
                    }
                },
            )
        }
    }
}
