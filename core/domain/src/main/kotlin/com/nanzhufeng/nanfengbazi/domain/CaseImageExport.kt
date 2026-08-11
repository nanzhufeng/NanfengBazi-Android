package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase

const val CASE_IMAGE_DOCUMENT_VERSION: Int = 3

enum class CaseImageExportScope {
    CURRENT_DETAIL_PAGES,
}

enum class CaseImageDeliveryMode {
    SAVE_TO_SYSTEM_FILE,
    SHARE_LONG_IMAGE,
}

data class CaseImageExportInput(
    val caseData: BaziCase,
    val documentVersion: Int = CASE_IMAGE_DOCUMENT_VERSION,
    val scope: CaseImageExportScope = CaseImageExportScope.CURRENT_DETAIL_PAGES,
)

/**
 * 真实页面长截图的稳定身份元数据。
 *
 * 这里不包含任何展示字段或区块；用户可见的图片排版只能来自当前 Compose 详情页面。
 */
data class CaseImageCaptureFacts(
    val documentVersion: Int,
    val scope: CaseImageExportScope,
    val caseId: String,
    val caseRevision: Long,
    val adoptedSnapshotId: String,
    val suggestedFileStem: String,
) {
    init {
        require(documentVersion > 0) { "图片文档版本必须大于零" }
        require(caseId.isNotBlank()) { "图片导出必须关联命例" }
        require(adoptedSnapshotId.isNotBlank()) { "图片导出必须关联采用快照" }
        require(suggestedFileStem.isNotBlank()) { "建议文件名不能为空" }
    }
}

enum class CaseImageExportErrorCode {
    UNSUPPORTED_DOCUMENT_VERSION,
    NO_ADOPTED_SNAPSHOT,
    MULTIPLE_ADOPTED_SNAPSHOTS,
    CONTENT_TOO_LARGE,
    RENDER_FAILED,
    OUTPUT_UNAVAILABLE,
    WRITE_FAILED,
    USER_CANCELLED,
    NO_SHARE_TARGET,
    SHARE_LAUNCH_FAILED,
}

data class CaseImageExportFailure(
    val code: CaseImageExportErrorCode,
    val message: String,
) {
    init {
        require(message.isNotBlank()) { "图片导出失败说明不能为空" }
    }
}

sealed interface CaseImageFactsResult {
    data class Prepared(
        val facts: CaseImageCaptureFacts,
    ) : CaseImageFactsResult

    data class Rejected(
        val failure: CaseImageExportFailure,
    ) : CaseImageFactsResult
}

data class RenderedCaseImage(
    val facts: CaseImageCaptureFacts,
    val mimeType: String,
    val fileExtension: String,
    val bytes: ByteArray,
    val widthPixels: Int,
    val heightPixels: Int,
    val sha256: String,
) {
    init {
        require(mimeType.isNotBlank() && fileExtension.isNotBlank())
        require(bytes.isNotEmpty()) { "页面长截图字节不能为空" }
        require(widthPixels > 0 && heightPixels > 0) { "页面长截图尺寸无效" }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "页面长截图哈希无效" }
    }
}

object CaseImageExportContract {
    fun prepare(input: CaseImageExportInput): CaseImageFactsResult {
        if (input.documentVersion != CASE_IMAGE_DOCUMENT_VERSION) {
            return CaseImageFactsResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.UNSUPPORTED_DOCUMENT_VERSION,
                    "暂不支持图片文档版本 ${input.documentVersion}。",
                ),
            )
        }
        val adoptedSnapshots = input.caseData.calculationSnapshots.filter { it.adopted }
        if (adoptedSnapshots.isEmpty()) {
            return CaseImageFactsResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.NO_ADOPTED_SNAPSHOT,
                    "当前命例没有唯一已采用的计算快照。",
                ),
            )
        }
        if (adoptedSnapshots.size > 1) {
            return CaseImageFactsResult.Rejected(
                CaseImageExportFailure(
                    CaseImageExportErrorCode.MULTIPLE_ADOPTED_SNAPSHOTS,
                    "当前命例存在多个已采用快照，无法确定截图对应版本。",
                ),
            )
        }
        val adopted = adoptedSnapshots.single()
        val fileStem = (input.caseData.name.value ?: input.caseData.alias)
            .trim()
            .take(48)
            .ifBlank { "命例" }
        return CaseImageFactsResult.Prepared(
            CaseImageCaptureFacts(
                documentVersion = input.documentVersion,
                scope = input.scope,
                caseId = input.caseData.id,
                caseRevision = input.caseData.revision,
                adoptedSnapshotId = adopted.id,
                suggestedFileStem = fileStem,
            ),
        )
    }
}
