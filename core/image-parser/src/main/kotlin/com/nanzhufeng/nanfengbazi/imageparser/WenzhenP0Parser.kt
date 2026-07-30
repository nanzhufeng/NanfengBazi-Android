package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.EvidenceBoundingBox
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextEvidence
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextType
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.OcrTextBlock
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import java.security.MessageDigest
import java.time.LocalDateTime

data class WenzhenP0ParseResult(
    val fields: List<CaseFieldEvidence>,
    val longTexts: List<ImportedLongTextEvidence>,
    val candidates: List<ImportCaseCandidate>,
)

class WenzhenP0Parser(
    private val identityExtractor: WenzhenIdentityExtractor = WenzhenIdentityExtractor(),
) {
    fun parse(
        images: List<ImportImageRef>,
        documents: List<OcrDocument>,
        groupedCandidates: List<ImportCaseCandidate>,
    ): WenzhenP0ParseResult {
        val documentByImageId = documents.associateBy(OcrDocument::imageId)
        val fields = mutableListOf<CaseFieldEvidence>()
        val longTexts = mutableListOf<ImportedLongTextEvidence>()
        val listCandidatesByImageId = mutableMapOf<String, List<ImportCaseCandidate>>()

        images.forEach { image ->
            val document = documentByImageId[image.id] ?: return@forEach
            if (image.pageType == WenzhenPageType.USER_LIST) {
                val rows = parseUserListRows(image, document)
                fields += rows.flatMap(ParsedUserRow::fields)
                listCandidatesByImageId[image.id] = rows.map(ParsedUserRow::candidate)
            } else {
                fields += parseIdentityFields(image, document)
                if (image.pageType == WenzhenPageType.BASIC_INFO) {
                    fields += parseBasicInfoFields(image, document)
                }
            }
            parseLongText(image, document)?.let(longTexts::add)
        }

        val fieldIdsByImageId = fields.groupBy(CaseFieldEvidence::attachmentId)
            .mapValues { (_, values) -> values.map(CaseFieldEvidence::id) }
        val longTextIdsByImageId = longTexts.groupBy(ImportedLongTextEvidence::imageId)
            .mapValues { (_, values) -> values.map(ImportedLongTextEvidence::id) }
        val candidates = groupedCandidates.flatMap { candidate ->
            val listImageId = candidate.imageIds
                .singleOrNull()
                ?.takeIf(listCandidatesByImageId::containsKey)
            if (listImageId != null) {
                listCandidatesByImageId.getValue(listImageId)
                    .ifEmpty { listOf(candidate) }
            } else {
                listOf(
                    candidate.copy(
                        fieldEvidenceIds = candidate.imageIds
                            .flatMap { fieldIdsByImageId[it].orEmpty() },
                        longTextEvidenceIds = candidate.imageIds
                            .flatMap { longTextIdsByImageId[it].orEmpty() },
                    ),
                )
            }
        }
        return WenzhenP0ParseResult(
            fields = fields,
            longTexts = longTexts,
            candidates = candidates,
        )
    }

    private fun parseUserListRows(
        image: ImportImageRef,
        document: OcrDocument,
    ): List<ParsedUserRow> {
        val orderedBlocks = document.blocks.sortedWith(
            compareBy<OcrTextBlock> { it.boundingBox?.top ?: Int.MAX_VALUE }
                .thenBy { it.boundingBox?.left ?: Int.MAX_VALUE },
        )
        val anchors = orderedBlocks.mapIndexedNotNull { dateIndex, dateBlock ->
            val solarDate = parseSolarDate(dateBlock.text) ?: return@mapIndexedNotNull null
            val dateTop = dateBlock.boundingBox?.top ?: return@mapIndexedNotNull null
            val nameBlock = orderedBlocks
                .take(dateIndex)
                .asReversed()
                .firstOrNull { block ->
                    val bottom = block.boundingBox?.bottom ?: return@firstOrNull false
                    bottom <= dateTop + ROW_VERTICAL_TOLERANCE_PX &&
                        dateTop - bottom <= MAX_NAME_DATE_DISTANCE_PX &&
                        parseNameAndSex(block.text) != null
                } ?: return@mapIndexedNotNull null
            UserRowAnchor(
                nameBlock = nameBlock,
                nameAndSex = requireNotNull(parseNameAndSex(nameBlock.text)),
                dateBlock = dateBlock,
                solarDate = solarDate,
            )
        }
        return anchors.mapIndexed { index, anchor ->
            val rowTop = (anchor.nameBlock.boundingBox?.top ?: 0) - ROW_VERTICAL_TOLERANCE_PX
            val rowBottom = anchors.getOrNull(index + 1)
                ?.nameBlock
                ?.boundingBox
                ?.top
                ?.minus(ROW_VERTICAL_TOLERANCE_PX)
                ?: ((anchor.dateBlock.boundingBox?.bottom ?: rowTop) + LAST_ROW_TAIL_PX)
            val rowBlocks = orderedBlocks.filter { block ->
                val box = block.boundingBox ?: return@filter false
                val center = (box.top + box.bottom) / 2
                center in rowTop..rowBottom
            }
            val rowText = rowBlocks.joinToString("\n", transform = OcrTextBlock::text)
            val identity = identityExtractor.extract(document.copy(rawText = rowText))
            val rowFourPillars = identity.fourPillars ?: extractCompactFourPillars(rowBlocks)
            val evidence = buildList {
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "row-$index",
                        fieldKey = FIELD_ALIAS,
                        rawText = anchor.nameAndSex.first,
                        value = TypedFieldValue.Text(anchor.nameAndSex.first),
                        confidence = anchor.nameBlock.confidence,
                        boundingBox = anchor.nameBlock.boundingBox,
                        parserConfidence = 0.94f,
                    ),
                )
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "row-$index",
                        fieldKey = FIELD_SEX,
                        rawText = anchor.nameAndSex.second,
                        value = TypedFieldValue.Text(anchor.nameAndSex.second),
                        confidence = anchor.nameBlock.confidence,
                        boundingBox = anchor.nameBlock.boundingBox,
                        parserConfidence = 0.92f,
                    ),
                )
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "row-$index",
                        fieldKey = FIELD_SOLAR_DATE,
                        rawText = anchor.dateBlock.text,
                        value = TypedFieldValue.Text(anchor.solarDate),
                        confidence = anchor.dateBlock.confidence,
                        boundingBox = anchor.dateBlock.boundingBox,
                        parserConfidence = 0.96f,
                    ),
                )
                rowFourPillars?.let { pillars ->
                    add(
                        field(
                            image = image,
                            document = document,
                            rowKey = "row-$index",
                            fieldKey = FIELD_FOUR_PILLARS,
                            rawText = pillars,
                            value = TypedFieldValue.FourPillarsValue(pillars.toFourPillars()),
                            confidence = rowBlocks.mapNotNull(OcrTextBlock::confidence)
                                .averageOrNull(),
                            boundingBox = rowBlocks.unionBoundingBox(),
                            parserConfidence = 0.9f,
                        ),
                    )
                } ?: add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "row-$index",
                        fieldKey = FIELD_FOUR_PILLARS,
                        rawText = probablePillarRawText(anchor, rowBlocks),
                        value = null,
                        confidence = rowBlocks.mapNotNull(OcrTextBlock::confidence)
                            .averageOrNull(),
                        boundingBox = rowBlocks.unionBoundingBox(),
                        parserConfidence = 0.25f,
                    ),
                )
            }
            val evidenceIds = evidence.map(CaseFieldEvidence::id)
            ParsedUserRow(
                fields = evidence,
                candidate = ImportCaseCandidate(
                    id = stableId("candidate", image.id, "row-$index", anchor.solarDate),
                    imageIds = listOf(image.id),
                    fieldEvidenceIds = evidenceIds,
                    suggestedAlias = anchor.nameAndSex.first,
                    groupingConfidence = if (rowFourPillars == null) 0.82f else 0.94f,
                    requiresReview = true,
                ),
            )
        }
    }

    private fun parseIdentityFields(
        image: ImportImageRef,
        document: OcrDocument,
    ): List<CaseFieldEvidence> {
        val identity = identityExtractor.extract(document)
        val box = document.blocks.unionBoundingBox()
        val confidence = document.blocks.mapNotNull(OcrTextBlock::confidence).averageOrNull()
        return buildList {
            identity.explicitName?.let { name ->
                add(
                    field(
                        image,
                        document,
                        "page",
                        FIELD_ALIAS,
                        name,
                        TypedFieldValue.Text(name),
                        confidence,
                        box,
                        0.9f,
                    ),
                )
            }
            identity.solarBirthDate?.let { date ->
                add(
                    field(
                        image,
                        document,
                        "page",
                        FIELD_SOLAR_DATE,
                        date,
                        TypedFieldValue.Text(date),
                        confidence,
                        box,
                        0.9f,
                    ),
                )
            }
            identity.fourPillars?.let { pillars ->
                add(
                    field(
                        image,
                        document,
                        "page",
                        FIELD_FOUR_PILLARS,
                        pillars,
                        TypedFieldValue.FourPillarsValue(pillars.toFourPillars()),
                        confidence,
                        box,
                        0.9f,
                    ),
                )
            }
        }
    }

    private fun parseLongText(
        image: ImportImageRef,
        document: OcrDocument,
    ): ImportedLongTextEvidence? {
        val type = when (image.pageType) {
            WenzhenPageType.FEEDBACK -> ImportedLongTextType.OWNER_FEEDBACK
            WenzhenPageType.COMMENTARY -> ImportedLongTextType.MASTER_COMMENTARY
            else -> return null
        }
        return ImportedLongTextEvidence(
            id = stableId("long-text", image.id, type.name),
            imageId = image.id,
            type = type,
            rawText = document.rawText,
            ocrConfidence = document.blocks.mapNotNull(OcrTextBlock::confidence).averageOrNull(),
            parserConfidence = image.pageConfidence ?: 0.8f,
            parserRuleId = PARSER_RULE_ID,
        )
    }

    private fun parseBasicInfoFields(
        image: ImportImageRef,
        document: OcrDocument,
    ): List<CaseFieldEvidence> = buildList {
        document.firstMatch(SEX_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_SEX,
                    rawText = match.value,
                    value = TypedFieldValue.Text(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.94f,
                ),
            )
        }
        document.firstMatch(NAME_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_NAME,
                    rawText = match.value,
                    value = TypedFieldValue.Text(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.94f,
                ),
            )
        }
        document.firstDateTimeMatch(SOLAR_DATETIME_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_SOLAR_DATETIME,
                    rawText = match.rawValue,
                    value = TypedFieldValue.DateTimeValue(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.96f,
                ),
            )
        }
        document.firstMatch(LUNAR_TEXT_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_LUNAR_TEXT,
                    rawText = match.value,
                    value = TypedFieldValue.Text(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.88f,
                ),
            )
        }
        document.firstDateTimeMatch(TRUE_SOLAR_DATETIME_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_TRUE_SOLAR_DATETIME,
                    rawText = match.rawValue,
                    value = TypedFieldValue.DateTimeValue(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.94f,
                ),
            )
        }
        document.firstMatch(LOCATION_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_LOCATION,
                    rawText = match.value,
                    value = TypedFieldValue.Text(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.9f,
                ),
            )
        }
        document.firstCoordinates()?.let { coordinates ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_LATITUDE,
                    rawText = coordinates.latitudeRaw,
                    value = TypedFieldValue.DecimalNumber(
                        coordinates.latitude.toString(),
                    ),
                    confidence = coordinates.block.confidence,
                    boundingBox = coordinates.block.boundingBox,
                    parserConfidence = 0.92f,
                ),
            )
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_LONGITUDE,
                    rawText = coordinates.longitudeRaw,
                    value = TypedFieldValue.DecimalNumber(
                        coordinates.longitude.toString(),
                    ),
                    confidence = coordinates.block.confidence,
                    boundingBox = coordinates.block.boundingBox,
                    parserConfidence = 0.92f,
                ),
            )
        }
        document.firstMatch(CONSTELLATION_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_CONSTELLATION,
                    rawText = match.value,
                    value = TypedFieldValue.Text(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.9f,
                ),
            )
        }
        document.firstMatch(ZODIAC_PATTERN)?.let { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info",
                    fieldKey = FIELD_ZODIAC,
                    rawText = match.value,
                    value = TypedFieldValue.Text(match.value),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.9f,
                ),
            )
        }
    }

    private fun field(
        image: ImportImageRef,
        document: OcrDocument,
        rowKey: String,
        fieldKey: String,
        rawText: String,
        value: TypedFieldValue?,
        confidence: Float?,
        boundingBox: EvidenceBoundingBox?,
        parserConfidence: Float,
    ) = CaseFieldEvidence(
        id = stableId("field", image.id, rowKey, fieldKey),
        attachmentId = image.id,
        fieldKey = fieldKey,
        rawText = rawText,
        normalizedValue = value,
        adoptedValue = null,
        ocrConfidence = confidence,
        parserConfidence = parserConfidence,
        consistencyConfidence = null,
        boundingBox = boundingBox,
        parserRuleId = PARSER_RULE_ID,
        userEdited = false,
        createdAt = document.recognizedAt,
    )

    private fun parseNameAndSex(text: String): Pair<String, String>? {
        val match = NAME_SEX_PATTERN.matchEntire(text.trim()) ?: return null
        return match.groupValues[1].trim() to match.groupValues[2]
    }

    private fun parseSolarDate(text: String): String? {
        val match = SOLAR_DATE_PATTERN.find(text) ?: return null
        val year = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()
        if (year !in 1800..2200 || month !in 1..12 || day !in 1..31) return null
        return "%04d-%02d-%02d".format(year, month, day)
    }

    private fun OcrDocument.firstMatch(pattern: Regex): BlockMatch? =
        blocks.firstNotNullOfOrNull { block ->
            pattern.find(block.text)?.groupValues?.getOrNull(1)
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { BlockMatch(block, it) }
        }

    private fun OcrDocument.firstDateTimeMatch(pattern: Regex): DateTimeBlockMatch? =
        blocks.firstNotNullOfOrNull { block ->
            val match = pattern.find(block.text) ?: return@firstNotNullOfOrNull null
            val dateTime = runCatching {
                LocalDateTime.of(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt(),
                    match.groupValues[4].toInt(),
                    match.groupValues[5].toInt(),
                    match.groupValues[6].ifBlank { "0" }.toInt(),
                )
            }.getOrNull() ?: return@firstNotNullOfOrNull null
            DateTimeBlockMatch(
                block = block,
                rawValue = match.value.substringAfter('：', match.value)
                    .substringAfter(':', match.value)
                    .trim(),
                value = CivilDateTime(
                    year = dateTime.year,
                    month = dateTime.monthValue,
                    day = dateTime.dayOfMonth,
                    hour = dateTime.hour,
                    minute = dateTime.minute,
                    second = dateTime.second,
                ),
            )
        }

    private fun OcrDocument.firstCoordinates(): CoordinatesBlockMatch? =
        blocks.firstNotNullOfOrNull { block ->
            val latitudeMatch = LATITUDE_PATTERN.find(block.text)
                ?: return@firstNotNullOfOrNull null
            val longitudeMatch = LONGITUDE_PATTERN.find(block.text)
                ?: return@firstNotNullOfOrNull null
            val latitudeMagnitude = latitudeMatch.groupValues[2].toDoubleOrNull()
                ?: return@firstNotNullOfOrNull null
            val longitudeMagnitude = longitudeMatch.groupValues[2].toDoubleOrNull()
                ?: return@firstNotNullOfOrNull null
            val latitude = if (latitudeMatch.groupValues[1] == "南纬") {
                -latitudeMagnitude
            } else {
                latitudeMagnitude
            }
            val longitude = if (longitudeMatch.groupValues[1] == "西经") {
                -longitudeMagnitude
            } else {
                longitudeMagnitude
            }
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                return@firstNotNullOfOrNull null
            }
            CoordinatesBlockMatch(
                block = block,
                latitudeRaw = latitudeMatch.value,
                latitude = latitude,
                longitudeRaw = longitudeMatch.value,
                longitude = longitude,
            )
        }

    private fun extractCompactFourPillars(blocks: List<OcrTextBlock>): String? {
        val stems = blocks.firstNotNullOfOrNull { block ->
            STEM_SEQUENCE.find(block.text)?.groupValues?.drop(1)
        } ?: return null
        val branches = blocks.firstNotNullOfOrNull { block ->
            BRANCH_SEQUENCE.find(block.text)?.groupValues?.drop(1)
        } ?: return null
        return stems.zip(branches).joinToString(" ") { (stem, branch) -> stem + branch }
    }

    private fun String.toFourPillars(): FourPillars {
        val values = split(Regex("\\s+"))
        require(values.size == 4) { "四柱必须包含四项" }
        return FourPillars(values[0], values[1], values[2], values[3])
    }

    private fun List<Float>.averageOrNull(): Float? =
        takeIf { it.isNotEmpty() }?.average()?.toFloat()

    private fun List<OcrTextBlock>.unionBoundingBox(): EvidenceBoundingBox? {
        val boxes = mapNotNull(OcrTextBlock::boundingBox)
        if (boxes.isEmpty()) return null
        return EvidenceBoundingBox(
            left = boxes.minOf(EvidenceBoundingBox::left),
            top = boxes.minOf(EvidenceBoundingBox::top),
            right = boxes.maxOf(EvidenceBoundingBox::right),
            bottom = boxes.maxOf(EvidenceBoundingBox::bottom),
        )
    }

    private fun stableId(prefix: String, vararg parts: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("\u0000").encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        return "$prefix-${digest.take(20)}"
    }

    private fun probablePillarRawText(
        anchor: UserRowAnchor,
        rowBlocks: List<OcrTextBlock>,
    ): String {
        val identityRight = maxOf(
            anchor.nameBlock.boundingBox?.right ?: 0,
            anchor.dateBlock.boundingBox?.right ?: 0,
        )
        return rowBlocks
            .filter { block -> (block.boundingBox?.left ?: 0) > identityRight + 32 }
            .joinToString(" / ", transform = OcrTextBlock::text)
            .takeIf(String::isNotBlank)
            ?: rowBlocks.joinToString(" / ", transform = OcrTextBlock::text)
    }

    private data class UserRowAnchor(
        val nameBlock: OcrTextBlock,
        val nameAndSex: Pair<String, String>,
        val dateBlock: OcrTextBlock,
        val solarDate: String,
    )

    private data class ParsedUserRow(
        val fields: List<CaseFieldEvidence>,
        val candidate: ImportCaseCandidate,
    )

    private data class BlockMatch(
        val block: OcrTextBlock,
        val value: String,
    )

    private data class DateTimeBlockMatch(
        val block: OcrTextBlock,
        val rawValue: String,
        val value: CivilDateTime,
    )

    private data class CoordinatesBlockMatch(
        val block: OcrTextBlock,
        val latitudeRaw: String,
        val latitude: Double,
        val longitudeRaw: String,
        val longitude: Double,
    )

    private companion object {
        private const val STEMS = "甲乙丙丁戊己庚辛壬癸"
        private const val BRANCHES = "子丑寅卯辰巳午未申酉戌亥"
        const val PARSER_RULE_ID = "wenzhen-basic-info-parser-v2"
        const val FIELD_ALIAS = "identity.alias"
        const val FIELD_NAME = "identity.name"
        const val FIELD_SEX = "identity.sex"
        const val FIELD_SOLAR_DATE = "birth.solar_date"
        const val FIELD_SOLAR_DATETIME = "birth.solar_datetime"
        const val FIELD_LUNAR_TEXT = "birth.lunar_text"
        const val FIELD_TRUE_SOLAR_DATETIME = "birth.true_solar_datetime"
        const val FIELD_LOCATION = "birth.location"
        const val FIELD_LATITUDE = "birth.latitude"
        const val FIELD_LONGITUDE = "birth.longitude"
        const val FIELD_CONSTELLATION = "identity.constellation"
        const val FIELD_ZODIAC = "identity.zodiac"
        const val FIELD_FOUR_PILLARS = "chart.four_pillars"
        const val MAX_NAME_DATE_DISTANCE_PX = 240
        const val ROW_VERTICAL_TOLERANCE_PX = 24
        const val LAST_ROW_TAIL_PX = 140
        val NAME_SEX_PATTERN = Regex(
            "([\\p{L}\\p{N}·_—-]{1,24})\\s*(男|女)",
        )
        val SOLAR_DATE_PATTERN = Regex(
            "(?:阳历|公历)\\s*[:：]?\\s*(\\d{4})[年./-](\\d{1,2})[月./-](\\d{1,2})日?",
        )
        val NAME_PATTERN = Regex("(?:姓名|命主)\\s*[:：]\\s*([\\p{L}·]{1,20})")
        val SEX_PATTERN = Regex("(?:性别|性別)\\s*[:：]\\s*(男|女)")
        val SOLAR_DATETIME_PATTERN = Regex(
            "(?:阳历|公历)\\s*[:：]?\\s*(\\d{4})[年./-](\\d{1,2})[月./-]" +
                "(\\d{1,2})日?\\s*(\\d{1,2})[:：](\\d{1,2})[:：](\\d{1,2})",
        )
        val TRUE_SOLAR_DATETIME_PATTERN = Regex(
            "(?:真[太大]阳时|真太陽時)\\s*[:：]?\\s*(\\d{4})[年./-](\\d{1,2})[月./-]" +
                "(\\d{1,2})日?\\s*(\\d{1,2})[:：](\\d{1,2})[:：](\\d{1,2})",
        )
        val LUNAR_TEXT_PATTERN = Regex(
            "(?:农历|農曆|阴历|陰曆)\\s*[:：]\\s*([^\\n]{2,48})",
        )
        val LOCATION_PATTERN = Regex(
            "(?:出生地区|出生地區|出生地)\\s*[:：]\\s*([^\\n]{2,80})",
        )
        val LATITUDE_PATTERN = Regex("(北纬|南纬)\\s*[:：]?\\s*(-?\\d{1,2}(?:\\.\\d+)?)")
        val LONGITUDE_PATTERN = Regex("(东经|西经)\\s*[:：]?\\s*(-?\\d{1,3}(?:\\.\\d+)?)")
        val CONSTELLATION_PATTERN = Regex(
            "(?:星座)\\s*[:：]\\s*([^\\s（(]{1,12})",
        )
        val ZODIAC_PATTERN = Regex(
            "(?:属相|屬相|生肖)\\s*[:：]\\s*([^\\s（(]{1,8})",
        )
        val STEM_SEQUENCE = Regex(
            "([$STEMS])\\s*([$STEMS])\\s*([$STEMS])\\s*([$STEMS])",
        )
        val BRANCH_SEQUENCE = Regex(
            "([$BRANCHES])\\s*([$BRANCHES])\\s*([$BRANCHES])\\s*([$BRANCHES])",
        )
    }
}
