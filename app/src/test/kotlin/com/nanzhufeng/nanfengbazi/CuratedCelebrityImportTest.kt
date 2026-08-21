package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine
import java.io.File
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedCelebrityImportTest {
    @Test
    fun `资料不足或冲突的具体时刻只保留为备选不作为默认事实`() = runTest {
        val repository = FakeCaseRepository()
        val importer = CuratedCelebrityImporter(
            repository,
            BaziEngine { input, _ -> calculationResult(input) },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val source = importer.decode(File("src/main/assets/catalogs/celebrity-research-v1.json").readText())
        val uncertain = source.cases.filter { it.evidenceRating in setOf("C", "DD") }

        assertTrue(uncertain.isNotEmpty())
        assertTrue(uncertain.all { it.defaultTime.precision == TimePrecision.APPROXIMATE })
        assertTrue(uncertain.count { it.alternativeTimes.isNotEmpty() } >= 10)
    }

    @Test
    fun `内置统一资料库只有规范案例且不会写入用户库`() = runTest {
        val repository = FakeCaseRepository()
        val importer = UnifiedCelebrityCatalogImporter(
            repository,
            BaziEngine { input, _ ->
                calculationResult(input.copy(resolvedUtcOffsetSeconds = 8 * 60 * 60))
            },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val source = importer.decode(File("src/main/assets/catalogs/celebrity-unified-v1.json").readText())
        val restoredId = source.cases.first { it.restoreCaseIds.isNotEmpty() }
            .restoreCaseIds
            .first()
        val privateCase = sampleStoredCase("private-user-case")
        repository.stored[privateCase.id] = privateCase
        repository.stored[restoredId] = sampleStoredCase(restoredId).copy(
            libraryType = CaseLibraryType.CELEBRITY,
            sourceType = CaseSourceType.CURATED_CELEBRITY_CATALOG,
            deletedAt = FixedInstant,
        )

        assertEquals(574, source.cases.size)
        assertEquals(574, source.cases.map { it.caseId }.distinct().size)
        assertTrue(source.cases.all { it.sourceEvidence.isNotEmpty() })
        assertTrue(source.groups.none { it.name in setOf("文博", "传媒", "思想", "待考") })
        assertEquals(
            mapOf(
                "丁丙" to "unified-celebrity-culture",
                "樊锦诗" to "unified-celebrity-culture",
                "沈辅" to "unified-celebrity-culture",
            ),
            source.cases
                .filter { it.canonicalName in setOf("丁丙", "樊锦诗", "沈辅") }
                .associate { it.canonicalName to it.groupId },
        )
        assertTrue(
            source.cases.single { it.canonicalName == "欧阳修" }.supersededCaseIds
                .contains("3d39cbc3-c933-3e04-9f9a-afa179efd6da"),
        )
        assertEquals(
            setOf(
                "阿尔伯特·爱因斯坦",
                "迈克尔·乔丹",
                "李小龙",
                "欧阳修",
                "玛丽莲·梦露",
            ),
            source.cases.filter { it.restoreCaseIds.isNotEmpty() }
                .map { it.canonicalName }
                .toSet(),
        )
        val sourceOnlyHistorical = source.cases.filter { sourceCase ->
            sourceCase.sourceEvidence.map { it.sourceType }.toSet() ==
                setOf(CaseSourceType.WENZHEN_WEB_IMPORT)
        }
        assertEquals(326, sourceOnlyHistorical.size)
        val rulers = source.cases.filter { it.groupId == "unified-celebrity-emperor" }
        assertEquals(27, rulers.size)
        val correctedRulers = rulers.filter {
            it.birthInput.sourceNote.orEmpty().contains("君主出生日期按公开史料校正")
        }
        assertEquals(27, correctedRulers.size)
        assertTrue(correctedRulers.all { it.birthInput.timePrecision == TimePrecision.APPROXIMATE })
        assertTrue(correctedRulers.all { it.birthInput.sourceNote.orEmpty().contains("不是出生时刻") })
        assertTrue(correctedRulers.none { sourceCase ->
            val year = when (val calendar = sourceCase.birthInput.calendarInput) {
                is com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar -> calendar.dateTime.year
                is com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Lunar -> calendar.dateTime.year
            }
            year >= 2000
        })
        assertEquals(
            mapOf(
                "康熙" to Triple(1654, 3, 18),
                "嘉靖" to Triple(1507, 8, 10),
                "朱元璋" to Triple(1328, 9, 18),
                "顺治" to Triple(1638, 1, 30),
            ),
            correctedRulers.filter { it.canonicalName in setOf("康熙", "嘉靖", "朱元璋", "顺治") }
                .associate { sourceCase ->
                    val date = (sourceCase.birthInput.calendarInput as com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Lunar).dateTime
                    sourceCase.canonicalName to Triple(date.year, date.month, date.day)
                },
        )
        val guangwu = correctedRulers.single { it.canonicalName == "汉世祖光武帝" }
        val guangwuDate = (guangwu.birthInput.calendarInput as
            com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar).dateTime
        assertEquals(Triple(-4, 1, 15), Triple(guangwuDate.year, guangwuDate.month, guangwuDate.day))
        assertTrue(guangwu.birthInput.sourceNote.orEmpty().contains("公元前5年1月15日"))
        assertTrue(sourceOnlyHistorical.all { sourceCase ->
            sourceCase.birthInput.timePrecision == TimePrecision.DOUBLE_HOUR_ONLY ||
                sourceCase.birthInput.sourceNote.orEmpty().contains("君主出生日期按公开史料校正")
        })
        assertTrue(correctedRulers.filter { it.birthTimeCandidates.isNotEmpty() }.all { sourceCase ->
            sourceCase.birthTimeCandidates.any { it.adopted && it.birthInput == sourceCase.birthInput }
        })

        val result = importer.synchronize(source)

        assertEquals(574, result.created)
        assertEquals(1, repository.findByIdsRequests.size)
        assertEquals(
            source.cases.flatMap { sourceCase ->
                listOf(sourceCase.caseId) + sourceCase.supersededCaseIds + sourceCase.restoreCaseIds
            }.toSet().size,
            repository.findByIdsRequests.single().size,
        )
        assertTrue(repository.findByIdRequests.isEmpty())
        assertTrue(result.errors.joinToString(separator = "\n"), result.invalid == 0)
        assertEquals(0, result.archivedDuplicates)
        assertEquals(1, result.restoredFromTrash)
        assertEquals(privateCase, repository.stored.getValue(privateCase.id))
        assertEquals(null, repository.stored.getValue(restoredId).deletedAt)
        assertEquals(1, repository.stored.values.count { it.libraryType == CaseLibraryType.USER })
        assertEquals(575, repository.stored.values.count { it.libraryType == CaseLibraryType.CELEBRITY })
        assertEquals(10, repository.groupCatalog.count { it.libraryType == CaseLibraryType.CELEBRITY })

        val repeat = importer.synchronize(source)
        assertEquals(0, repeat.created)
        assertEquals(0, repeat.updated)
        assertEquals(574, repeat.skipped)
        assertEquals(0, repeat.invalid)
    }

    @Test
    fun `统一资料包预检失败时不写入半套名人目录`() = runTest {
        val repository = FakeCaseRepository().apply {
            beforeFindByIds = { error("模拟资料批量读取失败") }
        }
        val importer = UnifiedCelebrityCatalogImporter(
            repository,
            BaziEngine { input, _ -> calculationResult(input) },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val source = importer.decode(File("src/main/assets/catalogs/celebrity-unified-v1.json").readText())

        val result = importer.synchronize(source)

        assertEquals(1, result.invalid)
        assertEquals(0, result.created)
        assertEquals(0, repository.stored.size)
        assertTrue(result.errors.single().contains("模拟资料批量读取失败"))
    }

    @Test
    fun `统一资料包更新保留用户填写的名人笔记与关键事件`() = runTest {
        val repository = FakeCaseRepository()
        val importer = UnifiedCelebrityCatalogImporter(
            repository,
            BaziEngine { input, _ -> calculationResult(input) },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val source = importer.decode(File("src/main/assets/catalogs/celebrity-unified-v1.json").readText())
        val target = source.cases.first { it.curatedCase != null }

        importer.synchronize(source)
        val existing = repository.stored.getValue(target.caseId)
        val userNote = CaseTextRecord(
            id = "user-note",
            type = CaseTextRecordType.OWNER_FEEDBACK,
            content = "这是用户自己补充的断事笔记。",
            sourceType = TextRecordSourceType.USER,
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val userEvent = CaseEvent(
            id = "user-event",
            rawText = "这是用户自己补充的关键事件。",
            createdAt = FixedInstant,
        )
        repository.stored[target.caseId] = existing.copy(
            textRecords = existing.textRecords + userNote,
            events = existing.events + userEvent,
            revision = existing.revision + 1,
        )

        val result = importer.synchronize(source)
        val updated = repository.stored.getValue(target.caseId)

        assertEquals(0, result.invalid)
        assertTrue(updated.textRecords.any { it.id == userNote.id && it.content == userNote.content })
        assertTrue(updated.events.any { it.id == userEvent.id && it.rawText == userEvent.rawText })
    }

    @Test
    fun `完整统一资料包可由正式排盘引擎预检并同步`() = runTest {
        val repository = FakeCaseRepository()
        val importer = UnifiedCelebrityCatalogImporter(
            repository,
            TymeBaziEngine(),
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val source = importer.decode(File("src/main/assets/catalogs/celebrity-unified-v1.json").readText())

        val result = importer.synchronize(source)

        assertTrue(result.errors.joinToString(separator = "\n"), result.invalid == 0)
        assertEquals(574, result.created)
        assertEquals(574, repository.stored.values.count { it.libraryType == CaseLibraryType.CELEBRITY })
    }

    @Test
    fun `内置首批资料包可完整解码并保留候选时刻`() = runTest {
        val repository = FakeCaseRepository()
        val importer = CuratedCelebrityImporter(
            repository,
            BaziEngine { input, _ ->
                calculationResult(input.copy(resolvedUtcOffsetSeconds = 8 * 60 * 60))
            },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val raw = File("src/main/assets/catalogs/celebrity-research-v1.json").readText()
        val source = importer.decode(raw)

        assertEquals(249, source.cases.size)
        assertEquals(249, importer.import(source).created)
        assertEquals(0, repository.stored.values.count { it.birthTimeCandidates.isEmpty() })
        assertTrue(
            repository.stored.values.single { it.alias == "斯蒂芬·霍金" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("仅有日期"),
        )
        assertEquals(
            4,
            repository.stored.values.single { it.alias == "泰勒·斯威夫特" }.birthTimeCandidates.size,
        )
        assertEquals(
            TimePrecision.APPROXIMATE,
            repository.stored.values.single { it.alias == "泰勒·斯威夫特" }.birthInput.timePrecision,
        )
        assertTrue(
            repository.stored.values.single { it.alias == "成龙" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("区间中点"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "习近平" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("时刻未知"),
        )
        assertEquals(
            3,
            repository.stored.values.single { it.alias == "克里斯蒂亚诺·罗纳尔多" }.birthTimeCandidates.size,
        )
        assertTrue(
            repository.stored.values.single { it.alias == "Lady Gaga" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("待核（来源冲突或未验证）"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "杰夫·贝索斯" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("仅有生日（时刻未证实）"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "莫言" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("诺贝尔奖官网日期：1956-03-25"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "刘德华" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("1961-09-27"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "张学友" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("音乐剧"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "周冬雨" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("少年的你"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "张艺兴" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("音乐制作人"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "檀健次" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("猎罪图鉴"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "张译" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("士兵突击"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "宋庆龄" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("1949"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "周恩来" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("外交家"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "邓颖超" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("妇女"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "董必武" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("法律"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "李先念" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("经济工作者"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "李维汉" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("统战"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "张謇" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("卯时（05:00–07:00）"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "沈钧儒" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("法学教育家"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "何香凝" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("妇女运动"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "但斌" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("东方港湾"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "本杰明·格雷厄姆" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("1894-05-09"),
        )
        val chowFeedback = repository.stored.values.single { it.alias == "周星驰" }
            .textRecords
            .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
            .content
        assertTrue(chowFeedback.contains("暂定午时（排盘占位）"))
        assertTrue(chowFeedback.contains("子时、丑时、寅时、卯时、辰时、巳时、未时、申时、酉时、戌时、亥时"))
        assertTrue(!chowFeedback.contains("候选：未知时刻占位"))
        assertTrue(
            repository.stored.values.single { it.alias == "余华" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("本人自述中午"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "苏炳添" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("子时、丑时、寅时、卯时、辰时、巳时、未时、申时、酉时、戌时、亥时"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "巩俐" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("《秋菊打官司》"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "周鸿祎" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("360集团"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "钱学森" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("工程控制论"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "林巧稚" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("妇产科学"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "李政道" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("公开异说：1926-11-24"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "钟南山" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("呼吸病学"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "吴孟超" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("来源记载黎明"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "李四光" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("地质力学"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "钱三强" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("暂定午时（排盘占位）"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "王选" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("激光照排"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "邓小平" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("广安市人民政府"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "江泽民" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("工程技术背景"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "朱镕基" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("中国政府网"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "玛丽·居里" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("两获诺贝尔奖"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "伊丽莎白二世" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("官方记载 02:40"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "贝利" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("三次夺得世界杯冠军"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "方济各" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("梵蒂冈官网"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "马拉拉·优素福扎伊" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("教育权倡导者"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "加夫列尔·加西亚·马尔克斯" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("魔幻现实主义"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "托妮·莫里森" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("诺贝尔奖官网"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "旺加里·马塔伊" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("绿带运动"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "萨姆·奥尔特曼" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("本人邮件称约凌晨05:00"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "黄仁勋" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("GPU"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "王传福" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("公开异说：1966-04-08"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "德米斯·哈萨比斯" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("AlphaFold"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "苏姿丰" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("Munzinger人物资料库"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "吉野彰" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("锂离子电池"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "林纳斯·托瓦兹" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("Git"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "马化腾" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("即时通信"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "孙正义" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("ARM"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "詹姆斯·高斯林" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("Java"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "凯瑟琳·约翰逊" }
                .textRecords
                .single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
                .content
                .contains("NASA"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "蒂姆·伯纳斯-李" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("万维网"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "戈登·摩尔" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("摩尔定律"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "丹·布里克林" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("VisiCalc"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "袁征" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("Zoom"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "何小鹏" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("智能电动车"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "王小川" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("大模型"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "陈景润" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("陈氏定理"),
        )
        assertTrue(
            repository.stored.values.single { it.alias == "顾方舟" }
                .textRecords
                .single { it.type == CaseTextRecordType.MASTER_COMMENTARY }
                .content
                .contains("脊髓灰质炎疫苗"),
        )
    }

    @Test
    fun `可考名人包保留来源和候选时刻并稳定去重`() = runTest {
        val repository = FakeCaseRepository()
        val importer = CuratedCelebrityImporter(
            repository,
            BaziEngine { input, _ ->
                calculationResult(input.copy(resolvedUtcOffsetSeconds = 8 * 60 * 60))
            },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val data = CuratedCelebrityCatalogPackage(
            format = CuratedCelebrityCatalogPackage.FORMAT,
            version = CuratedCelebrityCatalogPackage.VERSION,
            catalogVersion = "test",
            compiledAt = "2026-08-20",
            cases = listOf(
                CuratedCelebrityCase(
                    sourceId = "candidate-case",
                    name = "合成可考名人",
                    sex = "M",
                    groupName = "科学",
                    tags = listOf("测试", "候选时辰"),
                    birthPlace = "测试市",
                    timeZoneId = "Asia/Shanghai",
                    longitude = 116.4,
                    latitude = 39.9,
                    birthDate = CuratedDate(2000, 2, 29),
                    alternativeBirthDates = listOf(
                        CuratedDateCandidate(
                            label = "公开异说",
                            date = CuratedDate(2000, 2, 28),
                            rationale = "另一份公开资料采用该日期，不能与默认日期混为一谈。",
                            sourceUrl = "https://example.com/alternative-date",
                        ),
                    ),
                    defaultTime = CuratedTimeCandidate(
                        label = "默认：来源记录 10:30",
                        hour = 10,
                        minute = 30,
                        precision = TimePrecision.EXACT_TO_MINUTE,
                        rationale = "正式登记记录。",
                    ),
                    alternativeTimes = listOf(
                        CuratedTimeCandidate(
                            label = "备选：传记约 09:00",
                            hour = 9,
                            minute = 0,
                            precision = TimePrecision.APPROXIMATE,
                            rationale = "传记仅称上午九时左右。",
                        ),
                    ),
                    evidenceRating = "AA",
                    sources = listOf(
                        CuratedSourceReference(
                            title = "测试来源",
                            url = "https://example.com/source",
                            publisher = "测试出版方",
                            accessedAt = "2026-08-20",
                            claim = "出生时刻与地点。",
                        ),
                    ),
                    editorialCommentary = "仅作资料编审摘要。",
                ),
            ),
        )

        assertEquals(1, importer.preview(data).alternativeTimeCaseCount)
        assertEquals(1, importer.import(data).created)
        assertEquals(1, importer.import(data).skipped)

        val actual = repository.stored.values.single()
        assertEquals(CaseSourceType.CURATED_CELEBRITY_CATALOG, actual.sourceType)
        assertEquals(2, actual.birthTimeCandidates.size)
        assertEquals(1, actual.birthTimeCandidates.count { it.adopted })
        assertEquals(2, actual.calculationSnapshots.size)
        assertTrue(actual.calculationSnapshots.single { it.adopted }.result.normalizedInput == actual.birthInput)
        assertEquals(8 * 60 * 60, actual.birthInput.resolvedUtcOffsetSeconds)
        assertTrue(actual.birthInput.sourceNote.orEmpty().contains("https://example.com/source"))
        val feedback = actual.textRecords.single { it.type == CaseTextRecordType.OWNER_FEEDBACK }
        assertEquals(TextRecordSourceType.CURATED_RESEARCH, feedback.sourceType)
        assertTrue(feedback.content.contains("其他可能时辰"))
        assertTrue(feedback.content.contains("日期说明"))
        assertTrue(feedback.content.contains("https://example.com/alternative-date"))
        assertTrue(feedback.content.contains("https://example.com/source"))
        assertTrue(actual.textRecords.any { it.type == CaseTextRecordType.MASTER_COMMENTARY })
    }

    @Test
    fun `导入可考资料包不会覆盖同名问真网页名人`() = runTest {
        val original = sampleStoredCase("wenzhen-liu-dehua").copy(
            alias = "刘德华",
            libraryType = CaseLibraryType.CELEBRITY,
            sourceType = CaseSourceType.WENZHEN_WEB_IMPORT,
            revision = 8,
        )
        val repository = FakeCaseRepository().apply { stored[original.id] = original }
        val importer = CuratedCelebrityImporter(
            repository,
            BaziEngine { input, _ -> calculationResult(input.copy(resolvedUtcOffsetSeconds = 8 * 60 * 60)) },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val catalog = importer.decode(File("src/main/assets/catalogs/celebrity-research-v1.json").readText())
            .copy(cases = listOf(catalogCase("刘德华")))

        val result = importer.import(catalog)

        assertEquals(1, result.created)
        assertEquals(original, repository.stored.getValue(original.id))
        assertEquals(
            2,
            repository.stored.values.count { it.alias == "刘德华" && it.libraryType == CaseLibraryType.CELEBRITY },
        )
    }

    @Test
    fun `内置资料同步只更新受管理名人并保留用户案例`() = runTest {
        val userCase = sampleStoredCase("user-private-case")
        val repository = FakeCaseRepository().apply { stored[userCase.id] = userCase }
        val importer = CuratedCelebrityImporter(
            repository,
            BaziEngine { input, _ -> calculationResult(input.copy(resolvedUtcOffsetSeconds = 8 * 60 * 60)) },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val catalog = importer.decode(File("src/main/assets/catalogs/celebrity-research-v1.json").readText())
            .copy(cases = listOf(catalogCase("刘德华")))

        assertEquals(1, importer.import(catalog).created)
        val managed = repository.stored.values.single { it.alias == "刘德华" }.copy(
            alias = "旧资料名称",
            isFavorite = true,
            isPinned = true,
            deletedAt = FixedInstant,
        )
        repository.stored[managed.id] = managed

        val result = importer.synchronizeBuiltInCatalog(catalog)

        assertEquals(0, result.created)
        assertEquals(1, result.updated)
        assertEquals(0, result.invalid)
        assertEquals(userCase, repository.stored.getValue(userCase.id))
        val refreshed = repository.stored.getValue(managed.id)
        assertEquals("刘德华", refreshed.alias)
        assertTrue(refreshed.isFavorite)
        assertTrue(refreshed.isPinned)
        assertEquals(FixedInstant, refreshed.deletedAt)
        assertTrue(refreshed.revision > managed.revision)
    }

    private fun catalogCase(name: String): CuratedCelebrityCase =
        CuratedCelebrityImporter(
            FakeCaseRepository(),
            BaziEngine { input, _ -> calculationResult(input.copy(resolvedUtcOffsetSeconds = 8 * 60 * 60)) },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
            .decode(File("src/main/assets/catalogs/celebrity-research-v1.json").readText())
            .cases
            .single { it.name == name }
}
