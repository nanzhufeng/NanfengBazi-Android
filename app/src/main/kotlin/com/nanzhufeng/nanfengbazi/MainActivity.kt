package com.nanzhufeng.nanfengbazi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: StageTwoViewModel by viewModels {
        val app = application as NanfengBaziApplication
        StageTwoViewModel.Factory(app.container)
    }
    private val screenshotImportViewModel: ScreenshotImportViewModel by viewModels {
        val app = application as NanfengBaziApplication
        ScreenshotImportViewModel.Factory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val screenshotImportState by
                screenshotImportViewModel.state.collectAsStateWithLifecycle()
            var lastSingleCaseUri by remember { mutableStateOf<Uri?>(null) }
            var lastFullBackupUri by remember { mutableStateOf<Uri?>(null) }
            val pickScreenshotImages = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia(20),
            ) { uris ->
                importScreenshotUris(uris)
            }
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
            val createSingleCaseBundleDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/zip"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportCurrentCase {
                        contentResolver.openOutputStream(uri, "w")
                    }
                } else {
                    viewModel.clearPendingSingleCaseExport()
                }
            }
            val createEncryptedSingleCaseBundleDocument =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument(
                        "application/octet-stream",
                    ),
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
                onCreateSingleCaseDocument = { fileName ->
                    when {
                        fileName.endsWith(".json") ->
                            createSingleCaseDocument.launch(fileName)
                        fileName.contains("_加密") ->
                            createEncryptedSingleCaseBundleDocument.launch(fileName)
                        else -> createSingleCaseBundleDocument.launch(fileName)
                    }
                },
                onOpenSingleCaseDocument = {
                    openSingleCaseDocument.launch(
                        arrayOf(
                            "application/json",
                            "text/plain",
                            "application/zip",
                            "application/octet-stream",
                        ),
                    )
                },
                onImportScreenshots = {
                    pickScreenshotImages.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                screenshotImportState = screenshotImportState,
                onRetryScreenshotImport = screenshotImportViewModel::retryRecognition,
                onDeleteScreenshotImport = screenshotImportViewModel::deleteActiveImport,
                onConsumeScreenshotImportMessage =
                    screenshotImportViewModel::consumeMessage,
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
                onCommitSingleCaseImport = { decision ->
                    viewModel.commitSingleCaseImport(
                        decision = decision,
                        openInput = {
                            lastSingleCaseUri?.let(contentResolver::openInputStream)
                        },
                    )
                },
                onCommitSingleCaseMerge = {
                    viewModel.commitSingleCaseMerge(
                        openInput = {
                            lastSingleCaseUri?.let(contentResolver::openInputStream)
                        },
                    )
                },
                onCommitPasswordSingleCaseDocument = { password ->
                    viewModel.commitPendingSingleCaseBundle(password) {
                        lastSingleCaseUri?.let(contentResolver::openInputStream)
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
                onExecuteFullBackupDocument = { password ->
                    val uri = lastFullBackupUri
                    viewModel.executeFullBackupRestore(password) {
                        uri?.let(contentResolver::openInputStream)
                    }
                },
            )
        }
        consumeSharedImages(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSharedImages(intent)
    }

    internal fun consumeSharedImages(sourceIntent: Intent) {
        val uris = when (sourceIntent.action) {
            Intent.ACTION_SEND -> listOfNotNull(sourceIntent.sharedImageUri())
            Intent.ACTION_SEND_MULTIPLE -> sourceIntent.sharedImageUris()
            else -> emptyList()
        }
        if (uris.isNotEmpty()) {
            importScreenshotUris(uris.distinct())
            sourceIntent.action = null
            sourceIntent.removeExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun importScreenshotUris(uris: List<Uri>) {
        val sources = uris.mapNotNull { uri ->
            runCatching {
                PendingImportImage(
                    originalFileName = displayName(uri) ?: "共享图片",
                    mimeType = contentResolver.getType(uri)
                        ?.takeIf { it.startsWith("image/") }
                        ?: "image/unknown",
                    openInput = { contentResolver.openInputStream(uri) },
                )
            }.getOrNull()
        }
        screenshotImportViewModel.importImages(sources)
    }

    private fun displayName(uri: Uri): String? =
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }

    @Suppress("DEPRECATION")
    private fun Intent.sharedImageUri(): Uri? =
        getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun Intent.sharedImageUris(): List<Uri> =
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
}
