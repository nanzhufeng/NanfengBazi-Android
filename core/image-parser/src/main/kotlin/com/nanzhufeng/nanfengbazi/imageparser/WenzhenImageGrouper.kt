package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import java.security.MessageDigest

data class WenzhenIdentityEvidence(
    val explicitName: String?,
    val solarBirthDate: String?,
    val fourPillars: String?,
)

class WenzhenIdentityExtractor {
    fun extract(document: OcrDocument): WenzhenIdentityEvidence {
        val text = document.rawText
        return WenzhenIdentityEvidence(
            explicitName = NAME_PATTERN.find(text)?.groupValues?.get(1)?.trim(),
            solarBirthDate = extractSolarBirthDate(text),
            fourPillars = extractFourPillars(text),
        )
    }

    private fun extractSolarBirthDate(text: String): String? {
        val match = SOLAR_DATE_PATTERN.find(text) ?: return null
        val year = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()
        if (year !in 1800..2200 || month !in 1..12 || day !in 1..31) return null
        return "%04d-%02d-%02d".format(year, month, day)
    }

    private fun extractFourPillars(text: String): String? {
        PAIRED_PILLARS_PATTERN.find(text)?.let { match ->
            return (1..4).joinToString(" ") { match.groupValues[it] }
        }
        val lines = text.lines().map { it.trim() }.filter(String::isNotEmpty)
        lines.forEachIndexed { index, line ->
            val stems = STEM_ROW_PATTERN.find(line)?.groupValues?.drop(1) ?: return@forEachIndexed
            val branches = lines.drop(index + 1)
                .take(3)
                .firstNotNullOfOrNull { candidate ->
                    BRANCH_ROW_PATTERN.find(candidate)?.groupValues?.drop(1)
                } ?: return@forEachIndexed
            return stems.zip(branches).joinToString(" ") { (stem, branch) -> stem + branch }
        }
        return null
    }

    private companion object {
        private const val STEMS = "甲乙丙丁戊己庚辛壬癸"
        private const val BRANCHES = "子丑寅卯辰巳午未申酉戌亥"
        val NAME_PATTERN = Regex("(?:姓名|命主)\\s*[:：]\\s*([\\p{L}·]{1,20})")
        val SOLAR_DATE_PATTERN = Regex(
            "(?:阳历|公历)\\s*[:：]?\\s*(\\d{4})[年./-](\\d{1,2})[月./-](\\d{1,2})日?",
        )
        val PAIRED_PILLARS_PATTERN = Regex(
            "([$STEMS][$BRANCHES])\\s+" +
                "([$STEMS][$BRANCHES])\\s+" +
                "([$STEMS][$BRANCHES])\\s+" +
                "([$STEMS][$BRANCHES])",
        )
        val STEM_ROW_PATTERN = Regex(
            "([$STEMS])\\s+([$STEMS])\\s+([$STEMS])\\s+([$STEMS])",
        )
        val BRANCH_ROW_PATTERN = Regex(
            "([$BRANCHES])\\s+([$BRANCHES])\\s+([$BRANCHES])\\s+([$BRANCHES])",
        )
    }
}

class WenzhenImageGrouper(
    private val identityExtractor: WenzhenIdentityExtractor = WenzhenIdentityExtractor(),
) {
    fun group(
        images: List<ImportImageRef>,
        documents: List<OcrDocument>,
    ): List<ImportCaseCandidate> {
        val documentByImageId = documents.associateBy(OcrDocument::imageId)
        val nodes = images.map { image ->
            GroupingNode(
                image = image,
                identity = documentByImageId[image.id]?.let(identityExtractor::extract),
            )
        }
        val groups = mutableListOf<MutableList<GroupingNode>>()
        nodes.forEach { node ->
            val compatibleGroups = groups.filter { group ->
                group.all { existing -> node.canGroupWith(existing) }
            }
            if (compatibleGroups.size == 1) {
                compatibleGroups.single() += node
            } else {
                groups += mutableListOf(node)
            }
        }
        return groups
            .map { groupedNodes ->
                val imageIds = groupedNodes.map { it.image.id }.sorted()
                ImportCaseCandidate(
                    id = stableCandidateId(imageIds),
                    imageIds = imageIds,
                    suggestedAlias = groupedNodes
                        .mapNotNull { it.identity?.explicitName }
                        .distinct()
                        .singleOrNull(),
                    groupingConfidence = if (groupedNodes.size > 1) {
                        groupedNodes.pairwiseMinimumConfidence()
                    } else {
                        null
                    },
                    requiresReview = true,
                )
            }
            .sortedBy { it.imageIds.first() }
    }

    private fun GroupingNode.canGroupWith(other: GroupingNode): Boolean {
        if (
            image.pageType == WenzhenPageType.USER_LIST ||
            other.image.pageType == WenzhenPageType.USER_LIST
        ) {
            return false
        }
        val first = identity ?: return false
        val second = other.identity ?: return false
        if (first.hasConflictWith(second)) return false
        return first.matchWeight(second) >= REQUIRED_MATCH_WEIGHT
    }

    private fun WenzhenIdentityEvidence.hasConflictWith(other: WenzhenIdentityEvidence): Boolean =
        (fourPillars != null && other.fourPillars != null && fourPillars != other.fourPillars) ||
            (
                solarBirthDate != null &&
                    other.solarBirthDate != null &&
                    solarBirthDate != other.solarBirthDate
                ) ||
            (
                explicitName != null &&
                    other.explicitName != null &&
                    explicitName != other.explicitName
                )

    private fun WenzhenIdentityEvidence.matchWeight(other: WenzhenIdentityEvidence): Int {
        var weight = 0
        if (fourPillars != null && fourPillars == other.fourPillars) weight += 2
        if (solarBirthDate != null && solarBirthDate == other.solarBirthDate) weight += 1
        if (explicitName != null && explicitName == other.explicitName) weight += 1
        return weight
    }

    private fun List<GroupingNode>.pairwiseMinimumConfidence(): Float {
        var minimum = 1f
        indices.forEach { first ->
            for (second in first + 1 until size) {
                minimum = minOf(minimum, get(first).identity!!.confidenceWith(get(second).identity!!))
            }
        }
        return minimum
    }

    private fun WenzhenIdentityEvidence.confidenceWith(other: WenzhenIdentityEvidence): Float =
        when {
            fourPillars != null && fourPillars == other.fourPillars &&
                solarBirthDate != null && solarBirthDate == other.solarBirthDate -> 0.98f
            fourPillars != null && fourPillars == other.fourPillars -> 0.92f
            solarBirthDate != null && solarBirthDate == other.solarBirthDate &&
                explicitName != null && explicitName == other.explicitName -> 0.84f
            else -> 0f
        }

    private fun stableCandidateId(imageIds: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(imageIds.joinToString("\u0000").encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        return "candidate-${digest.take(20)}"
    }

    private data class GroupingNode(
        val image: ImportImageRef,
        val identity: WenzhenIdentityEvidence?,
    )

    private companion object {
        const val REQUIRED_MATCH_WEIGHT = 2
    }
}
