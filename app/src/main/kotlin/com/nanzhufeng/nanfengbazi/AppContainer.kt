package com.nanzhufeng.nanfengbazi

import android.app.Application
import androidx.room.Room
import androidx.room.InvalidationTracker
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudCredentialStore
import com.nanzhufeng.nanfengbazi.cloud.BaziGoogleAvatarCache
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudSnapshotBridge
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudSyncCoordinator
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudSyncScheduler
import com.nanzhufeng.nanfengbazi.cloud.BaziGoogleSignInClient
import com.nanzhufeng.nanfengbazi.cloud.BaziSupabaseConfig
import com.nanzhufeng.nanfengbazi.cloud.BaziSupabaseGateway
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupOperations
import com.nanzhufeng.nanfengbazi.data.backup.CaseBackupService
import com.nanzhufeng.nanfengbazi.data.db.DatabaseMigrations
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseBundleOperations
import com.nanzhufeng.nanfengbazi.data.exchange.SingleCaseBundleService
import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.data.repository.RoomImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.AlmanacReader
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.FourPillarsLookup
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeAlmanacReader
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeFourPillarsLookup
import com.nanzhufeng.nanfengbazi.imageparser.AnchorBasedWenzhenPageClassifier
import com.nanzhufeng.nanfengbazi.imageparser.DHashImageFingerprintEngine
import com.nanzhufeng.nanfengbazi.imageparser.ImportImageContentReader
import com.nanzhufeng.nanfengbazi.imageparser.ImportRecognitionCoordinator
import java.io.File
import java.nio.file.Path

interface AppContainer {
    val almanacReader: AlmanacReader
    val caseRepository: CaseRepository
    val caseCatalogStore: CaseCatalogStore
    val baziEngine: BaziEngine
    val fourPillarsLookup: FourPillarsLookup
    val caseBackupService: CaseBackupOperations
    val singleCaseBundleService: SingleCaseBundleOperations
    val importSessionRepository: ImportSessionRepository
    val importImageStore: PrivateImportImageStore
    val importRecognitionCoordinator: ImportRecognitionCoordinator
    val screenshotRecognitionScheduler: ScreenshotRecognitionScheduler
    val screenshotImportCommitter: ScreenshotImportCommitter
    val backupAttachmentRoot: Path
    val backupWorkRoot: Path
    val calculationPreferenceStore: CalculationPreferenceStore
    val baziCompatibilityHistoryStore: BaziCompatibilityHistoryStore
    val baziSkinPreferenceStore: BaziSkinPreferenceStore
    val aiCommentarySettings: AiCommentarySettingsStore
    val aiCommentaryGenerator: AiCommentaryGenerator
    val aiCommentaryCallLog: AiCommentaryCallLogStore
    val cloudSyncCoordinator: BaziCloudSyncCoordinator
    val googleSignInClient: BaziGoogleSignInClient
}

