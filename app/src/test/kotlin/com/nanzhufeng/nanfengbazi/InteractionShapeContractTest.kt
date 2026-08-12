package com.nanzhufeng.nanfengbazi

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionShapeContractTest {
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
        assertTrue(navigation.contains(".height(68.dp)"))
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
    fun recordOverflowMenuKeepsOrderedActionsAndDeleteDistinctWithoutHelperTitles() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val menu = source.substringAfter("NanfengWhiteDropdownMenu(\n                        expanded = moreExpanded")
            .substringBefore("}\n                }\n            }\n            OutlinedTextField")

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
    fun compactRootNavigationFloatsOverThePageWithBottomContentSafeSpace() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()

        assertTrue(source.contains("val shouldFloatRootNavigation = showRootNavigation && !useNavigationRail"))
        assertTrue(source.contains("FLOATING_ROOT_NAVIGATION_CONTENT_INSET = 84.dp"))
        assertTrue(source.contains("val rootScreenModifier = Modifier.padding(padding)"))
        assertTrue(source.contains("val floatingNavigationContentInset = if (shouldFloatRootNavigation)"))
        assertTrue(!source.contains("Modifier.padding(bottom = FLOATING_ROOT_NAVIGATION_CONTENT_INSET)"))
        assertTrue(source.contains(".align(Alignment.BottomCenter)"))
        assertTrue(source.contains(".navigationBarsPadding()"))
        assertTrue(source.contains("modifier = rootScreenModifier"))
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

        assertTrue(choiceGroup.contains(".height(32.dp)"))
        assertTrue(choiceGroup.contains(".width(70.dp)"))
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

        assertTrue(settings.contains("title = \"导入南枫命例包\""))
        assertTrue(settings.contains("跨设备转移单个命例；先预览冲突再决定合并"))
        assertTrue(settings.contains("title = \"恢复完整备份\""))
        assertTrue(!settings.contains("预览并恢复完整备份"))
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
    fun almanacKeepsBothPrimaryActionsTogetherAtTheTop() {
        val source = File(locateSourceRoot(), "AlmanacFeature.kt").readText()
        val screen = source.substringAfter("internal fun AlmanacScreen(")
            .substringBefore("private fun AlmanacPrimaryActions(")
        val actions = source.substringAfter("private fun AlmanacPrimaryActions(")
            .substringBefore("private fun AlmanacTopBar(")
        val details = source.substringAfter("private fun AlmanacDetailsCard(")
            .substringBefore("private fun AlmanacDoubleHourRail(")

        assertTrue(screen.indexOf("AlmanacPrimaryActions(") < screen.indexOf("BoxWithConstraints("))
        assertTrue(actions.contains("use_almanac_date_for_chart"))
        assertTrue(actions.contains("adjust_almanac_four_pillars"))
        assertTrue(actions.contains("horizontalArrangement = Arrangement.spacedBy(10.dp)"))
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
        assertTrue(timelineInput.contains("minLines = 1"))
        assertTrue(timelineInput.contains("maxLines = Int.MAX_VALUE"))
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
