package com.nanzhufeng.nanfengbazi

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionShapeContractTest {
    @Test
    fun notesIdentityHeaderCentersBalancedNameAndSexAroundThePillars() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val notesHeader = source.substringAfter("CaseDetailSection.RECORDS -> {")
            .substringBefore("HorizontalDivider(\n                        modifier = Modifier.padding(top = 1.dp")

        assertTrue(notesHeader.contains("case.name.value ?: case.alias"))
        assertTrue(notesHeader.contains(".testTag(\"notes_identity_name\")"))
        assertTrue(notesHeader.contains("fontSize = 15.sp"))
        assertTrue(notesHeader.contains(".testTag(\"notes_identity_sex\")"))
        assertTrue(notesHeader.contains("case.sexForFortuneDirection.displayName()"))
        assertTrue(notesHeader.contains("fontSize = 14.sp"))
        assertTrue(notesHeader.contains("Spacer(modifier = Modifier.width(136.dp))"))
        assertTrue(notesHeader.contains("contentAlignment = Alignment.Center"))
        assertTrue(!notesHeader.contains(".align(Alignment.CenterStart)"))
        assertTrue(!notesHeader.contains(".align(Alignment.CenterEnd)"))
        assertTrue(!notesHeader.contains("notes_identity_chart_type"))
    }

    @Test
    fun caseNotesOpenUrlsInsideTheirOriginalInputWithoutRenderingDuplicates() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val editor = source.substringAfter("private fun CaseNotesTextEditor(")
            .substringBefore("private fun CaseNotesEditorScrollbar(")
        val timeline = source.substringAfter("private fun CaseNotesTimelineInput(")
            .substringBefore("private fun CaseNotesTimePicker(")

        assertTrue(source.contains("private fun Modifier.openCaseNotesUrlWhenTapped("))
        assertTrue(source.contains("caseNotesWebUrlAtOffset(value"))
        assertTrue(source.contains("uriHandler.openUri(url)"))
        assertTrue(editor.contains("BasicTextField("))
        assertTrue(editor.contains("openCaseNotesUrlWhenTapped("))
        assertTrue(editor.contains("caseNotesWebUrlVisualTransformation("))
        assertTrue(!editor.contains("CaseNotesWebLinks(value)"))
        assertTrue(timeline.contains("CaseNotesTextEditor("))
        assertTrue(!timeline.contains("CaseNotesWebLinks(entry.content)"))
    }

    @Test
    fun trashListDoesNotExposeGroupFilteringOrManagement() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val list = source.substringAfter("private fun CaseListScreen(")
            .substringBefore("private fun CaseSummaryCard(")

        assertTrue(
            list.contains(
                "if (!isCompatibilitySelection && state.visibility != CaseVisibility.TRASHED) {\n            Row(",
            ),
        )
        assertTrue(list.contains("record_filter_strip_placeholder"))
        assertTrue(list.contains(".height(46.dp)"))
        assertTrue(list.contains("if (state.visibility != CaseVisibility.TRASHED) {\n                            NanfengOverflowMenuItem("))
    }

    @Test
    fun birthCalendarPickerKeepsQuickLocateAndFixedSheetHeight() {
        val pickers = File(locateSourceRoot(), "BirthInputPickers.kt").readText()
        val birthPicker = pickers.substringAfter("internal fun BirthDateTimePickerSheet(")
            .substringBefore("private fun BirthPickerHeader(")

        assertTrue(birthPicker.contains("BirthPickerQuickLocateInput("))
        assertTrue(birthPicker.contains("LaunchedEffect(mode, quickLocateText)"))
        assertTrue(birthPicker.contains(".height(BirthDateTimeWheelViewportHeight)"))
        assertTrue(pickers.contains("tag: String = \"birth_quick_locate_input\""))
        assertTrue(pickers.contains(".testTag(tag)"))
        assertTrue(pickers.contains("targetHeight = BirthPickerPanelHeight"))
    }

    @Test
    fun singleCaseExportKeepsTheDefaultJsonPathMinimalAndPasswordFlowCanReturn() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val viewModel = File(locateSourceRoot(), "StageTwoViewModel.kt").readText()
        val activity = File(locateSourceRoot(), "MainActivity.kt").readText()
        val exportDialog = source.substringAfter("if (state.singleCaseExportConfirmationVisible)")
            .substringBefore("if (state.singleCasePasswordExportVisible)")
        val passwordDialog = source.substringAfter("if (state.singleCasePasswordExportVisible)")
            .substringBefore("if (state.singleCasePasswordImportVisible)")
        val exportRequest = viewModel.substringAfter("fun requestSingleCaseExport()")
            .substringBefore("fun chooseSingleCaseExportAttachments")

        assertTrue(exportDialog.contains("导出当前命例"))
        assertTrue(exportDialog.contains("默认导出 JSON"))
        assertTrue(exportDialog.contains("文件含敏感命例资料，请妥善保存"))
        assertTrue(exportDialog.contains("Text(\"保存\")"))
        assertTrue(exportDialog.contains("single_case_export_with_attachments"))
        assertTrue(!exportDialog.contains("SelectionButton("))
        assertTrue(!exportDialog.contains("选择保存位置"))
        assertTrue(passwordDialog.contains("title = \"导出密码\""))
        assertTrue(passwordDialog.contains("输入至少 6 位密码，请妥善保管"))
        assertTrue(passwordDialog.contains("requireConfirmation = false"))
        assertTrue(passwordDialog.contains("dismissLabel = \"返回\""))
        assertTrue(!passwordDialog.contains("再次输入密码"))
        assertTrue(viewModel.contains("singleCaseExportConfirmationVisible = true"))
        assertTrue(exportRequest.contains("singleCaseExportIncludesAttachments = false"))
        assertTrue(viewModel.contains("MIN_EXPORT_PASSWORD_LENGTH = 6"))
        assertTrue(!source.contains("导出单命例"))
        assertTrue(activity.contains("when (request.kind)"))
        assertTrue(activity.contains("SingleCaseExportDocumentKind.ENCRYPTED_JSON"))
        assertTrue(!activity.contains("fileName.endsWith(\".json\")"))
    }

    @Test
    fun rootNavigationFeedbackUsesTheFullIconAndLabelSurface() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val navigation = source.substringAfter("private fun RootNavigationBar(")
            .substringBefore("private fun RootNavigationRail(")

        assertTrue(navigation.contains("Surface("))
        assertTrue(navigation.contains("Column("))
        assertTrue(navigation.contains("item.label"))
        assertTrue(navigation.contains("modifier: Modifier = Modifier"))
        assertTrue(navigation.contains(".height(ROOT_NAVIGATION_BAR_HEIGHT)"))
        assertTrue(navigation.contains(".height(58.dp)"))
        assertTrue(navigation.contains("val navigationShape = RoundedCornerShape(26.dp)"))
        assertTrue(navigation.contains("RoundedCornerShape(26.dp)"))
        assertTrue(navigation.contains("RoundedCornerShape(20.dp)"))
        assertTrue(navigation.contains("MaterialTheme.colorScheme.primaryContainer"))
        assertTrue(navigation.contains("MaterialTheme.colorScheme.primary"))
        assertTrue(!navigation.contains("NanfengWarmTint"))
        assertTrue(!navigation.contains("NanfengGold.copy(alpha = 0.26f)"))
        assertTrue(navigation.contains("ambientShadowColor = Color.Transparent"))
        assertTrue(navigation.contains("spotShadowColor = NanfengInk.copy(alpha = 0.30f)"))
        assertTrue(navigation.contains("shadowElevation = 12.dp.toPx()"))
        assertTrue(navigation.contains("shadowElevation = 0.dp"))
        assertTrue(!navigation.contains("NavigationBar("))
    }

    @Test
    fun homeQuickEntriesUseIndependentInnerAndOuterScreenHeights() {
        val screens = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val almanac = File(locateSourceRoot(), "AlmanacFeature.kt").readText()
        val create = screens.substringAfter("private fun WenzhenCreateCaseScreen(")
            .substringBefore("private fun BaziCompatibilityHomeEntry(")
        val compatibility = screens.substringAfter("private fun BaziCompatibilityHomeEntry(")
            .substringBefore("private fun HomeChoiceGroup(")

        assertTrue(create.contains("BoxWithConstraints("))
        assertTrue(create.contains("val homeQuickEntryEdgeGap = 10.dp"))
        assertTrue(create.contains("val quickEntryNavigationBottomInset"))
        assertTrue(create.contains("ROOT_NAVIGATION_BAR_HEIGHT"))
        assertTrue(create.contains("ROOT_NAVIGATION_BOTTOM_MARGIN"))
        assertFalse(create.contains("navigationBarInset"))
        assertTrue(create.contains(".padding(top = contentVerticalPadding)"))
        assertFalse(create.contains("QuickEntrySlotOffset"))
        assertFalse(create.contains("innerQuickEntryTopOffset"))
        assertTrue(create.contains("Spacer(modifier = Modifier.height(homeQuickEntryEdgeGap))"))
        assertTrue(create.contains(".weight(1f)"))
        assertTrue(create.contains(".fillMaxHeight()"))
        assertTrue(create.contains("BaziCompatibilityHomeEntry("))
        assertTrue(create.contains("onClick = onOpenBaziCompatibility"))
        assertTrue(create.contains("AlmanacHomeEntry("))
        assertTrue(create.contains("onClick = onOpenAlmanac"))
        assertTrue(create.contains("maxWidth >= INNER_DISPLAY_HOME_MIN_WIDTH"))
        assertTrue(!create.contains("maxHeight < 780.dp"))
        assertTrue(screens.contains("private val INNER_DISPLAY_HOME_MIN_WIDTH = 600.dp"))
        assertTrue(screens.contains("private val ROOT_NAVIGATION_BAR_HEIGHT = 68.dp"))
        assertTrue(screens.contains("private val ROOT_NAVIGATION_BOTTOM_MARGIN = 8.dp"))
        assertTrue(screens.contains("private val ROOT_NAVIGATION_CONTENT_GAP = 10.dp"))
        assertTrue(!create.contains("topContentHeightPx"))
        assertTrue(!create.contains("quickEntryReserve"))
        assertTrue(!create.contains("minQuickEntryHeight"))
        assertFalse(create.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(create.contains(".weight(1f)"))
        assertTrue(create.contains("height = if (compactHomeLayout) 132.dp else 148.dp"))
        assertTrue(create.contains("val pickerRowHeight = if (compactHomeLayout) 60.dp else 68.dp"))
        assertTrue(compatibility.contains(".fillMaxHeight()"))
        assertTrue(compatibility.contains("compact: Boolean"))
        assertTrue(compatibility.contains("contentDescription = \"打开八字合盘\""))
        assertTrue(compatibility.contains("painterResource(R.drawable.ic_bazi_compatibility)"))
        assertTrue(compatibility.contains("shape = HomeQuickEntryPillShape"))
        assertTrue(!compatibility.contains("Icons.Filled.Share"))
        assertTrue(almanac.contains(".fillMaxHeight()"))
        assertTrue(almanac.contains("compact: Boolean"))
        assertTrue(almanac.contains("shape = HomeQuickEntryPillShape"))
        assertTrue(almanac.contains("modifier = Modifier.fillMaxSize(),\n            contentAlignment = Alignment.Center"))
        assertTrue(screens.contains("internal val HomeQuickEntryPillShape = RoundedCornerShape(percent = 50)"))
    }

    @Test
    fun compatibilityParticipantSelectionReusesTheSearchableRecordList() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val compatibility = source.substringAfter("private fun BaziCompatibilityScreen(")
            .substringBefore("private fun BaziCompatibilityHistoryList(")
        val caseList = source.substringAfter("private fun CaseListScreen(")
            .substringBefore("private fun CaseSummaryCard(")

        assertTrue(compatibility.contains("onOpenParticipantList"))
        assertTrue(!source.contains("CompatibilityParticipantPickerDialog"))
        assertTrue(caseList.contains("compatibilitySelectionRole: SexForFortuneDirection?"))
        assertTrue(caseList.contains("onSelectCompatibilityCase"))
        assertTrue(caseList.contains("选择\${compatibilityRoleLabel}八字"))
        assertTrue(caseList.contains(".testTag(\"case_search\")"))
        assertTrue(caseList.contains("if (isCompatibilitySelection) {\n                            CaseSummaryRow("))
        assertTrue(caseList.contains("if (!isCompatibilitySelection) Surface("))
    }

    @Test
    fun compatibilityResultUsesASeparateVisualComparisonScreen() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val viewModel = File(locateSourceRoot(), "StageTwoViewModel.kt").readText()
        val resultScreen = source.substringAfter("private fun BaziCompatibilityReportScreen(")
            .substringBefore("private fun CompatibilitySetupPanel(")
        val report = source.substringAfter("private fun BaziCompatibilityReportContent(")
            .substringBefore("private fun FourPillars.compatibilityElementCounts()")

        assertTrue(viewModel.contains("data object BaziCompatibilityReport"))
        assertTrue(viewModel.contains("navigator.openBaziCompatibilityReport()"))
        assertTrue(resultScreen.contains("bazi_compatibility_report_screen"))
        assertTrue(resultScreen.contains("onReplaceParticipant"))
        val compatibilityScreen = source.substringAfter("private fun BaziCompatibilityScreen(")
            .substringBefore("private fun BaziCompatibilityReportScreen(")
        assertTrue(compatibilityScreen.contains("onReplaceParticipant = onOpenParticipantList"))
        assertTrue(report.contains("CompatibilitySideBySideChart(report, onReplaceParticipant)"))
        assertTrue(report.contains("CompatibilityImportantParameterTable(report)"))
        assertTrue(report.contains("CompatibilityElementVisualization(report)"))
        assertFalse(report.contains("CompatibilityRelationshipVisualization(report)"))
        assertTrue(report.contains("compatibility_element_balance"))
        assertTrue(report.contains("CompatibilityElementBalanceRow("))
        assertTrue(report.contains("CompatibilitySideBySideChart(report, onReplaceParticipant)"))
        assertTrue(report.contains("CompatibilityRelationshipSummary(report)"))
        assertTrue(report.indexOf("CompatibilityRelationshipSummary(report)") < report.indexOf("CompatibilityImportantParameterTable(report)"))
        assertTrue(report.indexOf("CompatibilityElementVisualization(report)") < report.indexOf("CompatibilityImportantParameterTable(report)"))
        assertTrue(report.contains("CompatibilityRelationshipTable("))
        assertTrue(report.contains("CompatibilityAiPromptSection(report = report,"))
        assertTrue(!report.contains("CompatibilityPillarComparison(report)"))
        assertTrue(!report.contains("CompatibilityReportFootnote"))
        assertTrue(!source.contains("未见直接作用"))
        assertTrue(source.contains("compatibilityBranchElementRelation"))
        assertTrue(source.contains("男方生女方"))
        assertTrue(source.contains("女方生男方"))
        assertTrue(source.contains("男方克女方"))
        assertTrue(source.contains("女方克男方"))
        assertTrue(report.contains("五行结构"))
        assertTrue(report.contains("对比项"))
        assertTrue(report.contains("\"相处优势\""))
        assertTrue(report.contains("\"相处提醒\""))
        assertTrue(report.contains("\"解析\""))
        assertTrue(source.contains(".groupBy { it.kind to it.values }"))
        assertTrue(source.contains("compatibilityEvidence"))
        assertTrue(source.contains("CompatibilityParticipantHeader("))
        val participantHeader = source.substringAfter("private fun CompatibilityParticipantHeader(")
            .substringBefore("private fun CompatibilityBasicChartGrid(")
        assertTrue(participantHeader.contains("background(Color(0xFF1F1D19))"))
        assertTrue(participantHeader.contains(".fillMaxHeight()"))
        assertTrue(source.contains(".height(IntrinsicSize.Min)\n                    .background(Color(0xFF1F1D19))"))
        assertTrue(participantHeader.contains("Icons.Filled.SwapHoriz"))
        assertTrue(participantHeader.contains("contentDescription = \"更换\${role}八字\""))
        assertTrue(participantHeader.contains("LocalMinimumInteractiveComponentEnforcement provides false"))
        assertTrue(participantHeader.contains(".size(36.dp)"))
        assertTrue(participantHeader.contains("modifier = Modifier.size(20.dp)"))
        assertTrue(participantHeader.contains(".clip(CircleShape)"))
        assertTrue(participantHeader.contains("阳历："))
        assertTrue(participantHeader.contains("农历："))
        assertTrue(participantHeader.contains("maxLines = 2"))
        assertTrue(!participantHeader.contains("overflow = TextOverflow.Ellipsis"))
        assertTrue(!participantHeader.contains("Text(\"换例\""))
        assertTrue(source.contains("CompatibilityPairedDecadeTimeline(report.left, report.right)"))
        val pairedDecades = source.substringAfter("private fun CompatibilityPairedDecadeTimeline(")
            .substringBefore("private fun CompatibilityImportantParameterTable(")
        assertTrue(pairedDecades.contains("val pairedSteps = (0 until maxOf(left.decadeFortunes.size, right.decadeFortunes.size))"))
        assertTrue(pairedDecades.contains("CompatibilityPairedDecadeColumn("))
        assertTrue(pairedDecades.contains("CompatibilityDecadeCell(left)"))
        assertTrue(pairedDecades.contains("CompatibilityDecadeCell(right)"))
        assertTrue(pairedDecades.contains(".height(66.dp)"))
        assertTrue(pairedDecades.contains("textAlign = TextAlign.Center"))
        assertTrue(!pairedDecades.contains("CompatibilityDecadeTimelineRow"))
        val basicChartGrid = source.substringAfter("private fun CompatibilityBasicChartGrid(")
            .substringBefore("private fun CompatibilityBasicGridRow(")
        assertTrue(basicChartGrid.contains("CompatibilityBasicHiddenStemGridRow("))
        assertTrue(basicChartGrid.contains("compatibilityHiddenStemEntries()"))
        assertTrue(basicChartGrid.contains("split('·', '\\n')"))
        assertTrue(basicChartGrid.contains("compatibility_basic_chart_hidden_stems"))
        assertTrue(basicChartGrid.contains("compatibilityTenGodColor"))
        assertTrue(source.contains("decade?.stemTenGod"))
        assertFalse(source.contains("private fun CompatibilityRelationshipVisualization("))
        assertFalse(source.contains("private fun CompatibilityRelationshipMatrix("))
        assertFalse(source.contains("亲密与家庭关系"))
        assertTrue(!source.contains("CompatibilityCoreRelationDiagram"))
        assertTrue(!source.contains("CompatibilitySignalDistributionRow"))
        assertTrue(source.contains("Text(\"双方关系总结\""))
        assertTrue(source.contains("label = \"关系判断\""))
        assertTrue(source.contains("label = \"优势\""))
        assertTrue(source.contains("label = \"需要留意\""))
        assertTrue(source.contains("label = \"相处建议\""))
        assertTrue(source.contains("label = \"资料提醒\""))
        assertTrue(source.contains("val summary = report.relationshipSummary"))
        assertFalse(source.contains("这段关系有彼此吸引、互相带动的基础"))

        val importantParameters = source.substringAfter("private fun CompatibilityImportantParameterTable(")
            .substringBefore("private fun CompatibilityElementVisualization(")
        assertTrue(!importantParameters.contains("listOf(\"公历出生\""))
        assertTrue(!importantParameters.contains("listOf(\"农历出生\""))
        assertTrue(!importantParameters.contains("listOf(\"时刻精度\""))
        assertTrue(importantParameters.contains("双方年支（生肖）"))
        assertTrue(importantParameters.contains("双方夫妻宫（日支）"))
        assertTrue(importantParameters.contains("\${report.left.alias}夫妻宫 ↔ \${report.right.alias}年支"))
        assertTrue(importantParameters.contains("\${report.right.alias}夫妻宫 ↔ \${report.left.alias}年支"))
        assertTrue(
            importantParameters.indexOf("双方年支（生肖）") <
                importantParameters.indexOf("双方夫妻宫（日支）"),
        )
        assertEquals(3, importantParameters.split("columnWeights = CompatibilityReportColumnWeights").size - 1)
        assertTrue(importantParameters.contains("表层五行缺失"))
        assertTrue(importantParameters.contains("日主旺衰（候选）"))
        assertTrue(importantParameters.contains("格局（候选）"))
        assertTrue(importantParameters.contains("compatibilityStrengthEvidence"))
        assertTrue(importantParameters.contains("compatibilityPatternEvidence"))
        assertTrue(!importantParameters.contains("格局（来源）"))
        assertTrue(importantParameters.contains("compatibilityPairImpact("))
        assertTrue(importantParameters.contains("title = \"核心关系\""))
        assertTrue(importantParameters.contains("title = \"家庭互动\""))
        assertTrue(importantParameters.contains("title = \"命盘结构\""))
        assertTrue(importantParameters.contains("headers = listOf(\"对比项\", report.left.alias, report.right.alias, \"解析\")"))
        assertFalse(importantParameters.contains("\"男方\", \"女方\""))

        val comparisonTable = source.substringAfter("private fun CompatibilityComparisonTable(")
            .substringBefore("private fun CompatibilityTableHeader(")
        assertTrue(comparisonTable.contains(".compatibilityTableSideBorders()"))
        assertTrue(!comparisonTable.contains(".border(0.5.dp, Color(0xFFDCDCD8))"))

        val relationshipTable = source.substringAfter("private fun CompatibilityRelationshipTable(")
            .substringBefore("private data class CompatibilityRelationshipGroup(")
        assertTrue(relationshipTable.contains(".compatibilityTableSideBorders()"))
        assertTrue(relationshipTable.contains("headers = listOf(\"对比项\", leftAlias, rightAlias, \"解析\")"))
        assertEquals(2, relationshipTable.split("columnWeights = CompatibilityReportColumnWeights").size - 1)
        assertFalse(relationshipTable.contains(".border(0.5.dp, Color(0xFFDCDCD8))"))

        val tableHeader = source.substringAfter("private fun CompatibilityTableHeader(")
            .substringBefore("private fun CompatibilityTableRow(")
        val tableRow = source.substringAfter("private fun CompatibilityTableRow(")
            .substringBefore("private fun CompatibilityRelationshipTable(")
        assertTrue(tableHeader.contains(".background(Color(0xFFF3F3F1))"))
        assertTrue(!tableHeader.contains("background(if (index == 0)"))
        assertTrue(tableHeader.contains(".fillMaxHeight()"))
        assertTrue(tableHeader.contains("contentAlignment = Alignment.Center"))
        assertTrue(tableHeader.contains("textAlign = TextAlign.Center"))
        assertTrue(tableRow.contains(".fillMaxHeight()"))
        assertTrue(tableRow.contains("contentAlignment = Alignment.Center"))
        assertTrue(tableRow.contains("textAlign = TextAlign.Center"))
        assertTrue(tableRow.contains("columnWeights.getOrElse"))
        assertTrue(source.contains("CompatibilityElementBar(\"男\""))
        assertTrue(source.contains("CompatibilityElementBar(\"女\""))
        assertTrue(!source.contains("CompatibilityElementRadar("))
        assertTrue(source.contains("background(if (index == 0) Color(0xFFF7F7F5) else Color.White)"))
        assertTrue(source.contains("fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium"))
        assertTrue(!source.contains("background = if (index % 2 == 0)"))
        assertTrue(source.contains("height(IntrinsicSize.Min)"))
        assertTrue(source.contains("VerticalDivider(modifier = Modifier.fillMaxHeight()"))
        assertTrue(source.contains("\"\${group.values}\\n\${group.explanation}\""))
        assertTrue(!source.contains("compatibilityElementColor"))
    }

    @Test
    fun compatibilityAiPromptRequiresSpecificEvidenceAndUsesFrozenNotes() {
        val source = File(locateSourceRoot(), "BaziAiPromptDialog.kt").readText()

        assertTrue(source.contains("断事笔记与已记录应事"))
        assertTrue(source.contains("盲派断事、子平格局"))
        assertTrue(source.contains("夫妻宫"))
        assertTrue(source.contains("不要泛泛使用"))
        assertTrue(source.contains("笔记内容｜对应命盘/岁运依据｜吻合程度"))
        assertTrue(!source.contains("compatibility_ai_prompt_privacy_row"))
        assertTrue(!source.contains("结尾强调结果仅供传统文化与自我沟通参考"))
    }

    @Test
    fun compatibilityHistoryUsesCompactPairedIdentityCardsInsteadOfBareNames() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val history = source.substringAfter("private fun CompatibilityHistoryScreen(")
            .substringBefore("private fun BaziCompatibilityReportContent(")

        assertTrue(history.contains("CompatibilityHistoryRecordCard("))
        assertTrue(history.contains("loading: Boolean"))
        assertTrue(history.contains("error: String?"))
        assertTrue(history.contains("if (loading && records.isEmpty())"))
        assertTrue(history.contains("LoadingBox(\"正在读取合盘记录…\")"))
        assertTrue(history.contains("else if (error != null && records.isEmpty())"))
        assertTrue(history.contains("actionLabel = \"重新读取\""))
        assertTrue(history.contains("CompatibilityHistoryParticipantSummary("))
        assertTrue(history.contains("combinedClickable(onClick = onClick, onLongClick = onLongClick)"))
        assertTrue(history.contains("selectionMode"))
        assertTrue(history.contains("compatibility_history_batch_delete"))
        assertTrue(history.contains("confirm_compatibility_history_delete"))
        assertTrue(history.contains("仅删除本机保存的合盘报告"))
        assertTrue(history.contains("roleLabel = \"男方\""))
        assertTrue(history.contains("roleLabel = \"女方\""))
        assertTrue(history.contains("participant.solarDateTimeText"))
        assertTrue(history.contains("生肖 \${participant.zodiac"))
        assertTrue(history.contains("日主 \${participant.dayMaster}"))
        assertTrue(history.contains("CompatibilityHistoryRelationshipFocus(record.report)"))
        assertTrue(history.contains("formatCompatibilityHistoryCreatedAt(record.createdAtEpochMillis)"))
        assertTrue(!history.contains("Icons.Filled.KeyboardArrowRight"))
    }

    @Test
    fun compatibilityHistoryBackAlwaysReturnsToItsImmediateParent() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val compatibility = source.substringAfter("private fun BaziCompatibilityScreen(")
            .substringBefore("private fun BaziCompatibilityReportScreen(")
        val history = source.substringAfter("private fun CompatibilityHistoryScreen(")
            .substringBefore("private fun CompatibilityHistoryRecordCard(")

        assertTrue(compatibility.contains("if (historyRecord != null) {\n        BackHandler(onBack = onCloseHistoryRecord)"))
        assertTrue(compatibility.contains("onBack = { showCompatibilityHistory = false }"))
        assertTrue(history.contains("BackHandler(onBack = onBack)"))
    }

    @Test
    fun detailTopBackUsesTheSameNavigatorParentAsTheSystemBackGesture() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val detailRoute = source.substringAfter("is AppDestination.CaseDetail -> {")
            .substringBefore("is AppDestination.CaseObjectiveSummary ->")

        assertTrue(detailRoute.contains("if (state.detailIsTransient)"))
        assertTrue(detailRoute.contains("viewModel.closeTransientDetail()"))
        assertTrue(detailRoute.contains("viewModel.navigateBack()"))
        assertTrue(!detailRoute.contains("viewModel.backToList()"))
    }

    @Test
    fun systemBackReturnsCompatibilityChildrenToTheirParentInsteadOfExiting() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val viewModel = File(locateSourceRoot(), "StageTwoViewModel.kt").readText()
        val rootBack = source.substringAfter("val returnsToCompatibilityCreate =")
            .substringBefore("NanfengBaziTheme(")
        val navigateBack = viewModel.substringAfter("fun navigateBack()")
            .substringBefore("fun closeTransientDetail()")

        assertTrue(rootBack.contains("state.compatibilityParticipantSelectionRole != null"))
        assertTrue(rootBack.contains("returnsToCompatibilityCreate"))
        assertTrue(rootBack.contains("viewModel.cancelCompatibilityParticipantCreate()"))
        assertTrue(navigateBack.contains("cancelCompatibilityParticipantCreate()"))
        assertTrue(navigateBack.contains("cancelCompatibilityParticipantList()"))
    }

    @Test
    fun recordLibraryTabsKeepStableGeometryWithDistinctSemanticTextColors() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val topTabs = source.substringAfter("private fun RecordTopTab(")
            .substringBefore("private fun RecordCategoryTab(")

        assertTrue(source.contains("tag = \"visibility_active\",\n                            accent = NanfengGreen"))
        assertTrue(source.contains("tag = \"visibility_celebrity\",\n                            accent = NanfengGoldText"))
        assertTrue(source.contains("tag = \"visibility_trashed\",\n                            accent = NanfengSolarTermRed"))
        assertTrue(topTabs.contains("accent: Color"))
        assertTrue(topTabs.contains("accent.copy(alpha = 0.78f)"))
        assertTrue(topTabs.contains("if (selected) accent.copy(alpha = 0.22f)"))
    }

    @Test
    fun recordToolbarSearchAndVisibilityTabsSharePillGeometry() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val toolbar = source.substringAfter(".testTag(\"record_toolbar\")")
            .substringBefore("private fun CaseListContent(")
        val topTabs = source.substringAfter("private fun RecordTopTab(")
            .substringBefore("private fun RecordCategoryTab(")

        assertTrue(source.contains("private val RecordToolbarPillShape = RoundedCornerShape(24.dp)"))
        assertTrue(toolbar.contains(".testTag(\"record_visibility_switcher\")"))
        assertTrue(toolbar.contains(".testTag(\"case_search\")"))
        assertTrue(toolbar.contains("shape = RecordToolbarPillShape"))
        assertTrue(topTabs.contains("shape = RecordToolbarPillShape"))
    }

    @Test
    fun celebrityLibraryIsBuiltInAndKeepsHistoricalImportAsEvidenceOnly() {
        val root = locateSourceRoot()
        val source = File(root, "StageTwoScreens.kt").readText()
        val activity = File(root, "MainActivity.kt").readText()
        val settings = source.substringAfter("SettingsGroupTitle(\"导入与建档\")")
            .substringBefore("SettingsGroupTitle(\"排盘偏好\")")
        assertTrue(settings.contains("title = \"名人案例统一资料库\""))
        assertTrue(settings.contains("安装即自带；应用更新后自动同步"))
        assertTrue(!settings.contains("导入可考名人案例"))
        assertTrue(settings.contains("名人案例会自动纳入统一资料库"))
        assertTrue(activity.contains("viewModel.synchronizeBuiltInUnifiedCelebrityCatalog("))
        assertTrue(activity.contains("celebrity-unified-v1.json"))
    }

    @Test
    fun settingsPlaceBackupAndRestoreBeforeChartPreferences() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()

        assertTrue(
            source.indexOf("SettingsGroupTitle(\"备份与恢复\")") <
                source.indexOf("SettingsGroupTitle(\"导入与建档\")"),
        )
        assertTrue(
            source.indexOf("SettingsGroupTitle(\"导入与建档\")") <
                source.indexOf("SettingsGroupTitle(\"排盘偏好\")"),
        )
    }

    @Test
    fun recordOverflowMenuKeepsOrderedActionsAndDeleteDistinctWithoutHelperTitles() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val menu = source.substringAfter("NanfengWhiteDropdownMenu(\n                        expanded = moreExpanded")
            .substringBefore("            OutlinedTextField(")

        assertTrue(!menu.contains("新建与导入"))
        assertTrue(!menu.contains("列表整理"))
        assertTrue(!menu.contains("批量操作"))
        assertTrue(menu.indexOf("label = \"新增名人案例\"") < menu.indexOf("label = \"列表排序\""))
        assertTrue(menu.indexOf("label = \"列表排序\"") < menu.indexOf("label = \"批量删除\""))
        assertTrue(menu.contains("accent = NanfengGoldText"))
        assertTrue(menu.contains("accent = NanfengOrange"))
        assertTrue(menu.contains("accent = NanfengSolarTermRed"))
        assertEquals(2, menu.split("HorizontalDivider(").size - 1)
        assertTrue(sharedOverflowMenuItemSource().contains("leadingIcon ="))
        assertTrue(!source.contains("private fun RecordOverflowMenuItem("))
    }

    @Test
    fun caseDetailOverflowMenuUsesTheSameSemanticIconAndColorSystem() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val detailMenu = source.substringAfter("contentDescription = \"管理命例\"")
            .substringBefore("} else {\n                            NanfengOverflowMenuItem(")

        assertTrue(detailMenu.contains("label = \"编辑基本资料\""))
        assertTrue(detailMenu.contains("icon = Icons.Filled.Label"))
        assertTrue(detailMenu.contains("icon = Icons.Filled.FileUpload"))
        assertTrue(detailMenu.contains("icon = Icons.Filled.Image"))
        assertTrue(detailMenu.contains("icon = Icons.Filled.Share"))
        assertTrue(detailMenu.contains("accent = NanfengSolarTermRed"))
        assertEquals(2, detailMenu.split("HorizontalDivider(").size - 1)
    }

    @Test
    fun compactRootNavigationFloatsOverContentWithoutAReservedBottomBand() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()

        assertTrue(source.contains("val shouldFloatRootNavigation = showRootNavigation && !useNavigationRail"))
        assertTrue(source.contains("val rootScreenModifier = Modifier.padding(padding)"))
        assertTrue(source.contains("The phone navigation is a true overlay"))
        assertTrue(source.contains("bottomContentInset = 0.dp"))
        assertTrue(source.contains("val recordAlphabetIndexBottomInset"))
        assertTrue(source.contains("alphabetIndexBottomInset = recordAlphabetIndexBottomInset"))
        assertTrue(source.contains(".padding(bottom = alphabetIndexBottomInset)"))
        assertTrue(!source.contains("floatingNavigationHeightPx"))
        assertTrue(!source.contains("FLOATING_ROOT_NAVIGATION_CONTENT_INSET"))
        assertTrue(!source.contains("FLOATING_ROOT_NAVIGATION_CLEARANCE"))
        assertTrue(source.contains(".align(Alignment.BottomCenter)"))
        assertTrue(source.contains(".navigationBarsPadding()"))
        assertTrue(source.contains("modifier = rootScreenModifier"))
        val compatibilityRoutes = source.substringAfter("AppDestination.BaziCompatibility -> BaziCompatibilityScreen(")
            .substringBefore("AppDestination.FourPillarsLookup ->")
        assertEquals(2, compatibilityRoutes.split("modifier = Modifier,").size - 1)
        assertTrue(!compatibilityRoutes.contains("modifier = Modifier.padding(padding)"))
    }

    @Test
    fun recordPageKeepsGrayStructureAroundWhiteCaseRows() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val recordPage = source.substringAfter("private fun CaseListScreen(")
            .substringBefore("private fun RecordTopTab(")
        val swipeRow = source.substringAfter("private fun SwipeableCaseSummaryRow(")
            .substringBefore("private fun SwipeCaseAction(")

        assertTrue(recordPage.contains(".background(MaterialTheme.colorScheme.background)"))
        assertTrue(recordPage.contains("else -> Box(\n                modifier = Modifier\n                    .weight(1f)"))
        assertTrue(recordPage.contains(".padding(bottom = bottomContentInset)\n                            .testTag(\"record_batch_action_bar\")"))
        assertTrue(
            recordPage.contains(".fillMaxWidth()\n                                    .background(MaterialTheme.colorScheme.background)"),
        )
        assertTrue(recordPage.contains("Box(modifier = Modifier.padding(end = 28.dp))"))
        assertTrue(!recordPage.contains("contentPadding = androidx.compose.foundation.layout.PaddingValues(\n                        end = 28.dp"))
        assertTrue(recordPage.contains("alphabetActivationDistancePx = with(LocalDensity.current) { 48.dp.roundToPx() }"))
        assertTrue(recordPage.contains("item.offset <= viewportStart + alphabetActivationDistancePx"))
        assertTrue(recordPage.contains(".padding(bottom = bottomContentInset)"))
        assertTrue(recordPage.contains("contentAlignment = Alignment.CenterEnd"))
        assertTrue(recordPage.contains(".fillMaxHeight(0.92f)"))
        assertTrue(recordPage.contains(".offset(y = 10.dp)"))
        assertTrue(recordPage.contains("firstPinnedCaseIndex"))
        assertTrue(recordPage.contains("pinnedActive = activeAlphabetInitial == '星'"))
        assertTrue(recordPage.contains("onPinnedClick = {"))
        assertTrue(recordPage.contains("当前列表没有置顶命例"))
        val pinnedLocatorJump = recordPage.substringAfter("onPinnedClick = {")
            .substringBefore("onInitialClick = { initial ->")
        assertTrue(pinnedLocatorJump.contains("caseListState.scrollToItem(targetIndex)"))
        assertFalse(pinnedLocatorJump.contains("caseListState.smoothAlphabetScrollToItem(targetIndex)"))
        assertTrue(recordPage.contains("onInitialClick = { initial ->"))
        assertTrue(recordPage.contains("alphabetJumpJob?.cancel()"))
        assertTrue(recordPage.contains("caseListState.smoothAlphabetScrollToItem(targetIndex)"))
        assertTrue(recordPage.contains("alphabetHaptics.perform(AppHapticEvent.SELECTION)"))
        assertTrue(recordPage.contains("record_alphabet_empty_hint"))
        assertTrue(source.contains("private suspend fun LazyListState.smoothAlphabetScrollToItem"))
        assertTrue(source.contains("scrollToItem(approachIndex)"))
        assertTrue(source.contains("animateScrollToItem(safeTargetIndex)"))
        assertTrue(source.contains("private fun RecordAlphabetIndex("))
        assertTrue(source.contains("Icons.Filled.VerticalAlignTop"))
        assertTrue(source.contains(".width(30.dp)"))
        assertTrue(source.contains(".weight(1f)"))
        assertTrue(source.contains("modifier = Modifier.size(24.dp)"))
        val alphabetIndex = source.substringAfter("private fun RecordAlphabetIndex(")
            .substringBefore("private inline fun <T> List<T>.indexOfFirstBy")
        assertTrue(alphabetIndex.contains("indication = null"))
        assertTrue(alphabetIndex.contains("val visualActive = active || pressed"))
        assertTrue(alphabetIndex.contains("shape = CircleShape"))
        assertTrue(alphabetIndex.contains("record_alphabet_pinned_locator"))
        assertTrue(alphabetIndex.contains("定位至置顶命例"))
        assertTrue(alphabetIndex.contains("val pinnedVisualActive = pinnedActive || pinnedPressed"))
        assertTrue(alphabetIndex.contains("color = if (pinnedVisualActive)"))
        assertTrue(alphabetIndex.contains("Color.Transparent"))
        assertTrue(!alphabetIndex.contains("shadowElevation = 1.dp"))
        assertTrue(!alphabetIndex.contains("shape = RectangleShape"))
        assertTrue(
            recordPage.contains(
                "state.listLoading && state.cases.isEmpty() -> LoadingBox(\"正在读取命例…\")",
            ),
        )
        assertTrue(
            source.contains("state.detailLoading && state.detail == null ->"),
        )
        assertTrue(recordPage.contains(".testTag(\"record_batch_select_all\")"))
        assertTrue(recordPage.contains("if (allVisibleDeleteCasesSelected) \"取消全选\" else \"全选\""))
        assertTrue(recordPage.contains("\"删除 ${'$'}{deleteSelection.size}\""))
        assertTrue(recordPage.contains(".testTag(\"record_search_delete\")"))
        assertTrue(recordPage.contains("onLongClick = { onQueryChange(\"\") }"))
        assertTrue(recordPage.contains("onQueryChange(state.query.dropLastTextElement())"))
        assertTrue(recordPage.contains(".width(72.dp)"))
        assertTrue(recordPage.contains("Alignment.CenterHorizontally"))
        assertTrue(recordPage.contains("horizontalArrangement = Arrangement.spacedBy(4.dp)"))
        val categoryTab = source.substringAfter("private fun RecordCategoryTab(")
            .substringBefore("private fun String.recordLabelAndCount()")
        assertTrue(categoryTab.contains(".width(92.dp)"))
        assertTrue(categoryTab.contains("val compactText = label.length + (count?.length ?: 0) >= 6"))
        assertTrue(categoryTab.contains("fontSize = if (compactText) 9.sp else 11.sp"))
        assertTrue(categoryTab.contains("fontSize = if (compactText) 8.sp else 9.sp"))
        assertTrue(categoryTab.contains("softWrap = false"))
        assertTrue(categoryTab.contains("if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White"))
        assertTrue(categoryTab.contains("fontWeight = FontWeight.Medium"))
        assertTrue(swipeRow.contains(".background(Color.White)"))
    }

    @Test
    fun homeChoiceGroupsKeepReadableTypeAtACompactHeight() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val choiceGroup = source.substringAfter("private fun HomeChoiceGroup(")
            .substringBefore("internal fun HomePickerRow(")

        assertTrue(choiceGroup.contains("itemHeight: Dp = 32.dp"))
        assertTrue(choiceGroup.contains("itemWidth: Dp = 70.dp"))
        assertTrue(choiceGroup.contains(".height(itemHeight)"))
        assertTrue(choiceGroup.contains(".width(itemWidth)"))
        assertTrue(choiceGroup.contains("style = MaterialTheme.typography.bodyMedium"))
        assertTrue(!choiceGroup.contains("fontSize = 11.sp"))
    }

    @Test
    fun caseImageActionsUseOneNeutralSentenceAndTheShortSaveName() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val confirmation = source.substringAfter("state.caseImageConfirmationMode?.let { mode ->")
            .substringBefore("if (state.fullBackupExportConfirmationVisible)")

        assertTrue(source.contains("label = \"保存命盘长图\""))
        assertTrue(sharedOverflowMenuItemSource().contains("text = { Text(label, color = foreground) }"))
        assertTrue(confirmation.contains("\"保存两张命盘长图\""))
        assertTrue(confirmation.contains("\"分享两张命盘长图\""))
        assertTrue(confirmation.contains("基本资料、基本排盘、专业细盘合并为一张长图"))
        assertTrue(confirmation.contains("命主反馈、师傅点评、AI 点评"))
        assertTrue(confirmation.contains("\"保存\""))
        assertTrue(confirmation.contains("\"分享\""))
        assertTrue(!confirmation.contains("\"保存长图\""))
        assertTrue(!confirmation.contains("\"分享长图\""))
        assertTrue(!confirmation.contains("四个页面合并为一张长图"))
        assertTrue(!confirmation.contains("MaterialTheme.colorScheme.error"))
    }

    @Test
    fun caseLongImageExportSplitsChartAndKeepsAllThreeNotesModes() {
        val capture = File(locateSourceRoot(), "CaseDetailLongImageCapture.kt").readText()
        val screens = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val activity = File(locateSourceRoot(), "MainActivity.kt").readText()

        assertTrue(capture.contains("CapturedCaseDetailLongImages"))
        assertTrue(capture.contains("MAX_CAPTURE_OUTPUT_WIDTH = 1_140"))
        assertTrue(capture.contains("CaseDetailSection.BASIC_INFO"))
        assertTrue(capture.contains("CaseDetailSection.BASIC_CHART"))
        assertTrue(capture.contains("CaseDetailSection.FORTUNE"))
        assertTrue(capture.contains("sections = listOf(CaseDetailSection.RECORDS)"))
        assertTrue(capture.contains("registry.updateNotesCaptureState(true)"))
        assertTrue(screens.contains("captureForLongImage = captureRegistry.notesCaptureActive"))
        assertTrue(screens.contains("owner_feedback_capture"))
        assertTrue(screens.contains("master_commentary_capture"))
        assertTrue(screens.contains("ai_commentary_capture"))
        assertTrue(screens.contains("命主反馈、师傅点评、AI 点评"))
        assertTrue(activity.contains("Intent.ACTION_SEND_MULTIPLE"))
        assertTrue(activity.contains("CASE_IMAGE_SHARE_FILE_NAMES"))
    }

    @Test
    fun homeDoesNotRenderAnInlineInstantChartAndGroupPickerKeepsBottomSafeSpace() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val home = source.substringAfter("private fun WenzhenCreateCaseScreen(")
            .substringBefore("private fun HomeChoiceGroup(")
        val groupPicker = source.substringAfter("private fun HomeCaseGroupPickerDialog(")
            .substringBefore("private fun HomeCaseGroupOption(")
        val groupEditor = source.substringAfter("private fun RecordGroupEditorDialog(")
            .substringBefore("private fun RecordCaseSelectionDialog(")

        assertTrue(!home.contains("InstantCalculationPreviewCard("))
        assertTrue(groupPicker.contains(".imePadding()"))
        assertTrue(groupPicker.contains(".navigationBarsPadding()"))
        assertTrue(groupPicker.contains(".fillMaxSize()"))
        assertTrue(groupPicker.contains(".height(GroupDialogBottomSafetySpace)"))
        assertTrue(groupPicker.contains("home_group_bottom_safety_space"))
        assertTrue(groupEditor.contains(".height(GroupDialogBottomSafetySpace)"))
        assertTrue(groupEditor.contains("record_group_bottom_safety_space"))
        assertTrue(source.contains("private val GroupDialogBottomSafetySpace = 96.dp"))
        assertTrue(groupPicker.contains(".fillMaxHeight(0.92f)"))
        assertTrue(groupPicker.contains("name = \"全部\""))
        assertTrue(groupPicker.contains("featured = true"))
        assertTrue(groupPicker.contains("home_group_add"))
        assertTrue(groupEditor.contains("record_group_reorder_\${group.id}"))
        assertTrue(groupEditor.contains("detectVerticalDragGestures"))
        assertTrue(groupEditor.contains("拖动排序\${group.name}"))
        assertTrue(!groupEditor.contains("上移\${group.name}"))
        assertTrue(!groupEditor.contains("下移\${group.name}"))
    }

    @Test
    fun fixedPickerSheetsKeepTheThreeBirthModesAboveTheGestureArea() {
        val pickers = File(locateSourceRoot(), "BirthInputPickers.kt").readText()
        val fixedSheet = pickers.substringAfter("internal fun FixedPickerSheet(")
            .substringBefore("internal fun ObservationDateTimePickerSheet(")
        val observationPicker = pickers.substringAfter("internal fun ObservationDateTimePickerSheet(")
            .substringBefore("private fun <T> PickerSegmentedControl(")
        val screens = File(locateSourceRoot(), "StageTwoScreens.kt").readText()

        assertTrue(pickers.contains("private val PickerSheetBottomClearance = 24.dp"))
        assertTrue(fixedSheet.contains(".padding(bottom = PickerSheetBottomClearance)"))
        assertTrue(fixedSheet.contains(".height(resolvedHeight)"))
        assertTrue(fixedSheet.contains("RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)"))
        assertTrue(observationPicker.contains("showQuickLocateInput: Boolean = false"))
        assertTrue(observationPicker.contains("LaunchedEffect(showQuickLocateInput, quickLocateText)"))
        assertTrue(observationPicker.contains("BirthPickerQuickLocateInput("))
        assertTrue(observationPicker.contains("tag = \"fortune_quick_locate_input\""))
        assertTrue(observationPicker.contains("inputContentDescription = \"快速定位观察日期与时刻\""))
        assertTrue(screens.contains("showQuickLocateInput = true"))
    }

    @Test
    fun aiServiceDestinationsUseWideCenteredCardsInsteadOfATopPackedList() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val page = source.substringAfter("private fun AiServiceSettingsPage(")
            .substringBefore("private fun AiServiceDestinationCard(")
        val card = source.substringAfter("private fun AiServiceDestinationCard(")
            .substringBefore("private fun SettingsGroupTitle(")

        assertTrue(page.contains(".weight(1f)"))
        assertTrue(page.contains("contentAlignment = Alignment.Center"))
        assertTrue(page.contains(".padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomContentInset)"))
        assertTrue(page.contains("AiServiceDestinationCard("))
        assertTrue(!page.contains("SettingsActionGroup"))
        assertTrue(card.contains(".fillMaxWidth()"))
        assertTrue(card.contains(".heightIn(min = 68.dp)"))
        assertTrue(!card.contains(".fillMaxHeight()"))
        assertTrue(card.contains("Modifier.size(38.dp)"))
        assertTrue(card.contains("color = Color.White"))
        assertTrue(card.contains("tonalElevation = 0.dp"))
        assertTrue(card.contains("MaterialTheme.typography.titleMedium"))
        assertTrue(card.contains("MaterialTheme.typography.bodySmall"))
    }

    @Test
    fun settingsNamesSingleCaseTransferAndFullBackupRecoveryPrecisely() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val settings = source.substringAfter("private fun SettingsHomeScreen(")
            .substringBefore("if (showRatHourRulePicker)")
        val records = source.substringAfter("private fun RecordHubScreen(")
            .substringBefore("private fun RecordCaseRow(")
        val recordNavigationRoute = source.substringAfter("AppDestination.CaseList -> CaseListScreen(")
            .substringBefore("AppDestination.CaseComparison")

        assertTrue(settings.contains("title = \"导入南枫命例包\""))
        assertTrue(settings.contains("跨设备转移单个命例；先预览冲突再决定合并"))
        assertTrue(settings.contains("title = \"恢复完整备份\""))
        assertTrue(!settings.contains("预览并恢复完整备份"))
        assertFalse(settings.contains("SettingsGroupTitle(\"功能审阅\")"))
        assertFalse(settings.contains("settings_feature_review_"))
        assertFalse(settings.contains("onOpenBaziCompatibility"))
        assertFalse(settings.contains("onOpenRecords"))
        assertTrue(settings.contains("Spacer(modifier = Modifier.height(bottomContentInset))"))
        assertFalse(settings.contains("bottomContentInset + 12.dp"))
        assertTrue(source.contains("val rootNavigationScrollEndInset = if (shouldFloatRootNavigation)"))
        assertTrue(source.contains("bottomContentInset = rootNavigationScrollEndInset"))
        assertTrue(records.contains("bottom = bottomContentInset"))
        assertFalse(records.contains("24.dp + bottomContentInset"))
        assertTrue(recordNavigationRoute.contains("bottomContentInset = rootNavigationScrollEndInset"))
    }

    @Test
    fun fullBackupExportUsesTheSameMinimalPasswordRouteAsSingleCaseExport() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val export = source.substringAfter("if (state.fullBackupExportConfirmationVisible)")
            .substringBefore("if (state.fullBackupPasswordImportVisible)")

        assertTrue(export.contains("title = { Text(\"导出完整备份\") }"))
        assertTrue(export.contains("默认导出 ZIP；文件含敏感命例资料，请妥善保存。"))
        assertTrue(export.contains("Text(\"导出\")"))
        assertTrue(export.contains("Text(\"改用密码加密\")"))
        assertTrue(export.contains("title = \"导出密码\""))
        assertTrue(export.contains("输入至少 6 位密码，请妥善保管。"))
        assertTrue(export.contains("requireConfirmation = false"))
        assertTrue(export.contains("dismissLabel = \"返回\""))
        assertTrue(!export.contains("full_backup_password_confirmation"))
        assertTrue(!export.contains("确认并选择位置"))
    }

    @Test
    fun almanacKeepsBothPrimaryActionsFixedAtTheBottom() {
        val source = File(locateSourceRoot(), "AlmanacFeature.kt").readText()
        val screen = source.substringAfter("internal fun AlmanacScreen(")
            .substringBefore("private fun AlmanacPrimaryActions(")
        val actions = source.substringAfter("private fun AlmanacPrimaryActions(")
            .substringBefore("private fun AlmanacTopBar(")
        val details = source.substringAfter("private fun AlmanacDetailsCard(")
            .substringBefore("private fun AlmanacDoubleHourRail(")

        assertTrue(screen.indexOf("AlmanacPrimaryActions(") > screen.indexOf("BoxWithConstraints("))
        assertTrue(screen.contains(".weight(1f)"))
        assertTrue(actions.contains("use_almanac_date_for_chart"))
        assertTrue(actions.contains("adjust_almanac_four_pillars"))
        assertTrue(actions.contains("horizontalArrangement = Arrangement.spacedBy(10.dp)"))
        assertTrue(actions.contains("navigationBarsPadding()"))
        assertTrue(actions.contains("shadowElevation = 8.dp"))
        assertTrue(!details.contains("use_almanac_date_for_chart"))
        assertTrue(!details.contains("adjust_almanac_four_pillars"))
    }

    @Test
    fun appOwnedOverflowMenusUseAnExplicitWhiteZeroTonalSurface() {
        val sourceRoot = locateSourceRoot()
        val theme = File(sourceRoot, "NanfengBaziTheme.kt").readText()
        val menu = theme.substringAfter("internal fun NanfengWhiteDropdownMenu(")

        assertTrue(menu.contains("Popup("))
        assertTrue(menu.contains("color = Color.White"))
        assertTrue(menu.contains("tonalElevation = 0.dp"))
        val directDropdownMenu = Regex("""\bDropdownMenu\(""")
        assertTrue(!directDropdownMenu.containsMatchIn(theme))
        sourceRoot.listFiles().orEmpty()
            .filter { it.extension == "kt" && it.name != "NanfengBaziTheme.kt" }
            .forEach { source ->
                assertTrue(
                    "${source.name} 不得绕过纯白菜单共享入口直接创建 DropdownMenu。",
                    !directDropdownMenu.containsMatchIn(source.readText()),
                )
            }
    }

    @Test
    fun directAiProviderModelMenusStayBelowTheirSelectedModelSurface() {
        val theme = File(locateSourceRoot(), "NanfengBaziTheme.kt").readText()
        val dialogs = File(locateSourceRoot(), "AiCommentaryDialogs.kt").readText()
        val picker = dialogs.substringAfter("testTag(\"ai_model_picker\")")
            .substringBefore("OutlinedTextField(")
        val vendorStyle = dialogs.substringAfter("private fun aiModelVendorStyle(")

        assertTrue(theme.contains("enum class NanfengPopupPlacement"))
        assertTrue(theme.contains("placement == NanfengPopupPlacement.BELOW_ANCHOR -> below"))
        assertTrue(theme.contains("alignment = Alignment.TopEnd"))
        assertTrue(theme.contains("offset = IntOffset(0, anchorBounds.height + menuGapPx)"))
        assertTrue(picker.contains("providerId == AiCommentaryProviderId.OPEN_ROUTER"))
        assertTrue(picker.contains("NanfengPopupPlacement.BELOW_ANCHOR"))
        assertTrue(vendorStyle.contains("preset.model.startsWith(\"deepseek-\")"))
        assertTrue(vendorStyle.contains("preset.model.startsWith(\"qwen\")"))
        assertTrue(vendorStyle.contains("container = Color(0xFFEDF3FC)"))
        assertTrue(vendorStyle.contains("container = Color(0xFFF3F0FA)"))
    }

    @Test
    fun manualClickablesAreEitherShapeClippedOrExplicitRectangularRegions() {
        val sourceRoot = locateSourceRoot()
        val expectedDirectClickables = mapOf(
            "AlmanacFeature.kt" to setOf("AlmanacCalendarCard"),
            "BirthInputPickers.kt" to setOf("FixedPickerSheet"),
            "FourPillarsInputPresentation.kt" to setOf("BaziCharacterSlot"),
            "BaziSkinPicker.kt" to setOf("BaziSkinPickerDialog"),
            "StageTwoScreens.kt" to setOf(
                "SettingsActionRow",
                "CaseSummaryRow",
                "HomePickerRow",
                "HomeCaseGroupPickerDialog",
                "RecordAdvancedFilterDialog",
                "RecordSortDialog",
                "RecordGroupEditorDialog",
                "RecordCaseSelectionDialog",
                "RecordAlphabetIndex",
                "NotesModeTab",
                "ReferenceOtherNotes",
                "ReferenceEventTimelineItem",
            ),
        )
        val clippedFunctions = setOf("AlmanacCalendarCard", "BaziCharacterSlot")

        val actual = sourceRoot.listFiles()
            .orEmpty()
            .filter { it.extension == "kt" }
            .associate { source ->
                var currentFunction = ""
                val clickables = mutableSetOf<String>()
                val lines = source.readLines()
                lines.forEachIndexed { index, line ->
                    functionName(line)?.let { currentFunction = it }
                    if (".clickable" in line && !line.trimStart().startsWith("import ")) {
                        clickables += currentFunction
                        if (currentFunction in clippedFunctions) {
                            val modifierWindow = lines.subList((index - 4).coerceAtLeast(0), index)
                            assertTrue(
                                "$currentFunction 的手写点击反馈必须先按自身形状裁切。",
                                modifierWindow.any { ".clip(" in it },
                            )
                        }
                    }
                }
                source.name to clickables
            }
            .filterValues { it.isNotEmpty() }

        assertEquals(
            "新增交互面应优先使用可点击 Surface/Card；确属矩形区域时才加入显式清单。",
            expectedDirectClickables,
            actual,
        )
    }

    @Test
    fun caseNotesHideProfileFactsAndKeepTimelineInputsContentSized() {
        val source = locateSourceRoot().resolve("StageTwoScreens.kt").readText()

        val notes = source.substringAfter("private fun ReferenceCaseNotes(")
            .substringBefore("private fun AiCommentaryEditor(")
        assertTrue(!notes.contains("ReferenceOwnerProfileSheet"))
        assertTrue(!source.contains("private fun ReferenceOwnerProfileSheet"))

        val timelineInput = source.substringAfter("private fun CaseNotesTimelineInput(")
            .substringBefore("private fun CaseNotesTimePicker(")
        val noteEditor = source.substringAfter("private fun CaseNotesTextEditor(")
            .substringBefore("private fun CaseNotesEditorScrollbar(")
        assertTrue(timelineInput.contains("minLines = 1"))
        assertTrue(timelineInput.contains("CaseNotesTextEditor("))
        assertTrue(noteEditor.contains("maxLines = Int.MAX_VALUE"))
    }

    @Test
    fun celebrityCaseTimelineUsesTheSameEditableInputAndSaveCallback() {
        val source = locateSourceRoot().resolve("StageTwoScreens.kt").readText()
        val celebrityTimeline = source.substringAfter("private fun CelebrityCaseTimeline(")
            .substringBefore("private fun Int.toTimelineYearLabel()")

        assertTrue(celebrityTimeline.contains("CaseNotesTimelineInput("))
        assertTrue(celebrityTimeline.contains("enabled = enabled"))
        assertTrue(celebrityTimeline.contains("onContentChange = onContentChange"))
    }

    @Test
    fun ownerFeedbackAndMasterCommentaryShareTheSameFixedHeaderSlot() {
        val source = locateSourceRoot().resolve("StageTwoScreens.kt").readText()
        val notes = source.substringAfter("private fun ReferenceCaseNotes(")
            .substringBefore("private fun AiCommentaryEditor(")

        assertTrue(notes.contains("tag = \"owner_feedback_header\""))
        assertTrue(notes.contains("tag = \"master_commentary_header\""))
        assertTrue(!notes.contains("WenzhenSectionHeader(\n                title = \"命主反馈\""))
    }

    @Test
    fun aiCommentaryRegenerationActionsFollowSkinPrimaryColor() {
        val screens = locateSourceRoot().resolve("StageTwoScreens.kt").readText()
        val dialogs = locateSourceRoot().resolve("AiCommentaryDialogs.kt").readText()
        val header = screens.substringAfter("private fun CaseNotesCommentaryHeader(")
            .substringBefore("private fun CaseNotesSaveFooter(")
        val generation = dialogs.substringAfter("private fun AiCommentaryGenerationDialog(")
            .substringBefore("private fun AiCommentarySettingsDialog(")

        assertTrue(header.contains("color = MaterialTheme.colorScheme.primary"))
        assertTrue(header.contains("contentColor = MaterialTheme.colorScheme.onPrimary"))
        assertTrue(generation.contains("containerColor = MaterialTheme.colorScheme.primary"))
        assertTrue(generation.contains("contentColor = Color.White"))
        assertTrue(generation.contains("RoundedCornerShape(24.dp)"))
    }

    @Test
    fun aiCommentaryVersionsAreSelectableAndLongImageKeepsEveryModel() {
        val source = locateSourceRoot().resolve("StageTwoScreens.kt").readText()
        val editor = source.substringAfter("private fun AiCommentaryEditor(")
            .substringBefore("private fun AiCommentaryVersionSelector(")
        val selector = source.substringAfter("private fun AiCommentaryVersionSelector(")
            .substringBefore("private fun CaseNotesCommentaryHeader(")

        assertTrue(editor.contains("versions.asReversed().forEach"))
        assertTrue(editor.contains("ai_commentary_capture_"))
        assertTrue(selector.contains("ai_commentary_version_selector"))
        assertTrue(selector.contains("Surface("))
        assertTrue(selector.contains("RoundedCornerShape(14.dp)"))
        assertTrue(selector.contains("contentAlignment = Alignment.Center"))
        assertTrue(selector.contains("textAlign = TextAlign.Center"))
        assertTrue(selector.contains("widthIn(max = 156.dp)"))
    }

    @Test
    fun notesFooterHidesRoutineAutosaveHintsButKeepsRealErrors() {
        val source = locateSourceRoot().resolve("StageTwoScreens.kt").readText()
        val footer = source.substringAfter("private fun CaseNotesSaveFooter(")
            .substringBefore("private fun CaseNotesTextEditor(")

        assertTrue(footer.contains("if (saveError != null)"))
        assertTrue(!footer.contains("已自动保存"))
        assertTrue(!footer.contains("编辑中，将自动保存"))
        assertTrue(!footer.contains("正在保存…"))
    }

    @Test
    fun compactNotesModePressFeedbackStaysInsideTheVisibleRail() {
        val source = locateSourceRoot().resolve("StageTwoScreens.kt").readText()
        val tab = source.substringAfter("private fun NotesModeTab(")
            .substringBefore("private fun ReferenceEventTimelineItem(")

        assertTrue(tab.contains("collectIsPressedAsState()"))
        assertTrue(tab.contains("indication = null"))
        assertTrue(tab.contains("val visualShape = RoundedCornerShape(18.dp)"))
        assertTrue(tab.contains(".clip(visualShape)"))
        assertTrue(tab.contains("Color.Black.copy(alpha = if (selected) 0.13f else 0.08f)"))
    }

    @Test
    fun professionalSectionTitlesUseTheSkinGrayBackgroundInsteadOfButtonPrimary() {
        val source = locateSourceRoot().resolve("StageTwoScreens.kt").readText()
        val sections = source.substringAfter("private fun ProfessionalTextSections(")
            .substringBefore("private fun DecadeFortuneDetailsView(")

        assertTrue(sections.contains("val themeBackground = LocalBaziSkinTokens.current.background"))
        assertTrue(sections.contains("background(themeBackground)"))
        assertTrue(sections.contains("val themeText = LocalBaziSkinTokens.current.textPrimary"))
        assertTrue(!sections.contains("background(MaterialTheme.colorScheme.primary)"))
    }

    @Test
    fun almanacShenShaAreaReservesFiveCompactRows() {
        val almanac = locateSourceRoot().resolve("AlmanacFeature.kt").readText()

        assertTrue(almanac.contains("lineHeight = 13.sp"))
        assertTrue(almanac.contains("AlmanacShenShaReservedHeight = 68.dp"))
        assertTrue(almanac.contains("默认预留五条紧凑神煞"))
    }

    @Test
    fun almanacHourPillarRailCentersWholeTwelveHourGroupWhenItFits() {
        val almanac = locateSourceRoot().resolve("AlmanacFeature.kt").readText()
        val rail = almanac.substringAfter("private fun AlmanacHourPillarRail(")
            .substringBefore("private fun AlmanacEightCharacterTable(")

        assertTrue(rail.contains("BoxWithConstraints"))
        assertTrue(rail.contains("centerWholeRail = maxWidth >= railContentWidth"))
        assertTrue(rail.contains("Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)"))
        assertTrue(rail.contains("Modifier.horizontalScroll(rememberScrollState())"))
    }

    @Test
    fun almanacPillarTableAndUnselectedHoursShareTheHalfStrengthBackground() {
        val almanac = locateSourceRoot().resolve("AlmanacFeature.kt").readText()
        val rail = almanac.substringAfter("private fun AlmanacHourPillarRail(")
            .substringBefore("private fun AlmanacEightCharacterTable(")
        val table = almanac.substringAfter("private fun AlmanacEightCharacterTable(")
            .substringBefore("private fun AlmanacPillarRow(")

        assertTrue(almanac.contains("AlmanacMutedBackgroundAlpha = 0.5f"))
        assertTrue(rail.contains("else almanacMutedBackgroundColor()"))
        assertTrue(table.contains("color = almanacMutedBackgroundColor()"))
    }

    @Test
    fun baziSkinSystemKeepsSixLocalOnlySkinsWithTheNanfengjiVerticalPreviewFlow() {
        val root = locateSourceRoot()
        val skin = File(root, "BaziSkin.kt").readText()
        val picker = File(root, "BaziSkinPicker.kt").readText()
        val screens = File(root, "StageTwoScreens.kt").readText()

        assertEquals(6, Regex("id = ").findAll(skin).count())
        listOf("玄墨星盘", "青玉五行", "朱砂云纹", "金棕卦象", "靛蓝星河", "霜白桃花")
            .forEach { assertTrue(skin.contains(it)) }
        assertTrue(skin.contains("不参与命例、备份、恢复码或南枫云快照"))
        assertTrue(picker.contains("VerticalPager"))
        assertTrue(picker.contains("上下滑动逐套预览，停稳后立即应用"))
        assertTrue(picker.contains("onSkinPreview(originalSkin)"))
        assertTrue(picker.contains("BaziHomeSkinHeader"))
        assertTrue(screens.indexOf("SettingsGroupTitle(\"皮肤\")") <
            screens.indexOf("SettingsGroupTitle(\"南枫云\")"))
        assertTrue(screens.contains("Modifier.testTag(\"home_skin_header\")"))
    }

    @Test
    fun homeSkinHeaderUsesAClippedTapAndDragFiveElementFlowInsteadOfStaticDecoration() {
        val picker = File(locateSourceRoot(), "BaziSkinPicker.kt").readText()
        val header = picker.substringAfter("internal fun BaziHomeSkinHeader(")
            .substringBefore("internal fun BaziSkinSettingRow(")

        assertTrue(header.contains("Surface(\n        onClick = activateFlow"))
        assertTrue(header.contains("BaziHomeSkinHeaderShape"))
        assertTrue(header.contains("detectHorizontalDragGestures"))
        assertTrue(header.contains("flowProgress.animateTo("))
        assertTrue(header.contains("BaziHomeFlowOverlay("))
        assertTrue(header.contains("BaziHomeHeaderArtworkOverscan"))
        assertTrue(header.contains("点击或左右滑动可查看五行流转效果"))
        assertTrue(header.contains("mutableStateOf(baziHomeSolarDateTime())"))
        assertTrue(header.contains("LaunchedEffect(Unit)"))
        assertTrue(header.contains("currentSolarTime = baziHomeSolarDateTime()"))
        assertTrue(header.contains(".align(Alignment.BottomStart)"))
        assertTrue(header.contains("yyyy年MM月dd日 · HH:mm:ss"))
        assertTrue(picker.contains("private fun BaziHomeFlowOverlay("))
        assertTrue(picker.contains("Canvas(modifier = modifier)"))
        assertTrue(picker.contains("Brush.linearGradient("))
        assertTrue(picker.contains("drawPath("))
    }

    private fun sharedOverflowMenuItemSource(): String =
        File(locateSourceRoot(), "NanfengBaziTheme.kt").readText()
            .substringAfter("internal fun NanfengOverflowMenuItem(")
            .substringBefore("private object FullScreenPopupPositionProvider")

    private fun locateSourceRoot(): File = generateSequence(File(".").canonicalFile) { it.parentFile }
        .flatMap { root ->
            sequenceOf(
                File(root, "app/src/main/kotlin/com/nanzhufeng/nanfengbazi"),
                File(root, "src/main/kotlin/com/nanzhufeng/nanfengbazi"),
            )
        }
        .firstOrNull(File::isDirectory)
        ?: error("找不到 app Compose 源码目录。")

    private fun functionName(line: String): String? =
        Regex("""(?:private|internal|public)?\s*fun\s+(?:<[^>]+>\s*)?(\w+)""")
            .find(line)
            ?.groupValues
            ?.get(1)
}
