package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import java.time.Instant

enum class CaseSortOrder {
    NAME_ASC,
    LAST_VIEWED_DESC,
    UPDATED_DESC,
    CREATED_DESC,
    BIRTH_ASC,
}

data class PillarCharacterFilter(
    val stem: Char? = null,
    val branch: Char? = null,
    val stemTenGod: String? = null,
    val branchTenGod: String? = null,
) {
    val isActive: Boolean
        get() = stem != null || branch != null || stemTenGod != null || branchTenGod != null
}

data class FourPillarsSearchFilter(
    val year: PillarCharacterFilter = PillarCharacterFilter(),
    val month: PillarCharacterFilter = PillarCharacterFilter(),
    val day: PillarCharacterFilter = PillarCharacterFilter(),
    val hour: PillarCharacterFilter = PillarCharacterFilter(),
) {
    val activeCount: Int get() = listOf(year, month, day, hour).count { it.isActive }
}

data class CaseAdvancedFilter(
    val sex: com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection? = null,
    val ganZhi: Set<Char> = emptySet(),
    val fourPillars: FourPillarsSearchFilter = FourPillarsSearchFilter(),
    val birthRegion: String = "",
    val seasonalWuxingStates: Set<String> = emptySet(),
    val shenSha: Set<String> = emptySet(),
) {
    val activeCategoryCount: Int
        get() = listOf(
            sex != null,
            ganZhi.isNotEmpty(),
            fourPillars.activeCount > 0,
            birthRegion.isNotBlank(),
            seasonalWuxingStates.isNotEmpty(),
            shenSha.isNotEmpty(),
        ).count { it }
}

object SeasonalWuxingStateResolver {
    fun resolve(monthBranch: Char): Set<String> = when (monthBranch) {
        '寅', '卯', '辰' -> setOf("木旺", "火相", "水休", "金囚", "土死")
        '巳', '午', '未' -> setOf("火旺", "土相", "木休", "水囚", "金死")
        '申', '酉', '戌' -> setOf("金旺", "水相", "土休", "火囚", "木死")
        '亥', '子', '丑' -> setOf("水旺", "木相", "金休", "土囚", "火死")
        else -> emptySet()
    }
}

enum class CaseVisibility {
    ACTIVE,
    TRASHED,
    ALL,
}

data class CaseSearchRequest(
    val query: String = "",
    val groupId: String? = null,
    val tagId: String? = null,
    val sortOrder: CaseSortOrder = CaseSortOrder.UPDATED_DESC,
    val visibility: CaseVisibility = CaseVisibility.ACTIVE,
    val libraryType: CaseLibraryType? = null,
    val advancedFilter: CaseAdvancedFilter = CaseAdvancedFilter(),
)

/** Applies a record-list query to an already loaded catalog. */
fun List<CaseSummary>.searchCases(request: CaseSearchRequest): List<CaseSummary> {
    val query = request.query.trim()
    return asSequence()
        .filter { summary ->
            when (request.visibility) {
                CaseVisibility.ACTIVE -> summary.deletedAt == null
                CaseVisibility.TRASHED -> summary.deletedAt != null
                CaseVisibility.ALL -> true
            }
        }
        .filter { request.libraryType == null || it.libraryType == request.libraryType }
        .filter { summary -> request.groupId == null || summary.groups.any { it.id == request.groupId } }
        .filter { summary -> request.tagId == null || summary.tags.any { it.id == request.tagId } }
        .filter { query.isEmpty() || it.matchesQuery(query) }
        .filter { it.matchesAdvancedFilter(request.advancedFilter) }
        .sortedWith(request.sortOrder.summaryComparator())
        .toList()
}

private fun CaseSummary.matchesAdvancedFilter(filter: CaseAdvancedFilter): Boolean {
    if (filter.sex != null && sexForFortuneDirection != filter.sex) return false
    val pillarValues = fourPillars?.let { listOf(it.year, it.month, it.day, it.hour) }.orEmpty()
    if (filter.ganZhi.isNotEmpty() && pillarValues.none { pillar -> pillar.any(filter.ganZhi::contains) }) {
        return false
    }
    val requestedPillars = listOf(
        filter.fourPillars.year,
        filter.fourPillars.month,
        filter.fourPillars.day,
        filter.fourPillars.hour,
    )
    if (requestedPillars.any { it.isActive } && pillarValues.size != requestedPillars.size) return false
    if (requestedPillars.zip(pillarValues).withIndex().any { (index, pair) ->
            val (requested, actual) = pair
            !actual.matchesPillarFilter(
                filter = requested,
                stemTenGod = pillarStemTenGods.getOrNull(index),
                branchTenGod = pillarBranchTenGods.getOrNull(index),
            )
        }
    ) return false
    if (
        filter.birthRegion.isNotBlank() &&
        !birthInput.locationName.orEmpty().trim()
            .equals(filter.birthRegion.trim(), ignoreCase = true)
    ) return false
    if (
        filter.seasonalWuxingStates.isNotEmpty() &&
        seasonalWuxingStates.intersect(filter.seasonalWuxingStates).isEmpty()
    ) return false
    if (filter.shenSha.isNotEmpty() && shenShaNames.intersect(filter.shenSha).isEmpty()) return false
    return true
}

