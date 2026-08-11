package com.nanzhufeng.nanfengbazi

import android.app.AlertDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var pendingLargeBatchUris: List<Uri>? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val uris = pendingLargeBatchUris.orEmpty()
        pendingLargeBatchUris = null
        if (!granted && uris.isNotEmpty()) {
            Toast.makeText(
                this,
                "通知未开启，识别仍会继续；请回到应用查看进度。",
                Toast.LENGTH_LONG,
            ).show()
        }
        importScreenshotUrisNow(uris)
    }

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
            val createEncryptedSingleCaseDocument =
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
            val openWenzhenImportDocument = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    viewModel.previewWenzhenWebImport {
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
            val shareCaseImage = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) {
                // Third-party share activities do not provide a trustworthy send result.
                viewModel.completeCaseImageShare(cancelled = true)
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
                onCreateSingleCaseDocument = { request ->
                    when (request.kind) {
                        SingleCaseExportDocumentKind.JSON ->
                            createSingleCaseDocument.launch(request.fileName)
                        SingleCaseExportDocumentKind.BUNDLE ->
                            createSingleCaseBundleDocument.launch(request.fileName)
                        SingleCaseExportDocumentKind.ENCRYPTED_JSON,
                        SingleCaseExportDocumentKind.ENCRYPTED_BUNDLE,
                        -> createEncryptedSingleCaseDocument.launch(request.fileName)
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
                onOpenWenzhenImportDocument = {
                    openWenzhenImportDocument.launch(
                        arrayOf("application/json", "text/plain"),
                    )
                },
                onImportScreenshots = {
                    pickScreenshotImages.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                screenshotImportState = screenshotImportState,
                onRetryScreenshotImport = screenshotImportViewModel::retryRecognition,
                onConfirmScreenshotAiRecognition =
                    screenshotImportViewModel::confirmAiModelRecognition,
                onCancelScreenshotAiRecognition =
                    screenshotImportViewModel::cancelAiModelRecognition,
                onDeleteScreenshotImport = screenshotImportViewModel::deleteActiveImport,
                onSetScreenshotFieldAdopted =
                    screenshotImportViewModel::setFieldAdopted,
                onUpdateScreenshotFieldValue =
                    screenshotImportViewModel::updateFieldNormalizedValue,
                onSetScreenshotLongTextAdopted =
                    screenshotImportViewModel::setLongTextAdopted,
                onSetScreenshotCandidateAdopted =
                    screenshotImportViewModel::setCandidateAdopted,
                onCommitScreenshotCandidate =
                    screenshotImportViewModel::commitCandidate,
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
                onSaveCaseImagesToGallery = ::savePreparedCaseImagesToGallery,
                onSharePreparedCaseImages = {
                    val shareDirectory = File(cacheDir, CASE_IMAGE_SHARE_DIRECTORY)
                    val shareFiles = CASE_IMAGE_SHARE_FILE_NAMES.map { File(shareDirectory, it) }
                    val directoryReady = shareDirectory.isDirectory ||
                        shareDirectory.mkdirs()
                    if (!directoryReady) {
                        viewModel.reportCaseImageShareLaunchFailed()
                    } else {
                        viewModel.copyPreparedCaseImagesForShare(
                            openOutput = { index -> shareFiles.getOrNull(index)?.outputStream() },
                            onReady = {
                                val uris = shareFiles.mapNotNull { file ->
                                    runCatching {
                                        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                                    }.getOrNull()
                                }
                                if (uris.size != 2) {
                                    viewModel.reportCaseImageShareLaunchFailed()
                                } else {
                                    val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "image/png"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                        clipData = ClipData.newUri(
                                            contentResolver,
                                            "南枫八字命盘长图",
                                            uris.first(),
                                        ).also { clip -> uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val hasTarget = packageManager
                                        .queryIntentActivities(
                                            sendIntent,
                                            PackageManager.MATCH_DEFAULT_ONLY,
                                        )
                                        .isNotEmpty()
                                    if (!hasTarget) {
                                        viewModel.reportNoCaseImageShareTarget()
                                    } else {
                                        runCatching {
                                            shareCaseImage.launch(
                                                Intent.createChooser(
                                                    sendIntent,
                                                    "分享两张命盘长图",
                                                ),
                                            )
                                            viewModel.markCaseImageShareHandedOff()
                                        }.onFailure {
                                            viewModel.reportCaseImageShareLaunchFailed()
                                        }
                                    }
                                }
                            },
                        )
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

    private fun savePreparedCaseImagesToGallery(fileNames: List<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            viewModel.reportCaseImageGallerySaveFailed()
            return
        }
        if (fileNames.size != 2) {
            viewModel.reportCaseImageGallerySaveFailed()
            return
        }
        val uris = fileNames.mapNotNull { fileName ->
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/南枫八字")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            runCatching { contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) }.getOrNull()
        }
        if (uris.size != 2) {
            uris.forEach { uri -> runCatching { contentResolver.delete(uri, null, null) } }
            viewModel.reportCaseImageGallerySaveFailed()
            return
        }
        viewModel.exportPreparedCaseImages(
            openOutput = { index -> uris.getOrNull(index)?.let { contentResolver.openOutputStream(it, "w") } },
            onCompleted = { written ->
                if (!written) {
                    uris.forEach { uri -> runCatching { contentResolver.delete(uri, null, null) } }
                    true
                } else {
                    val published = uris.all { uri ->
                        runCatching {
                            contentResolver.update(uri, ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }, null, null) > 0
                        }.getOrDefault(false)
                    }
                    if (!published) {
                        uris.forEach { uri -> runCatching { contentResolver.delete(uri, null, null) } }
                    }
                    published
                }
            },
        )
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
        if (uris.isNotEmpty()) {
            viewModel.openRecordHub()
        }
        if (shouldRequestLargeBatchNotificationPermission(
                imageCount = uris.size,
                sdkInt = Build.VERSION.SDK_INT,
                permissionGranted = checkSelfPermission(
                    POST_NOTIFICATIONS_PERMISSION,
                ) == PackageManager.PERMISSION_GRANTED,
            )
        ) {
            pendingLargeBatchUris = uris
            AlertDialog.Builder(this)
                .setTitle("允许显示长批次识别进度？")
                .setMessage(
                    "本次选择了 ${uris.size} 张图片，AI 模型识别可能持续较久。" +
                        "允许通知后可在后台查看进度并取消；不允许也会继续导入。",
                )
                .setPositiveButton("允许通知") { _, _ ->
                    notificationPermissionLauncher.launch(
                        POST_NOTIFICATIONS_PERMISSION,
                    )
                }
                .setNegativeButton("不允许，继续") { _, _ ->
                    val pending = pendingLargeBatchUris.orEmpty()
                    pendingLargeBatchUris = null
                    Toast.makeText(
                        this,
                        "识别会继续，请回到应用查看进度。",
                        Toast.LENGTH_LONG,
                    ).show()
                    importScreenshotUrisNow(pending)
                }
                .setCancelable(false)
                .show()
            return
        }
        importScreenshotUrisNow(uris)
    }

    private fun importScreenshotUrisNow(uris: List<Uri>) {
        if (uris.isEmpty()) return
        lifecycleScope.launch {
            val sources = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
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
            }
            screenshotImportViewModel.importImages(sources)
        }
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

internal fun shouldRequestLargeBatchNotificationPermission(
    imageCount: Int,
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
    imageCount >= 8 &&
    !permissionGranted

private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
private const val CASE_IMAGE_SHARE_DIRECTORY = "case-image-share"
private val CASE_IMAGE_SHARE_FILE_NAMES = listOf(
    "shared-case-chart.png",
    "shared-case-notes.png",
)
