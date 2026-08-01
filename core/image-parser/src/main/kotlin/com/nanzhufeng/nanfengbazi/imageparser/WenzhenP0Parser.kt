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
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenSourceFidelityContract
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Locale

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
                if (image.pageType == WenzhenPageType.BASIC_CHART) {
                    fields += parseBasicChartFields(image, document)
                }
                if (image.pageType == WenzhenPageType.PROFESSIONAL_CHART) {
                    fields += parseProfessionalChartFields(image, document)
                }
                if (image.pageType == WenzhenPageType.FEEDBACK) {
                    fields += parseFeedbackEventFields(image, document)
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
        val anchors = orderedBlocks.mapNotNull { dateBlock ->
            val solarDate = parseSolarDate(dateBlock.text) ?: return@mapNotNull null
            dateBlock.boundingBox ?: return@mapNotNull null
            UserRowDateAnchor(
                dateBlock = dateBlock,
                solarDate = solarDate,
            )
        }.deduplicateNearbyDateAnchors()
        return anchors.mapIndexed { index, anchor ->
            val currentCenter = requireNotNull(anchor.dateBlock.boundingBox).verticalCenter()
            val previousCenter = anchors.getOrNull(index - 1)
                ?.dateBlock
                ?.boundingBox
                ?.verticalCenter()
            val nextCenter = anchors.getOrNull(index + 1)
                ?.dateBlock
                ?.boundingBox
                ?.verticalCenter()
            val rowTop = previousCenter
                ?.let { previous -> (previous + currentCenter) / 2 }
                ?: (currentCenter - (
                    nextCenter?.minus(currentCenter)?.div(2) ?: FIRST_ROW_HEAD_PX
                    )).coerceAtLeast(0)
            val rowBottom = nextCenter
                ?.let { next -> (currentCenter + next) / 2 }
                ?: currentCenter + (
                    previousCenter?.let(currentCenter::minus)?.div(2) ?: LAST_ROW_TAIL_PX
                    )
            val rowBlocks = orderedBlocks.filter { block ->
                val box = block.boundingBox ?: return@filter false
                val center = box.verticalCenter()
                center in rowTop..rowBottom
            }
            val rowText = rowBlocks.joinToString("\n", transform = OcrTextBlock::text)
            val identity = identityExtractor.extract(document.copy(rawText = rowText))
            val nameMatch = resolveUserListIdentity(
                rowBlocks = rowBlocks,
                dateBlock = anchor.dateBlock,
            )
            val identityRawText = rowBlocks
                .filter { block ->
                    val box = block.boundingBox ?: return@filter false
                    box.left <= requireNotNull(anchor.dateBlock.boundingBox).right + 32
                }
                .joinToString(" / ", transform = OcrTextBlock::text)
                .takeIf(String::isNotBlank)
                ?: anchor.dateBlock.text
            val compactFourPillars = extractCompactFourPillars(rowBlocks)
            val pairedFourPillars = extractPairedFourPillars(rowBlocks)
            val spatialFourPillars = extractSpatialFourPillars(rowBlocks)
            val rowFourPillars = if (rowBlocks.hasExplicitUnknownPillarMarker(anchor.dateBlock)) {
                null
            } else {
                compactFourPillars ?: pairedFourPillars ?: spatialFourPillars
            }
            val pillarParserConfidence = when (rowFourPillars) {
                null -> 0.25f
                compactFourPillars, pairedFourPillars -> 0.9f
                else -> 0.72f
            }
            val evidence = buildList {
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "row-$index",
                        fieldKey = FIELD_ALIAS,
                        rawText = nameMatch?.second?.first ?: identityRawText,
                        value = nameMatch?.second?.first?.let(TypedFieldValue::Text),
                        confidence = nameMatch?.first?.confidence,
                        boundingBox = nameMatch?.first?.boundingBox
                            ?: rowBlocks.unionBoundingBox(),
                        parserConfidence = if (nameMatch == null) 0.2f else 0.94f,
                    ),
                )
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "row-$index",
                        fieldKey = FIELD_SEX,
                        rawText = nameMatch?.second?.second ?: identityRawText,
                        value = nameMatch?.second?.second?.let(TypedFieldValue::Text),
                        confidence = nameMatch?.first?.confidence,
                        boundingBox = nameMatch?.first?.boundingBox
                            ?: rowBlocks.unionBoundingBox(),
                        parserConfidence = if (nameMatch == null) 0.2f else 0.92f,
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
                            parserConfidence = pillarParserConfidence,
                        ),
                    )
                } ?: add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "row-$index",
                        fieldKey = FIELD_FOUR_PILLARS,
                        rawText = probablePillarRawText(anchor.dateBlock, rowBlocks),
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
                    suggestedAlias = nameMatch?.second?.first,
                    groupingConfidence = when {
                        nameMatch == null -> 0.62f
                        rowFourPillars == null -> 0.82f
                        else -> 0.94f
                    },
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
        BASIC_INFO_GANZHI_FIELDS.forEach { definition ->
            document.firstMatch(definition.pattern)?.let { match ->
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "basic-info",
                        fieldKey = definition.fieldKey,
                        rawText = match.rawValue,
                        value = TypedFieldValue.Text(match.value),
                        confidence = match.block.confidence,
                        boundingBox = match.block.boundingBox,
                        parserConfidence = 0.92f,
                    ),
                )
            }
        }
        BASIC_INFO_SOURCE_TEXT_FIELDS.forEach { definition ->
            document.firstMatch(definition.pattern)?.let { match ->
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "basic-info-source-only",
                        fieldKey = definition.fieldKey,
                        rawText = match.rawValue,
                        value = TypedFieldValue.Text(match.value),
                        confidence = match.block.confidence,
                        boundingBox = match.block.boundingBox,
                        parserConfidence = 0.88f,
                    ),
                )
            }
        }
        document.sourceOnlyPercentageMatches().forEach { match ->
            add(
                field(
                    image = image,
                    document = document,
                    rowKey = "basic-info-source-only",
                    fieldKey = match.fieldKey,
                    rawText = match.rawValue,
                    value = TypedFieldValue.DecimalNumber(match.canonicalValue),
                    confidence = match.block.confidence,
                    boundingBox = match.block.boundingBox,
                    parserConfidence = 0.9f,
                ),
            )
        }
        val birthAt = document.firstDateTimeMatch(SOLAR_DATETIME_PATTERN)
            ?.value
            ?.toLocalDateTime()
        if (birthAt != null) {
            val terms = document.solarTermMatches()
            terms.filter { it.at <= birthAt }
                .maxByOrNull(SolarTermBlockMatch::at)
                ?.let { term ->
                    add(
                        field(
                            image = image,
                            document = document,
                            rowKey = "basic-info",
                            fieldKey = FIELD_PREVIOUS_JIE,
                            rawText = term.rawText,
                            value = TypedFieldValue.Text(term.canonicalValue),
                            confidence = term.block.confidence,
                            boundingBox = term.block.boundingBox,
                            parserConfidence = 0.94f,
                        ),
                    )
                }
            terms.filter { it.at > birthAt }
                .minByOrNull(SolarTermBlockMatch::at)
                ?.let { term ->
                    add(
                        field(
                            image = image,
                            document = document,
                            rowKey = "basic-info",
                            fieldKey = FIELD_NEXT_JIE,
                            rawText = term.rawText,
                            value = TypedFieldValue.Text(term.canonicalValue),
                            confidence = term.block.confidence,
                            boundingBox = term.block.boundingBox,
                            parserConfidence = 0.94f,
                        ),
                    )
                }
        }
    }

    private fun parseFeedbackEventFields(
        image: ImportImageRef,
        document: OcrDocument,
    ): List<CaseFieldEvidence> {
        val orderedBlocks = document.blocks.sortedWith(
            compareBy<OcrTextBlock> { it.boundingBox?.top ?: Int.MAX_VALUE }
                .thenBy { it.boundingBox?.left ?: Int.MAX_VALUE },
        )
        val anchors = orderedBlocks.mapIndexedNotNull { blockIndex, block ->
            val match = EVENT_HEADING_PATTERN.find(block.text)
                ?: return@mapIndexedNotNull null
            FeedbackEventAnchor(
                blockIndex = blockIndex,
                year = match.groupValues[1].toInt(),
                stemBranch = match.groupValues[2].ifBlank { null },
                inlineText = block.text.substring(match.range.last + 1).trim(),
            )
        }
        return anchors.mapIndexedNotNull { eventIndex, anchor ->
            val nextBlockIndex = anchors.getOrNull(eventIndex + 1)?.blockIndex
                ?: orderedBlocks.size
            val eventBlocks = orderedBlocks.subList(anchor.blockIndex, nextBlockIndex)
            val normalizedText = buildList {
                anchor.inlineText.takeIf(String::isNotBlank)?.let(::add)
                addAll(eventBlocks.drop(1).map(OcrTextBlock::text))
            }.joinToString("\n").trim()
            if (normalizedText.isEmpty()) return@mapIndexedNotNull null
            val rawText = buildString {
                append(anchor.year)
                append('年')
                anchor.stemBranch?.let {
                    append(' ')
                    append(it)
                }
                append('\n')
                append(normalizedText)
            }
            field(
                image = image,
                document = document,
                rowKey = "feedback-event-$eventIndex",
                fieldKey = "$FIELD_EVENT_PREFIX${anchor.year}.$eventIndex",
                rawText = rawText,
                value = TypedFieldValue.Text(normalizedText),
                confidence = eventBlocks.mapNotNull(OcrTextBlock::confidence).averageOrNull(),
                boundingBox = eventBlocks.unionBoundingBox(),
                parserConfidence = 0.86f,
            )
        }
    }

    private fun parseBasicChartFields(
        image: ImportImageRef,
        document: OcrDocument,
    ): List<CaseFieldEvidence> {
        val chartBlocks = document.blocks.filter { it.boundingBox != null }
        val rowAnchors = CHART_ROW_DEFINITIONS.mapNotNull { definition ->
            chartBlocks.firstNotNullOfOrNull { block ->
                val normalized = block.text.normalizeChartText()
                val matchedLabel = definition.labels.firstOrNull { label ->
                    CHART_ROW_PREFIX_PATTERN.getValue(label).containsMatchIn(normalized)
                } ?: return@firstNotNullOfOrNull null
                ChartRowAnchor(definition, block, matchedLabel)
            }
        }.sortedBy { it.block.boundingBox?.top }
        val terminatorTop = chartBlocks
            .filter { block ->
                CHART_TABLE_TERMINATOR_PATTERN.containsMatchIn(block.text.normalizeChartText())
            }
            .mapNotNull { it.boundingBox?.top }
            .minOrNull()

        return rowAnchors.flatMapIndexed { rowIndex, anchor ->
            val anchorBox = requireNotNull(anchor.block.boundingBox)
            val nextRowTop = rowAnchors.getOrNull(rowIndex + 1)
                ?.block
                ?.boundingBox
                ?.top
            val rowBottom = listOfNotNull(nextRowTop, terminatorTop)
                .filter { it > anchorBox.top }
                .minOrNull()
                ?: Int.MAX_VALUE
            val rowBlocks = chartBlocks.filter { block ->
                val box = requireNotNull(block.boundingBox)
                val centerY = (box.top + box.bottom) / 2
                centerY >= anchorBox.top && centerY < rowBottom
            }
            parseChartRow(image, document, anchor, rowBlocks)
        }
    }

    private fun parseProfessionalChartFields(
        image: ImportImageRef,
        document: OcrDocument,
    ): List<CaseFieldEvidence> {
        val blocks = document.blocks.filter { it.boundingBox != null }
        val headers = findProfessionalColumnAnchors(blocks)
        val pillarFields = if (headers.size == PROFESSIONAL_COLUMN_DEFINITIONS.size) {
            parseProfessionalPillars(image, document, blocks, headers)
        } else {
            emptyList()
        }
        return buildList {
            parseProfessionalObservedAt(image, document)?.let(::add)
            addAll(pillarFields)
        }
    }

    private fun findProfessionalColumnAnchors(
        blocks: List<OcrTextBlock>,
    ): List<ProfessionalColumnAnchor> {
        val candidates = blocks.flatMap { block ->
            val normalized = block.text.normalizeProfessionalText()
            val box = requireNotNull(block.boundingBox)
            PROFESSIONAL_COLUMN_DEFINITIONS.mapNotNull { definition ->
                val start = normalized.indexOf(definition.label)
                if (start < 0) return@mapNotNull null
                val anchorBlock = if (normalized == definition.label) {
                    block
                } else {
                    val textLength = normalized.length.coerceAtLeast(1)
                    val width = (box.right - box.left).coerceAtLeast(textLength)
                    val left = box.left + start * width / textLength
                    val right = (
                        box.left +
                            (start + definition.label.length) * width / textLength
                        ).coerceAtLeast(left + 1)
                        .coerceAtMost(box.right.coerceAtLeast(left + 1))
                    block.copy(
                        id = "${block.id}-professional-${definition.key}",
                        text = definition.label,
                        boundingBox = EvidenceBoundingBox(left, box.top, right, box.bottom),
                    )
                }
                ProfessionalColumnAnchor(definition, anchorBlock)
            }
        }
        val rows = mutableListOf<MutableList<ProfessionalColumnAnchor>>()
        candidates.sortedBy { it.block.centerY() }.forEach { candidate ->
            val matchingRow = rows.lastOrNull()?.takeIf { row ->
                kotlin.math.abs(row.last().block.centerY() - candidate.block.centerY()) <=
                    PROFESSIONAL_HEADER_ROW_TOLERANCE_PX
            }
            if (matchingRow == null) {
                rows += mutableListOf(candidate)
            } else {
                matchingRow += candidate
            }
        }
        val bestRow = rows.maxByOrNull { row ->
            row.map { it.definition.key }.distinct().size
        }.orEmpty()
        return PROFESSIONAL_COLUMN_DEFINITIONS.mapNotNull { definition ->
            bestRow.firstOrNull { it.definition == definition }
        }
    }

    private fun OcrTextBlock.centerY(): Int =
        requireNotNull(boundingBox).let { box -> (box.top + box.bottom) / 2 }

    private fun parseProfessionalObservedAt(
        image: ImportImageRef,
        document: OcrDocument,
    ): CaseFieldEvidence? {
        val sourceBlock = document.blocks.firstOrNull { block ->
            PROFESSIONAL_OBSERVED_DATE_PATTERN.containsMatchIn(
                block.text.normalizeProfessionalText(),
            )
        } ?: return null
        val normalized = sourceBlock.text.normalizeProfessionalText()
        val match = PROFESSIONAL_OBSERVED_DATE_PATTERN.find(normalized) ?: return null
        val representativeHour = PROFESSIONAL_EXPLICIT_TIME_PATTERN.find(normalized)
            ?.let { time ->
                time.groupValues[1].toInt() to time.groupValues[2].toInt()
            }
            ?: PROFESSIONAL_DOUBLE_HOUR_PATTERN.find(normalized)
                ?.groupValues
                ?.get(1)
                ?.let(PROFESSIONAL_DOUBLE_HOUR_START::get)
                ?.let { hour -> hour to 0 }
            ?: return null
        val value = runCatching {
            LocalDateTime.of(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
                representativeHour.first,
                representativeHour.second,
            )
        }.getOrNull() ?: return null
        return field(
            image = image,
            document = document,
            rowKey = "professional-observed-at",
            fieldKey = FIELD_PROFESSIONAL_OBSERVED_AT,
            rawText = sourceBlock.text,
            value = TypedFieldValue.DateTimeValue(
                CivilDateTime(
                    year = value.year,
                    month = value.monthValue,
                    day = value.dayOfMonth,
                    hour = value.hour,
                    minute = value.minute,
                    second = 0,
                ),
            ),
            confidence = sourceBlock.confidence,
            boundingBox = sourceBlock.boundingBox,
            parserConfidence = if (
                PROFESSIONAL_EXPLICIT_TIME_PATTERN.containsMatchIn(normalized)
            ) {
                0.9f
            } else {
                0.78f
            },
        )
    }

    private fun parseProfessionalPillars(
        image: ImportImageRef,
        document: OcrDocument,
        blocks: List<OcrTextBlock>,
        headers: List<ProfessionalColumnAnchor>,
    ): List<CaseFieldEvidence> {
        val pillarBlocks = blocks.flatMap { it.splitProfessionalPillarCells() }
        val orderedHeaders = headers.sortedBy {
            requireNotNull(it.block.boundingBox).let { box -> (box.left + box.right) / 2 }
        }
        val centers = orderedHeaders.map {
            requireNotNull(it.block.boundingBox).let { box -> (box.left + box.right) / 2 }
        }
        val tableTop = orderedHeaders.minOf { requireNotNull(it.block.boundingBox).bottom }
        val headerHeight = orderedHeaders.maxOf {
            requireNotNull(it.block.boundingBox).let { box -> box.bottom - box.top }
        }
        val tableBottom = tableTop + maxOf(
            PROFESSIONAL_PILLAR_SCAN_HEIGHT_PX,
            headerHeight * PROFESSIONAL_PILLAR_SCAN_HEADER_HEIGHT_MULTIPLIER,
        )
        return buildList {
            orderedHeaders.forEachIndexed { index, anchor ->
                val leftBoundary = if (index == 0) {
                    Int.MIN_VALUE
                } else {
                    (centers[index - 1] + centers[index]) / 2
                }
                val rightBoundary = if (index == centers.lastIndex) {
                    Int.MAX_VALUE
                } else {
                    (centers[index] + centers[index + 1]) / 2
                }
                val candidates = pillarBlocks.filter { block ->
                    block.id != anchor.block.id &&
                        requireNotNull(block.boundingBox).let { box ->
                            val centerX = (box.left + box.right) / 2
                            box.top >= tableTop &&
                                box.top <= tableBottom &&
                                centerX in leftBoundary until rightBoundary
                        }
                }.sortedBy { requireNotNull(it.boundingBox).top }
                val parsed = candidates.toProfessionalPillar() ?: return@forEachIndexed
                add(
                    field(
                        image = image,
                        document = document,
                        rowKey = "professional-${anchor.definition.key}",
                        fieldKey = "professional.${anchor.definition.key}",
                        rawText = "${anchor.definition.label}\n${parsed.pillar}",
                        value = TypedFieldValue.Text(parsed.pillar),
                        confidence =
                            parsed.blocks.mapNotNull(OcrTextBlock::confidence).averageOrNull(),
                        boundingBox = (listOf(anchor.block) + parsed.blocks).unionBoundingBox(),
                        parserConfidence = if (parsed.blocks.size == 1) 0.9f else 0.84f,
                    ),
                )
            }
        }
    }

    private fun OcrTextBlock.splitProfessionalPillarCells(): List<OcrTextBlock> {
        val normalized = text.normalizeProfessionalText()
        val tokens = when {
            PROFESSIONAL_PILLAR_SEQUENCE_PATTERN.matches(normalized) ->
                normalized.chunked(2)
            PROFESSIONAL_STEM_SEQUENCE_PATTERN.matches(normalized) ->
                normalized.map(Char::toString)
            PROFESSIONAL_BRANCH_SEQUENCE_PATTERN.matches(normalized) ->
                normalized.map(Char::toString)
            else -> return listOf(this)
        }
        if (tokens.size <= 1) return listOf(this)
        val box = boundingBox ?: return listOf(this)
        val width = (box.right - box.left).coerceAtLeast(tokens.size)
        return tokens.mapIndexed { index, token ->
            val left = box.left + index * width / tokens.size
            val right = (box.left + (index + 1) * width / tokens.size)
                .coerceAtLeast(left + 1)
                .coerceAtMost(box.right.coerceAtLeast(left + 1))
            copy(
                id = "$id-professional-cell-$index",
                text = token,
                boundingBox = EvidenceBoundingBox(left, box.top, right, box.bottom),
            )
        }
    }

    private fun List<OcrTextBlock>.toProfessionalPillar(): ParsedProfessionalPillar? {
        forEach { block ->
            PROFESSIONAL_PILLAR_PATTERN.find(block.text.normalizeProfessionalText())
                ?.value
                ?.let { return ParsedProfessionalPillar(it, listOf(block)) }
        }
        val stem = firstNotNullOfOrNull { block ->
            PROFESSIONAL_STEM_ONLY_PATTERN.matchEntire(block.text.normalizeProfessionalText())
                ?.value
                ?.let { it to block }
        } ?: return null
        val branch = dropWhile { it.id != stem.second.id }
            .drop(1)
            .firstNotNullOfOrNull { block ->
                PROFESSIONAL_BRANCH_ONLY_PATTERN.matchEntire(
                    block.text.normalizeProfessionalText(),
                )?.value?.let { it to block }
            } ?: return null
        return ParsedProfessionalPillar(
            pillar = stem.first + branch.first,
            blocks = listOf(stem.second, branch.second),
        )
    }

    private fun parseChartRow(
        image: ImportImageRef,
        document: OcrDocument,
        anchor: ChartRowAnchor,
        rowBlocks: List<OcrTextBlock>,
    ): List<CaseFieldEvidence> {
        val anchorBox = requireNotNull(anchor.block.boundingBox)
        val valuesByColumn = List(CHART_COLUMN_KEYS.size) { mutableListOf<ChartCellPart>() }
        val valueBlocks = rowBlocks.mapNotNull { block ->
            val normalized = block.text.normalizeChartText()
            val valueText = if (block.id == anchor.block.id) {
                normalized
                    .replaceFirst(CHART_ROW_PREFIX_PATTERN.getValue(anchor.matchedLabel), "")
                    .trim()
            } else {
                normalized.trim()
            }
            valueText.takeIf(String::isNotBlank)?.let { block to it }
        }
        val probableChartRight = rowBlocks
            .mapNotNull { it.boundingBox?.right }
            .maxOrNull()
            ?: anchorBox.right
        val chartLeft = anchorBox.right
        val chartWidth = (probableChartRight - chartLeft).coerceAtLeast(4)

        valueBlocks.forEach { (block, valueText) ->
            val tokens = anchor.definition.tokenizeValues(valueText)
            if (tokens.size >= CHART_COLUMN_KEYS.size &&
                tokens.size % CHART_COLUMN_KEYS.size == 0
            ) {
                tokens.forEachIndexed { index, token ->
                    valuesByColumn[index % CHART_COLUMN_KEYS.size] += ChartCellPart(
                        text = token,
                        block = block,
                    )
                }
            } else {
                val box = requireNotNull(block.boundingBox)
                val centerX = (box.left + box.right) / 2
                val columnIndex = (
                    (centerX - chartLeft).coerceAtLeast(0) *
                        CHART_COLUMN_KEYS.size / chartWidth
                    ).coerceIn(CHART_COLUMN_KEYS.indices)
                valuesByColumn[columnIndex] += ChartCellPart(
                    text = valueText,
                    block = block,
                )
            }
        }
        if (valuesByColumn.any(List<ChartCellPart>::isEmpty)) return emptyList()

        return valuesByColumn.mapIndexed { columnIndex, parts ->
            val columnKey = CHART_COLUMN_KEYS[columnIndex]
            val normalizedText = parts.joinToString("\n", transform = ChartCellPart::text)
            val sourceBlocks = parts.map(ChartCellPart::block).distinctBy(OcrTextBlock::id)
            field(
                image = image,
                document = document,
                rowKey = "chart-${anchor.definition.key}-$columnKey",
                fieldKey = "chart.$columnKey.${anchor.definition.key}",
                rawText = "${anchor.definition.displayLabel}·" +
                    "${CHART_COLUMN_LABELS[columnIndex]}\n$normalizedText",
                value = TypedFieldValue.Text(normalizedText),
                confidence = sourceBlocks.mapNotNull(OcrTextBlock::confidence).averageOrNull(),
                boundingBox = sourceBlocks.unionBoundingBox(),
                parserConfidence = if (parts.size == 1) 0.86f else 0.8f,
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

    private fun resolveUserListIdentity(
        rowBlocks: List<OcrTextBlock>,
        dateBlock: OcrTextBlock,
    ): Pair<OcrTextBlock, Pair<String, String>>? {
        val dateBox = dateBlock.boundingBox ?: return null
        rowBlocks.mapNotNull { block ->
            parseNameAndSex(block.text)?.let { parsed -> block to parsed }
        }.minByOrNull { (block, _) ->
            kotlin.math.abs(requireNotNull(block.boundingBox).verticalCenter() - dateBox.verticalCenter())
        }?.let { return it }

        val sexBlocks = rowBlocks.filter { block ->
            block.text.trim() in setOf("男", "女") && block.boundingBox != null
        }
        return sexBlocks.mapNotNull { sexBlock ->
            val sexBox = requireNotNull(sexBlock.boundingBox)
            val nameBlock = rowBlocks
                .asSequence()
                .filter { candidate ->
                    val box = candidate.boundingBox ?: return@filter false
                    box.right <= sexBox.left + USER_LIST_IDENTITY_HORIZONTAL_TOLERANCE_PX &&
                        box.left <= dateBox.right + USER_LIST_IDENTITY_HORIZONTAL_TOLERANCE_PX &&
                        kotlin.math.abs(box.verticalCenter() - sexBox.verticalCenter()) <=
                        USER_LIST_IDENTITY_BASELINE_TOLERANCE_PX
                }
                .mapNotNull { candidate ->
                    normalizeStandaloneUserListName(candidate.text)?.let { candidate to it }
                }
                .minByOrNull { (candidate, _) ->
                    val box = requireNotNull(candidate.boundingBox)
                    kotlin.math.abs(box.verticalCenter() - sexBox.verticalCenter()) * 10 +
                        kotlin.math.abs(box.right - sexBox.left)
                } ?: return@mapNotNull null
            val combinedBox = listOf(nameBlock.first, sexBlock).unionBoundingBox()
            val combinedBlock = nameBlock.first.copy(
                id = "${nameBlock.first.id}+${sexBlock.id}",
                text = "${nameBlock.second}${sexBlock.text.trim()}",
                confidence = listOfNotNull(nameBlock.first.confidence, sexBlock.confidence)
                    .averageOrNull(),
                boundingBox = combinedBox,
            )
            combinedBlock to (nameBlock.second to sexBlock.text.trim())
        }.minByOrNull { (block, _) ->
            kotlin.math.abs(requireNotNull(block.boundingBox).verticalCenter() - dateBox.verticalCenter())
        }
    }

    private fun normalizeStandaloneUserListName(text: String): String? {
        val normalized = text.trim().replace(Regex("\\s+"), "")
        if (!USER_LIST_NAME_PATTERN.matches(normalized)) return null
        if (SOLAR_DATE_PATTERN.containsMatchIn(normalized)) return null
        return normalized
    }

    private fun List<UserRowDateAnchor>.deduplicateNearbyDateAnchors(): List<UserRowDateAnchor> =
        buildList {
            this@deduplicateNearbyDateAnchors.forEach { candidate ->
                val previous = lastOrNull()
                val isDuplicate = previous?.let {
                    kotlin.math.abs(
                        requireNotNull(it.dateBlock.boundingBox).verticalCenter() -
                            requireNotNull(candidate.dateBlock.boundingBox).verticalCenter(),
                    ) <= USER_LIST_DATE_ANCHOR_DEDUPLICATION_PX
                } == true
                if (!isDuplicate) {
                    add(candidate)
                } else if (candidate.dateBlock.identityEvidenceScore() >
                    requireNotNull(previous).dateBlock.identityEvidenceScore()
                ) {
                    this[lastIndex] = candidate
                }
            }
        }

    private fun OcrTextBlock.identityEvidenceScore(): Int =
        ((confidence ?: 0f) * 1_000).toInt() + text.length

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
            pattern.find(block.text)?.let { match ->
                match.groupValues.getOrNull(1)
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { BlockMatch(block, match.value, it) }
            }
        }

    private fun OcrDocument.sourceOnlyPercentageMatches(): List<PercentageBlockMatch> {
        val matches = buildList {
            blocks.forEach { block ->
                SOURCE_ONLY_PARTY_PERCENT_PATTERNS.forEach { (fieldKey, pattern) ->
                    pattern.find(block.text)?.toPercentageBlockMatch(block, fieldKey)?.let(::add)
                }
                FIVE_ELEMENT_PERCENT_PATTERN.findAll(block.text).forEach { match ->
                    val fieldKey = FIVE_ELEMENT_PERCENT_FIELD_KEYS[match.groupValues[1]]
                        ?: return@forEach
                    match.toPercentageBlockMatch(block, fieldKey, valueGroupIndex = 2)
                        ?.let(::add)
                }
            }
        }
        return matches.distinctBy(PercentageBlockMatch::fieldKey)
    }

    private fun MatchResult.toPercentageBlockMatch(
        block: OcrTextBlock,
        fieldKey: String,
        valueGroupIndex: Int = 1,
    ): PercentageBlockMatch? {
        val value = groupValues[valueGroupIndex].toBigDecimalOrNull() ?: return null
        if (value < java.math.BigDecimal.ZERO || value > java.math.BigDecimal("100")) return null
        return PercentageBlockMatch(
            block = block,
            fieldKey = fieldKey,
            rawValue = this.value,
            canonicalValue = value.stripTrailingZeros().toPlainString(),
        )
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

    private fun OcrDocument.solarTermMatches(): List<SolarTermBlockMatch> =
        blocks.flatMap { block ->
            SOLAR_TERM_DATETIME_PATTERN.findAll(block.text).mapNotNull { match ->
                val at = runCatching {
                    LocalDateTime.of(
                        match.groupValues[2].toInt(),
                        match.groupValues[3].toInt(),
                        match.groupValues[4].toInt(),
                        match.groupValues[5].toInt(),
                        match.groupValues[6].toInt(),
                        match.groupValues[7].ifBlank { "0" }.toInt(),
                    )
                }.getOrNull() ?: return@mapNotNull null
                SolarTermBlockMatch(
                    block = block,
                    rawText = match.value,
                    name = match.groupValues[1],
                    at = at,
                )
            }.toList()
        }

    private fun extractCompactFourPillars(blocks: List<OcrTextBlock>): String? {
        val stems = blocks.firstNotNullOfOrNull { block ->
            STEM_SEQUENCE.find(block.text)?.groupValues?.drop(1)
        } ?: return null
        val branches = blocks.firstNotNullOfOrNull { block ->
            BRANCH_SEQUENCE.find(block.text)?.groupValues?.drop(1)
        } ?: return null
        return stems.zip(branches).toValidatedFourPillarsText()
    }

    private fun extractPairedFourPillars(blocks: List<OcrTextBlock>): String? =
        blocks.firstNotNullOfOrNull { block ->
            PAIRED_PILLAR_SEQUENCE.find(block.text)?.groupValues?.drop(1)
                ?.takeIf { pillars -> pillars.all(SEXAGENARY_CYCLE::contains) }
                ?.joinToString(" ")
        }

    private fun List<OcrTextBlock>.hasExplicitUnknownPillarMarker(
        dateBlock: OcrTextBlock,
    ): Boolean {
        val identityRight = dateBlock.boundingBox?.right ?: return false
        return any { block ->
            (block.boundingBox?.left ?: 0) > identityRight + 32 &&
                block.text.any { it == '*' || it == '＊' }
        }
    }

    private fun extractSpatialFourPillars(blocks: List<OcrTextBlock>): String? {
        val stemColumns = blocks.spatialColumns(STEMS) ?: return null
        val remainingBranchColumns = blocks.spatialColumns(BRANCHES)?.toMutableList()
            ?: return null
        val branchColumns = stemColumns.map { stemColumn ->
            val branchColumn = remainingBranchColumns.minByOrNull { candidate ->
                kotlin.math.abs(candidate.centerX - stemColumn.centerX)
            }?.takeIf { candidate ->
                kotlin.math.abs(candidate.centerX - stemColumn.centerX) <=
                    USER_LIST_PILLAR_COLUMN_TOLERANCE_PX
            } ?: return null
            remainingBranchColumns.remove(branchColumn)
            branchColumn
        }
        return stemColumns.zip(branchColumns).map { (stemVotes, branchVotes) ->
            val ranked = SEXAGENARY_CYCLE.map { pillar ->
                pillar to (stemVotes.votes[pillar[0]] ?: 0) +
                    (branchVotes.votes[pillar[1]] ?: 0)
            }.sortedByDescending(Pair<String, Int>::second)
            val winner = ranked.firstOrNull() ?: return null
            if (winner.second < USER_LIST_PILLAR_MIN_PAIR_VOTES) return null
            if (ranked.getOrNull(1)?.second == winner.second) return null
            winner.first
        }.joinToString(" ")
    }

    private fun <A, B> List<Pair<A, B>>.toValidatedFourPillarsText(): String? {
        val pillars = map { (stem, branch) -> "$stem$branch" }
        return pillars.takeIf { values -> values.all(SEXAGENARY_CYCLE::contains) }
            ?.joinToString(" ")
    }

    private fun List<OcrTextBlock>.spatialColumns(
        alphabet: String,
    ): List<SpatialColumnVotes>? {
        val occurrences = flatMap { block ->
            val box = block.boundingBox ?: return@flatMap emptyList()
            val compact = block.text.filterNot(Char::isWhitespace)
            if (compact.isEmpty()) return@flatMap emptyList()
            val matching = compact.withIndex().filter { (_, char) -> char in alphabet }
            if (matching.isEmpty() || compact.length > 6 || matching.size * 2 < compact.length) {
                return@flatMap emptyList()
            }
            matching.map { (index, char) ->
                val charCenterX = box.left +
                    ((index + 0.5) * (box.right - box.left) / compact.length).toInt()
                SpatialCharacter(char = char, centerX = charCenterX)
            }
        }.sortedBy(SpatialCharacter::centerX)
        if (occurrences.isEmpty()) return null
        val clusters = mutableListOf<MutableList<SpatialCharacter>>()
        occurrences.forEach { occurrence ->
            val nearest = clusters.minByOrNull { cluster ->
                kotlin.math.abs(cluster.map(SpatialCharacter::centerX).average() - occurrence.centerX)
            }
            if (nearest != null && kotlin.math.abs(
                    nearest.map(SpatialCharacter::centerX).average() - occurrence.centerX,
                ) <= USER_LIST_PILLAR_COLUMN_TOLERANCE_PX
            ) {
                nearest += occurrence
            } else {
                clusters += mutableListOf(occurrence)
            }
        }
        val relevant = clusters
            .filter { it.size >= USER_LIST_PILLAR_MIN_COLUMN_VOTES }
        if (relevant.size < 4) return null
        return relevant
            .sortedByDescending(List<SpatialCharacter>::size)
            .take(4)
            .sortedBy { cluster -> cluster.map(SpatialCharacter::centerX).average() }
            .map { cluster ->
                SpatialColumnVotes(
                    centerX = cluster.map(SpatialCharacter::centerX).average(),
                    votes = cluster.groupingBy(SpatialCharacter::char).eachCount(),
                )
            }
    }

    private fun String.toFourPillars(): FourPillars {
        val values = split(Regex("\\s+"))
        require(values.size == 4) { "四柱必须包含四项" }
        return FourPillars(values[0], values[1], values[2], values[3])
    }

    private fun String.normalizeChartText(): String = trim()
        .replace('運', '运')
        .replace('納', '纳')
        .replace('｜', '|')

    private fun String.normalizeProfessionalText(): String = trim()
        .replace("已迷日期", "已选日期")
        .replace('運', '运')
        .replace('時', '时')
        .replace('選', '选')
        .replace('杜', '柱')
        .replace(Regex("\\s+"), "")

    private fun ChartRowDefinition.tokenizeValues(text: String): List<String> {
        val splitValues = text.split(CHART_VALUE_SEPARATOR_PATTERN)
            .filter(String::isNotBlank)
        if (splitValues.size > 1 || valuePattern == null) return splitValues
        val compact = text.replace(CHART_VALUE_SEPARATOR_PATTERN, "")
        val matchedValues = valuePattern.findAll(compact).map(MatchResult::value).toList()
        return matchedValues.takeIf { it.joinToString("") == compact } ?: splitValues
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

    private fun EvidenceBoundingBox.verticalCenter(): Int = (top + bottom) / 2

    private fun stableId(prefix: String, vararg parts: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("\u0000").encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        return "$prefix-${digest.take(20)}"
    }

    private fun probablePillarRawText(
        dateBlock: OcrTextBlock,
        rowBlocks: List<OcrTextBlock>,
    ): String {
        val identityRight = dateBlock.boundingBox?.right ?: 0
        return rowBlocks
            .filter { block -> (block.boundingBox?.left ?: 0) > identityRight + 32 }
            .joinToString(" / ", transform = OcrTextBlock::text)
            .takeIf(String::isNotBlank)
            ?: rowBlocks.joinToString(" / ", transform = OcrTextBlock::text)
    }

    private data class UserRowDateAnchor(
        val dateBlock: OcrTextBlock,
        val solarDate: String,
    )

    private data class ParsedUserRow(
        val fields: List<CaseFieldEvidence>,
        val candidate: ImportCaseCandidate,
    )

    private data class BlockMatch(
        val block: OcrTextBlock,
        val rawValue: String,
        val value: String,
    )

    private data class PercentageBlockMatch(
        val block: OcrTextBlock,
        val fieldKey: String,
        val rawValue: String,
        val canonicalValue: String,
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

    private data class SolarTermBlockMatch(
        val block: OcrTextBlock,
        val rawText: String,
        val name: String,
        val at: LocalDateTime,
    ) {
        val canonicalValue: String
            get() = "$name ${at.format(BASIC_INFO_DATE_TIME_FORMATTER)}"
    }

    private data class BasicInfoFieldDefinition(
        val fieldKey: String,
        val pattern: Regex,
    )

    private data class FeedbackEventAnchor(
        val blockIndex: Int,
        val year: Int,
        val stemBranch: String?,
        val inlineText: String,
    )

    private data class ChartRowDefinition(
        val key: String,
        val displayLabel: String,
        val labels: List<String>,
        val valuePattern: Regex? = null,
    )

    private data class ChartRowAnchor(
        val definition: ChartRowDefinition,
        val block: OcrTextBlock,
        val matchedLabel: String,
    )

    private data class ChartCellPart(
        val text: String,
        val block: OcrTextBlock,
    )

    private data class ProfessionalColumnDefinition(
        val key: String,
        val label: String,
    )

    private data class ProfessionalColumnAnchor(
        val definition: ProfessionalColumnDefinition,
        val block: OcrTextBlock,
    )

    private data class ParsedProfessionalPillar(
        val pillar: String,
        val blocks: List<OcrTextBlock>,
    )

    private companion object {
        private const val STEMS = "甲乙丙丁戊己庚辛壬癸"
        private const val BRANCHES = "子丑寅卯辰巳午未申酉戌亥"
        private const val PROFESSIONAL_HEADER_ROW_TOLERANCE_PX = 48
        private const val PROFESSIONAL_PILLAR_SCAN_HEADER_HEIGHT_MULTIPLIER = 8
        const val PARSER_RULE_ID = "wenzhen-p0-parser-v9"
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
        const val FIELD_PREVIOUS_JIE = "birth.previous_jie"
        const val FIELD_NEXT_JIE = "birth.next_jie"
        const val FIELD_FETAL_ORIGIN = "chart.fetal_origin"
        const val FIELD_FETAL_BREATH = "chart.fetal_breath"
        const val FIELD_OWN_SIGN = "chart.own_sign"
        const val FIELD_BODY_SIGN = "chart.body_sign"
        const val FIELD_FOUR_PILLARS = "chart.four_pillars"
        const val FIELD_PROFESSIONAL_OBSERVED_AT = "professional.observed_at"
        const val FIELD_EVENT_PREFIX = "event.candidate."
        val CHART_COLUMN_KEYS = listOf("year", "month", "day", "hour")
        val CHART_COLUMN_LABELS = listOf("年柱", "月柱", "日柱", "时柱")
        val TEN_GOD_PATTERN = Regex(
            "正印|偏印|比肩|劫财|食神|伤官|正财|偏财|正官|七杀|元男|元女",
        )
        val STEM_ELEMENT_PATTERN = Regex("[$STEMS][金木水火土]")
        val LIFE_STAGE_PATTERN = Regex(
            "长生|沐浴|冠带|临官|帝旺|衰|病|死|墓|绝|胎|养",
        )
        val VOID_PAIR_PATTERN = Regex("[$BRANCHES]{2}")
        val NAYIN_PATTERN = Regex(
            "海中金|炉中火|大林木|路旁土|剑锋金|山头火|涧下水|城头土|" +
                "白蜡金|杨柳木|泉中水|屋上土|霹雳火|松柏木|长流水|沙中金|" +
                "山下火|平地木|壁上土|金箔金|覆灯火|天河水|大驿土|钗钏金|" +
            "桑柘木|大溪水|沙中土|天上火|石榴木|大海水",
        )
        val CHART_ROW_DEFINITIONS = listOf(
            ChartRowDefinition("main_star", "主星", listOf("主星"), TEN_GOD_PATTERN),
            ChartRowDefinition("hidden_stems", "藏干", listOf("藏干"), STEM_ELEMENT_PATTERN),
            ChartRowDefinition("secondary_stars", "副星", listOf("副星"), TEN_GOD_PATTERN),
            ChartRowDefinition("fortune_stage", "星运", listOf("星运"), LIFE_STAGE_PATTERN),
            ChartRowDefinition("self_stage", "自坐", listOf("自坐"), LIFE_STAGE_PATTERN),
            ChartRowDefinition("void", "空亡", listOf("空亡"), VOID_PAIR_PATTERN),
            ChartRowDefinition("nayin", "纳音", listOf("纳音"), NAYIN_PATTERN),
            ChartRowDefinition("spirits", "神煞", listOf("神煞")),
        )
        val CHART_ROW_PREFIX_PATTERN = CHART_ROW_DEFINITIONS
            .flatMap(ChartRowDefinition::labels)
            .associateWith { label -> Regex("^\\s*${Regex.escape(label)}\\s*[:：|]?\\s*") }
        val CHART_VALUE_SEPARATOR_PATTERN = Regex("[\\s|]+")
        val CHART_TABLE_TERMINATOR_PATTERN = Regex(
            "^(?:智能干支图示|AI指令|原局天干|原局地支|原局整柱)",
        )
        val PROFESSIONAL_COLUMN_DEFINITIONS = listOf(
            ProfessionalColumnDefinition("flow_hour", "流时"),
            ProfessionalColumnDefinition("flow_day", "流日"),
            ProfessionalColumnDefinition("flow_month", "流月"),
            ProfessionalColumnDefinition("flow_year", "流年"),
            ProfessionalColumnDefinition("decade", "大运"),
            ProfessionalColumnDefinition("natal_year", "年柱"),
            ProfessionalColumnDefinition("natal_month", "月柱"),
            ProfessionalColumnDefinition("natal_day", "日柱"),
            ProfessionalColumnDefinition("natal_hour", "时柱"),
        )
        val PROFESSIONAL_PILLAR_PATTERN = Regex("[$STEMS][$BRANCHES]")
        val PROFESSIONAL_STEM_ONLY_PATTERN = Regex("[$STEMS]")
        val PROFESSIONAL_BRANCH_ONLY_PATTERN = Regex("[$BRANCHES]")
        val PROFESSIONAL_PILLAR_SEQUENCE_PATTERN = Regex("(?:[$STEMS][$BRANCHES]){2,}")
        val PROFESSIONAL_STEM_SEQUENCE_PATTERN = Regex("[$STEMS]{2,}")
        val PROFESSIONAL_BRANCH_SEQUENCE_PATTERN = Regex("[$BRANCHES]{2,}")
        val PROFESSIONAL_OBSERVED_DATE_PATTERN = Regex(
            "(?:已选日期|选定日期|日期)[:：]?(\\d{4})年(\\d{1,2})月(\\d{1,2})日",
        )
        val PROFESSIONAL_EXPLICIT_TIME_PATTERN = Regex(
            "(?:日|\\s)([01]?\\d|2[0-3])[:：](\\d{2})",
        )
        val PROFESSIONAL_DOUBLE_HOUR_PATTERN = Regex(
            "([子丑寅卯辰巳午未申酉戌亥])时",
        )
        val PROFESSIONAL_DOUBLE_HOUR_START = mapOf(
            "子" to 23,
            "丑" to 1,
            "寅" to 3,
            "卯" to 5,
            "辰" to 7,
            "巳" to 9,
            "午" to 11,
            "未" to 13,
            "申" to 15,
            "酉" to 17,
            "戌" to 19,
            "亥" to 21,
        )
        const val PROFESSIONAL_PILLAR_SCAN_HEIGHT_PX = 220
        const val FIRST_ROW_HEAD_PX = 120
        const val LAST_ROW_TAIL_PX = 140
        const val USER_LIST_DATE_ANCHOR_DEDUPLICATION_PX = 48
        const val USER_LIST_IDENTITY_BASELINE_TOLERANCE_PX = 28
        const val USER_LIST_IDENTITY_HORIZONTAL_TOLERANCE_PX = 36
        const val USER_LIST_PILLAR_COLUMN_TOLERANCE_PX = 22
        const val USER_LIST_PILLAR_MIN_COLUMN_VOTES = 1
        const val USER_LIST_PILLAR_MIN_PAIR_VOTES = 2
        val NAME_SEX_PATTERN = Regex(
            "([\\p{L}\\p{N}·._—()（）-]{1,32})\\s*(男|女)",
        )
        val USER_LIST_NAME_PATTERN = Regex("[\\p{L}\\p{N}·._—()（）-]{1,32}")
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
        val BASIC_INFO_GANZHI_FIELDS = listOf(
            BasicInfoFieldDefinition(
                FIELD_FETAL_ORIGIN,
                Regex("(?:胎元)\\s*[:：]?\\s*([$STEMS][$BRANCHES])"),
            ),
            BasicInfoFieldDefinition(
                FIELD_FETAL_BREATH,
                Regex("(?:胎息)\\s*[:：]?\\s*([$STEMS][$BRANCHES])"),
            ),
            BasicInfoFieldDefinition(
                FIELD_OWN_SIGN,
                Regex("(?:命宫|命宮)\\s*[:：]?\\s*([$STEMS][$BRANCHES])"),
            ),
            BasicInfoFieldDefinition(
                FIELD_BODY_SIGN,
                Regex("(?:身宫|身宮)\\s*[:：]?\\s*([$STEMS][$BRANCHES])"),
            ),
        )
        val BASIC_INFO_SOURCE_TEXT_FIELDS = listOf(
            BasicInfoFieldDefinition(
                WenzhenSourceFidelityContract.FIELD_STAR_LODGE,
                Regex("(?:星宿)\\s*[:：]?\\s*([^\\s]{1,30})"),
            ),
            BasicInfoFieldDefinition(
                WenzhenSourceFidelityContract.FIELD_LIFE_GUA,
                Regex("(?:命卦)\\s*[:：]?\\s*([^\\s]{1,30})"),
            ),
            BasicInfoFieldDefinition(
                WenzhenSourceFidelityContract.FIELD_DAY_MASTER_ATTRIBUTE,
                Regex("(?:日主属性|日主屬性)\\s*[:：]?\\s*([^\\s]{1,20})"),
            ),
            BasicInfoFieldDefinition(
                WenzhenSourceFidelityContract.FIELD_YIN_YANG_ATTRIBUTE,
                Regex("(?:阴阳属性|陰陽屬性)\\s*[:：]?\\s*([^\\s]{1,20})"),
            ),
            BasicInfoFieldDefinition(
                WenzhenSourceFidelityContract.FIELD_USER_STRENGTH,
                Regex("(?:自定旺衰)\\s*[:：]?\\s*([^\\s]{1,20})"),
            ),
            BasicInfoFieldDefinition(
                WenzhenSourceFidelityContract.FIELD_USER_STRUCTURE,
                Regex("(?:自定格局)\\s*[:：]?\\s*([^\\s]{1,30})"),
            ),
        )
        val SOURCE_ONLY_PARTY_PERCENT_PATTERNS = listOf(
            WenzhenSourceFidelityContract.FIELD_SAME_PARTY_PERCENT to
                Regex("(?:同党|同黨)\\s*[:：]?\\s*(\\d{1,3}(?:\\.\\d+)?)\\s*[％%]"),
            WenzhenSourceFidelityContract.FIELD_OPPOSING_PARTY_PERCENT to
                Regex("(?:异党|異黨)\\s*[:：]?\\s*(\\d{1,3}(?:\\.\\d+)?)\\s*[％%]"),
        )
        val FIVE_ELEMENT_PERCENT_PATTERN =
            Regex("([木火土金水])\\s*[:：]?\\s*(\\d{1,3}(?:\\.\\d+)?)\\s*[％%]")
        val FIVE_ELEMENT_PERCENT_FIELD_KEYS = mapOf(
            "木" to WenzhenSourceFidelityContract.FIELD_WOOD_PERCENT,
            "火" to WenzhenSourceFidelityContract.FIELD_FIRE_PERCENT,
            "土" to WenzhenSourceFidelityContract.FIELD_EARTH_PERCENT,
            "金" to WenzhenSourceFidelityContract.FIELD_METAL_PERCENT,
            "水" to WenzhenSourceFidelityContract.FIELD_WATER_PERCENT,
        )
        val SOLAR_TERM_DATETIME_PATTERN = Regex(
            "(立春|惊蛰|清明|立夏|芒种|小暑|立秋|白露|寒露|立冬|大雪|小寒)" +
                "\\s*[:：]?\\s*(\\d{4})[年./-](\\d{1,2})[月./-](\\d{1,2})日?" +
                "\\s*(\\d{1,2})[:：](\\d{1,2})(?:[:：](\\d{1,2}))?",
        )
        val BASIC_INFO_DATE_TIME_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        val EVENT_HEADING_PATTERN = Regex(
            "^\\s*((?:19|20)\\d{2})\\s*年\\s*" +
                "([甲乙丙丁戊己庚辛壬癸][子丑寅卯辰巳午未申酉戌亥])?(?=\\s|$)",
        )
        val STEM_SEQUENCE = Regex(
            "([$STEMS])\\s*([$STEMS])\\s*([$STEMS])\\s*([$STEMS])",
        )
        val BRANCH_SEQUENCE = Regex(
            "([$BRANCHES])\\s*([$BRANCHES])\\s*([$BRANCHES])\\s*([$BRANCHES])",
        )
        val PAIRED_PILLAR_SEQUENCE = Regex(
            "([$STEMS][$BRANCHES])\\s+" +
                "([$STEMS][$BRANCHES])\\s+" +
                "([$STEMS][$BRANCHES])\\s+" +
                "([$STEMS][$BRANCHES])",
        )
        val SEXAGENARY_CYCLE = buildSet {
            repeat(60) { index ->
                add("${STEMS[index % STEMS.length]}${BRANCHES[index % BRANCHES.length]}")
            }
        }
    }

    private data class SpatialCharacter(
        val char: Char,
        val centerX: Int,
    )

    private data class SpatialColumnVotes(
        val centerX: Double,
        val votes: Map<Char, Int>,
    )
}

private fun CivilDateTime.toLocalDateTime(): LocalDateTime = LocalDateTime.of(
    year,
    month,
    day,
    hour,
    minute,
    second,
)