private fun String.matchesPillarFilter(
    filter: PillarCharacterFilter,
    stemTenGod: String?,
    branchTenGod: String?,
): Boolean =
    (!filter.isActive) ||
        (filter.stem == null || firstOrNull() == filter.stem) &&
        (filter.branch == null || getOrNull(1) == filter.branch) &&
        (filter.stemTenGod == null || stemTenGod == filter.stemTenGod) &&
        (filter.branchTenGod == null || branchTenGod == filter.branchTenGod)

private fun CaseSummary.matchesQuery(query: String): Boolean {
    if (alias.contains(query, ignoreCase = true)) return true
    if (name.value?.contains(query, ignoreCase = true) == true) return true
    val pillars = fourPillars ?: return false
    return listOf(pillars.year, pillars.month, pillars.day, pillars.hour)
        .any { it.contains(query, ignoreCase = true) }
}

private fun CaseSortOrder.summaryComparator(): Comparator<CaseSummary> {
    val selected = when (this) {
        CaseSortOrder.NAME_ASC ->
            Comparator { left, right ->
                compareCaseNames(left.displayName(), right.displayName())
            }
        CaseSortOrder.LAST_VIEWED_DESC ->
            compareByDescending<CaseSummary> { it.lastViewedAt }.thenByDescending { it.updatedAt }
        CaseSortOrder.UPDATED_DESC -> compareByDescending { it.updatedAt }
        CaseSortOrder.CREATED_DESC -> compareByDescending { it.createdAt }
        CaseSortOrder.BIRTH_ASC -> compareBy { it.birthSortKey() }
    }
    return compareByDescending<CaseSummary> { it.isPinned }
        .then(selected)
        .thenBy { it.id }
}

private fun CaseSummary.displayName(): String = name.value?.ifBlank { null } ?: alias

private fun CaseSummary.birthSortKey(): String =
    canonicalSolarDateTime?.toSortKey() ?: birthInput.rawDateTimeSortKey()

private fun BirthInput.rawDateTimeSortKey(): String {
    val dateTime = when (val input = calendarInput) {
        is com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Solar -> input.dateTime
        is com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput.Lunar ->
            CivilDateTime(
                year = input.dateTime.year,
                month = input.dateTime.month,
                day = input.dateTime.day,
                hour = input.dateTime.hour,
                minute = input.dateTime.minute,
                second = input.dateTime.second,
            )
    }
    return dateTime.toSortKey()
}

private fun CivilDateTime.toSortKey(): String =
    "%04d%02d%02d%02d%02d%02d".format(year, month, day, hour, minute, second)

enum class DuplicateReason {
    SAME_BIRTH_INPUT,
    SAME_FOUR_PILLARS,
}

data class DuplicateCaseCandidate(
    val summary: CaseSummary,
    val reasons: Set<DuplicateReason>,
)

interface CaseRepository {
    suspend fun save(
        case: BaziCase,
        expectedRevision: Long?,
    ): CaseWriteResult

    suspend fun findById(id: String): BaziCase?

    suspend fun search(request: CaseSearchRequest = CaseSearchRequest()): List<CaseSummary>

    /**
     * Resolves several list projections from one repository snapshot when supported.
     *
     * The default keeps compatibility with lightweight and test repositories. Persistent
     * repositories can override this to avoid repeating the same full-catalog read for every
     * projection needed by a single screen refresh.
     */
    suspend fun searchBatch(requests: List<CaseSearchRequest>): List<List<CaseSummary>> =
        requests.map { request -> search(request) }

    suspend fun findDuplicateCandidates(
        birthInput: BirthInput,
        fourPillars: FourPillars?,
        canonicalSolarDateTime: CivilDateTime? = null,
        excludeCaseId: String? = null,
    ): List<DuplicateCaseCandidate>

    suspend fun markViewed(
        caseId: String,
        viewedAt: Instant,
    ): Boolean

    suspend fun listGroups(libraryType: CaseLibraryType? = null): List<CaseGroup> = search(
        CaseSearchRequest(visibility = CaseVisibility.ALL, libraryType = libraryType),
    ).flatMap { it.groups }.distinctBy { it.id }.sortedBy { it.name }

    suspend fun createGroup(
        name: String,
        libraryType: CaseLibraryType = CaseLibraryType.USER,
    ): CaseGroup? = null

    suspend fun renameGroup(groupId: String, name: String): Boolean = false

    suspend fun reorderGroups(
        groupIds: List<String>,
        libraryType: CaseLibraryType = CaseLibraryType.USER,
    ): Boolean = false

    suspend fun deleteGroup(groupId: String): Boolean = false

    suspend fun setCasesPinned(
        caseIds: Set<String>,
        pinned: Boolean,
        updatedAt: Instant,
    ): Int = 0

    suspend fun moveCasesToTrash(
        caseIds: Set<String>,
        deletedAt: Instant,
    ): Int = 0

    /** Permanently removes only cases that are already in the trash. */
    suspend fun deleteTrashedCasesPermanently(caseIds: Set<String>): Int = 0
}

sealed interface CaseWriteResult {
    data class Created(
        val caseId: String,
        val revision: Long,
    ) : CaseWriteResult

    data class Updated(
        val caseId: String,
        val revision: Long,
    ) : CaseWriteResult

    data class AlreadyExists(
        val caseId: String,
        val revision: Long,
    ) : CaseWriteResult

    data class RevisionConflict(
        val caseId: String,
        val expectedRevision: Long,
        val actualRevision: Long,
    ) : CaseWriteResult
}
