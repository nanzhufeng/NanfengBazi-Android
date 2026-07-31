package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDiagnosticsTest {
    @Test
    fun `诊断包覆盖通用交换备份和截图状态且不包含敏感值`() {
        val sensitive = "张三-JOHN_SECRET-1992-08-24-/private/import/a.jpg"
        val report = buildAppDiagnosticText(
            state = StageTwoUiState(
                destination = AppDestination.CaseDetail(sensitive),
                listError = "读取失败：$sensitive",
                singleCaseExchangeError = "文件失败（PAYLOAD_HASH_MISMATCH）：$sensitive",
                fullBackupError = "恢复失败（SOURCE_CHANGED_AFTER_PREVIEW）：$sensitive",
            ),
            screenshot = ScreenshotImportUiState(
                sessionStatus = ImportStatus.NEEDS_REVIEW,
                parserVersion = "wenzhen-p0-v5",
                completedImageCount = 3,
                failedImageCount = 1,
                caseCandidateCount = 2,
                extractedFieldCount = 18,
                classifiedPageTypes = listOf(
                    WenzhenPageType.BASIC_INFO,
                    WenzhenPageType.BASIC_INFO,
                    WenzhenPageType.FEEDBACK,
                ),
                failureCodes = listOf("OCR_FAILED"),
                failureDiagnosticIds = listOf("diagnostic-1"),
            ),
            appVersion = "0.3.0-alpha56",
            versionCode = 57,
        )

        assertTrue(report.contains("ui.destination=CASE_DETAIL"))
        assertTrue(report.contains("exchange.error=PAYLOAD_HASH_MISMATCH"))
        assertTrue(report.contains("backup.error=SOURCE_CHANGED_AFTER_PREVIEW"))
        assertTrue(report.contains("screenshot.status=NEEDS_REVIEW"))
        assertTrue(report.contains("BASIC_INFO:2"))
        assertTrue(report.contains("screenshot.failure_codes=OCR_FAILED"))
        assertFalse(report.contains(sensitive))
        assertFalse(report.contains("张三"))
        assertFalse(report.contains("JOHN_SECRET"))
        assertFalse(report.contains("1992-08-24"))
        assertFalse(report.contains("/private/import"))
    }
}
