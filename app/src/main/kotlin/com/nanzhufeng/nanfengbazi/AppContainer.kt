package com.nanzhufeng.nanfengbazi

import android.app.Application
import androidx.room.Room
import com.nanzhufeng.nanfengbazi.data.db.DatabaseMigrations
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine

interface AppContainer {
    val caseRepository: CaseRepository
    val baziEngine: BaziEngine
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
        )
        .build()

    override val caseRepository: CaseRepository = RoomCaseRepository(database)
    override val baziEngine: BaziEngine = TymeBaziEngine()

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
