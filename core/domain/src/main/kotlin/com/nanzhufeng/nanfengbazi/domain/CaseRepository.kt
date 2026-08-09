package com.nanzhufeng.nanfengbazi.domain

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
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
    val advancedFilter: CaseAdvancedFilter = CaseAdvancedFilter(),
)

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

    suspend fun listGroups(): List<CaseGroup> = search(
        CaseSearchRequest(visibility = CaseVisibility.ALL),
    ).flatMap { it.groups }.distinctBy { it.id }.sortedBy { it.name }

    suspend fun createGroup(name: String): CaseGroup? = null

    suspend fun renameGroup(groupId: String, name: String): Boolean = false

    suspend fun reorderGroups(groupIds: List<String>): Boolean = false

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