class DefaultAppContainer(
    application: Application,
) : AppContainer {
    override val almanacReader: AlmanacReader = TymeAlmanacReader()
    override val calculationPreferenceStore: CalculationPreferenceStore =
        AndroidCalculationPreferenceStore(application)
    override val baziCompatibilityHistoryStore: BaziCompatibilityHistoryStore =
        LocalBaziCompatibilityHistoryStore(application)
    override val baziSkinPreferenceStore: BaziSkinPreferenceStore =
        BaziSkinPreferenceStore(application)
    override val aiCommentarySettings: AiCommentarySettingsStore =
        SecureAiCommentarySettings(application)
    override val aiCommentaryGenerator: AiCommentaryGenerator =
        OpenAiCompatibleAiCommentaryGenerator()
    override val aiCommentaryCallLog: AiCommentaryCallLogStore =
        LocalAiCommentaryCallLogStore(application)
    private val database = Room.databaseBuilder(
        application,
        NanfengBaziDatabase::class.java,
        DATABASE_NAME,
    )
        .addMigrations(
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5,
            DatabaseMigrations.MIGRATION_5_6,
            DatabaseMigrations.MIGRATION_6_7,
            DatabaseMigrations.MIGRATION_7_8,
            DatabaseMigrations.MIGRATION_8_9,
            DatabaseMigrations.MIGRATION_9_10,
            DatabaseMigrations.MIGRATION_10_11,
            DatabaseMigrations.MIGRATION_11_12,
        )
        .build()

    override val caseRepository: CaseRepository = RoomCaseRepository(database)
    override val caseCatalogStore: CaseCatalogStore =
        RepositoryCaseCatalogStore(
            repository = caseRepository,
            persistentCache = FileCaseCatalogSnapshotCache(
                File(application.noBackupFilesDir, "case-catalog-v1.json"),
            ),
        )
    override val baziEngine: BaziEngine = TymeBaziEngine()
    override val fourPillarsLookup: FourPillarsLookup = TymeFourPillarsLookup(baziEngine)
    override val caseBackupService: CaseBackupOperations = CaseBackupService(
        database = database,
        stagingDatabaseContext = application,
        compatibilityHistoryFile = File(
            application.noBackupFilesDir,
            "bazi-compatibility-history-v1.json",
        ).toPath(),
    )
    override val singleCaseBundleService: SingleCaseBundleOperations =
        SingleCaseBundleService(database)
    override val importSessionRepository: ImportSessionRepository =
        RoomImportSessionRepository(database)
    override val importImageStore: PrivateImportImageStore =
        PrivateImportImageStore(application.filesDir.toPath().resolve("import-images"))
    override val importRecognitionCoordinator: ImportRecognitionCoordinator =
        ImportRecognitionCoordinator(
            repository = importSessionRepository,
            contentReader = ImportImageContentReader(importImageStore::readBytes),
            ocrEngine = AiVisionScreenshotOcrEngine(
                settings = aiCommentarySettings,
                callLog = aiCommentaryCallLog,
            ),
            pageClassifier = AnchorBasedWenzhenPageClassifier(),
            parseResultRefiner =
                com.nanzhufeng.nanfengbazi.imageparser.WenzhenPillarDateConsistencyRefiner(
                    fourPillarsLookup,
                ),
            fingerprintEngine = DHashImageFingerprintEngine(),
        )
    override val screenshotRecognitionScheduler: ScreenshotRecognitionScheduler =
        WorkManagerScreenshotRecognitionScheduler(
            context = application,
            repository = importSessionRepository,
        )
    override val backupAttachmentRoot: Path = application.filesDir.toPath().resolve("attachments")
    override val backupWorkRoot: Path = application.cacheDir.toPath().resolve("backup-work")
    override val googleSignInClient: BaziGoogleSignInClient = BaziGoogleSignInClient(
        application,
        BuildConfig.NANFENG_CLOUD_GOOGLE_SERVER_CLIENT_ID,
    )
    override val cloudSyncCoordinator: BaziCloudSyncCoordinator = BaziCloudSyncCoordinator(
        gateway = BaziSupabaseGateway(
            BaziSupabaseConfig(
                url = BuildConfig.NANFENG_CLOUD_URL,
                publishableKey = BuildConfig.NANFENG_CLOUD_PUBLISHABLE_KEY,
            ),
        ),
        credentialStore = BaziCloudCredentialStore(application),
        avatarCache = BaziGoogleAvatarCache(application),
        snapshotBridge = BaziCloudSnapshotBridge(
            backups = caseBackupService,
            attachmentRoot = backupAttachmentRoot,
            workRoot = backupWorkRoot.resolve("cloud"),
            appVersion = BuildConfig.VERSION_NAME,
        ),
        onSignedIn = { BaziCloudSyncScheduler.enable(application) },
        onSignedOut = { BaziCloudSyncScheduler.disable(application) },
        onRetryRequested = { BaziCloudSyncScheduler.enqueueRetry(application) },
    )
    override val screenshotImportCommitter: ScreenshotImportCommitter =
        ScreenshotImportCommitter(
            caseRepository = caseRepository,
            importSessionRepository = importSessionRepository,
            baziEngine = baziEngine,
            fourPillarsLookup = fourPillarsLookup,
            importImageStore = importImageStore,
            attachmentRoot = backupAttachmentRoot,
            professionalFortuneResolver =
                com.nanzhufeng.nanfengbazi.engine.tyme.TymeProfessionalFortuneResolver(),
        )

    init {
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(
                "cases",
                "calculation_snapshots",
                "text_records",
                "text_record_revisions",
                "case_events",
                "case_event_revisions",
                "case_groups",
                "case_tags",
                "case_group_cross_ref",
                "case_tag_cross_ref",
            ) {
                override fun onInvalidated(tables: Set<String>) {
                    // 这里只监听真实命例表的 Room 失效；登录恢复、设置页打开和头像刷新都不会进入。
                    // 即使前一轮同步正在运行，也要保留后续写入的延迟同步，避免快照后新增的内容漏传。
                    BaziCloudSyncScheduler.enqueueAfterLocalChange(application)
                }
            },
        )
        cloudSyncCoordinator.restorePersistedState()
        if (cloudSyncCoordinator.state.value is com.nanzhufeng.nanfengbazi.cloud.BaziCloudSyncState.Ready) {
            BaziCloudSyncScheduler.enable(application)
        }
    }

    private companion object {
        const val DATABASE_NAME = "nanfeng-bazi.db"
    }
}

class NanfengBaziApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
