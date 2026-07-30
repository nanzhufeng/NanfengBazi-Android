package com.nanzhufeng.nanfengbazi

import android.app.Application
import androidx.room.Room
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
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine
import com.nanzhufeng.nanfengbazi.imageparser.AnchorBasedWenzhenPageClassifier
import com.nanzhufeng.nanfengbazi.imageparser.DHashImageFingerprintEngine
import com.nanzhufeng.nanfengbazi.imageparser.ImportImageContentReader
import com.nanzhufeng.nanfengbazi.imageparser.ImportRecognitionCoordinator
import com.nanzhufeng.nanfengbazi.imageparser.MlKitChineseOcrEngine
import java.nio.file.Path

interface AppContainer {
    val caseRepository: CaseRepository
    val baziEngine: BaziEngine
    val caseBackupService: CaseBackupOperations
    val singleCaseBundleService: SingleCaseBundleOperations
    val importSessionRepository: ImportSessionRepository
    val importImageStore: PrivateImportImageStore
    val importRecognitionCoordinator: ImportRecognitionCoordinator
    val screenshotRecognitionScheduler: ScreenshotRecognitionScheduler
    val screenshotImportCommitter: ScreenshotImportCommitter
    val backupAttachmentRoot: Path
    val backupWorkRoot: Path
}

class DefaultAppContainer(
    application: Application,
) : AppContainer {
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
        )
        .build()

    override val caseRepository: CaseRepository = RoomCaseRepository(database)
    override val baziEngine: BaziEngine = TymeBaziEngine()
    override val caseBackupService: CaseBackupOperations = CaseBackupService(database)
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
            ocrEngine = MlKitChineseOcrEngine(),
            pageClassifier = AnchorBasedWenzhenPageClassifier(),
            fingerprintEngine = DHashImageFingerprintEngine(),
        )
    override val screenshotRecognitionScheduler: ScreenshotRecognitionScheduler =
        WorkManagerScreenshotRecognitionScheduler(
            context = application,
            repository = importSessionRepository,
        )
    override val backupAttachmentRoot: Path = application.filesDir.toPath().resolve("attachments")
    override val backupWorkRoot: Path = application.cacheDir.toPath().resolve("backup-work")
    override val screenshotImportCommitter: ScreenshotImportCommitter =
        ScreenshotImportCommitter(
            caseRepository = caseRepository,
            importSessionRepository = importSessionRepository,
            baziEngine = baziEngine,
            importImageStore = importImageStore,
            attachmentRoot = backupAttachmentRoot,
        )

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
