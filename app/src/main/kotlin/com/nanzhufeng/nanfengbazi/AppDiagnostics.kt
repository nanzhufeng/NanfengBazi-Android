package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase

/**
 * Builds a privacy-safe support bundle from structured UI state.
 *
 * Deliberately excluded: case ids, names, birth data, file names, paths, URIs,
 * OCR text, passwords, attachment hashes and screenshot contents.
 */
internal fun buildAppDiagnosticText(
    state: StageTwoUiState,
    screenshot: ScreenshotImportUiState,
    appVersion: String,
    versionCode: Int,
): String = buildString {
    appendLine("南枫八字诊断包")
    appendLine("privacy=REDACTED")
    appendLine("app.version=$appVersion")
    appendLine("app.version_code=$versionCode")
    appendLine("database.schema=${NanfengBaziDatabase.SCHEMA_VERSION}")
    appendLine("ui.destination=${state.destination.diagnosticName()}")
    appendLine("ui.list_error=${state.listError.diagnosticCode()}")
    appendLine("ui.detail_error=${state.detailError.diagnosticCode()}")
    appendLine("ui.form_error=${state.formError.diagnosticCode()}")
    appendLine("ui.mutation_error=${state.mutationError.diagnosticCode()}")
    appendLine("exchange.busy=${state.singleCaseExchangeBusy}")
    appendLine("exchange.error=${state.singleCaseExchangeError.diagnosticCode()}")
    appendLine("backup.busy=${state.fullBackupBusy}")
    appendLine("backup.preview=${state.fullBackupPreview != null}")
    appendLine("backup.plan=${state.fullBackupRestorePlan != null}")
    appendLine("backup.error=${state.fullBackupError.diagnosticCode()}")
    appendLine("screenshot.busy=${screenshot.busy}")
    appendLine("screenshot.status=${screenshot.sessionStatus?.name ?: "NONE"}")
    appendLine("screenshot.parser=${screenshot.parserVersion ?: "NONE"}")
    appendLine("screenshot.images=${screenshot.completedImageCount}")
    appendLine("screenshot.failed_images=${screenshot.failedImageCount}")
    appendLine("screenshot.candidates=${screenshot.caseCandidateCount}")
    appendLine("screenshot.fields=${screenshot.extractedFieldCount}")
    appendLine("screenshot.long_texts=${screenshot.extractedLongTextCount}")
    appendLine("screenshot.recoverable_sessions=${screenshot.recoverableSessionCount}")
    appendLine(
        "screenshot.page_types=" +
            screenshot.classifiedPageTypes
                .groupingBy { it.name }
                .eachCount()
                .toSortedMap()
                .entries
                .joinToString(",") { (type, count) -> "$type:$count" }
                .ifEmpty { "NONE" },
    )
    appendLine(
        "screenshot.failure_codes=" +
            screenshot.failureCodes.distinct().sorted().joinToString(",").ifEmpty { "NONE" },
    )
    appendLine(
        "screenshot.diagnostic_ids=" +
            screenshot.failureDiagnosticIds.distinct().sorted().take(8)
                .joinToString(",").ifEmpty { "NONE" },
    )
}

private fun AppDestination.diagnosticName(): String = when (this) {
    AppDestination.CaseList -> "CASE_LIST"
    AppDestination.RecordHub -> "RECORD_HUB"
    AppDestination.Settings -> "SETTINGS"
    AppDestination.CreateCase -> "CREATE_CASE"
    AppDestination.ScreenshotImportReview -> "SCREENSHOT_IMPORT_REVIEW"
    is AppDestination.CaseDetail -> "CASE_DETAIL"
    is AppDestination.EditCase -> "EDIT_CASE"
    is AppDestination.AddBirthTimeCandidate -> "ADD_BIRTH_TIME_CANDIDATE"
    is AppDestination.EditMetadata -> "EDIT_METADATA"
    is AppDestination.EditTextRecord -> "EDIT_TEXT_RECORD"
    is AppDestination.EditEvent -> "EDIT_EVENT"
}

private fun String?.diagnosticCode(): String {
    if (this == null) return "NONE"
    val stableCodes = PARENTHESIZED_DIAGNOSTIC_CODE.findAll(this)
        .map { match -> match.groupValues[1] }
        .distinct()
        .take(4)
        .toList()
    return stableCodes.joinToString(",").ifEmpty { "PRESENT_REDACTED" }
}

private val PARENTHESIZED_DIAGNOSTIC_CODE =
    Regex("[（(]([A-Z][A-Z0-9_]{2,})[）)]")
