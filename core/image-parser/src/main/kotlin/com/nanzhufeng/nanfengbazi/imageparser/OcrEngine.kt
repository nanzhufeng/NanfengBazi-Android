package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType

enum class OcrExecutionMode {
    OFFLINE,
    ONLINE,
}

data class OcrImageInput(
    val image: ImportImageRef,
    val bytes: ByteArray,
) {
    init {
        require(bytes.isNotEmpty()) { "OCR 输入图片不能为空" }
        require(bytes.size.toLong() == image.byteSize) { "OCR 输入图片大小与来源证据不一致" }
    }
}

interface OcrEngine {
    val engineId: String
    val engineVersion: String
    val executionMode: OcrExecutionMode

    suspend fun recognize(input: OcrImageInput): OcrDocument
}

fun interface OcrDocumentRefiner {
    suspend fun refine(
        input: OcrImageInput,
        pageType: WenzhenPageType,
        initialDocument: OcrDocument,
    ): OcrDocument
}

object NoOpOcrDocumentRefiner : OcrDocumentRefiner {
    override suspend fun refine(
        input: OcrImageInput,
        pageType: WenzhenPageType,
        initialDocument: OcrDocument,
    ): OcrDocument = initialDocument
}

fun interface ImportImageContentReader {
    suspend fun readBytes(image: ImportImageRef): ByteArray
}
