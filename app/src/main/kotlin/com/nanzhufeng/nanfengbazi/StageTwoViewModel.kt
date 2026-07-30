package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppDestination {
    data object CaseList : AppDestination
    data object CreateCase : AppDestination
    data class CaseDetail(val caseId: String) : AppDestination
}

class StageTwoNavigator {
    var current: AppDestination = AppDestination.CaseList
        private set

    fun openCreate(): AppDestination {
        current = AppDestination.CreateCase
        return current
    }

    fun openDetail(caseId: String): AppDestination {
        current = AppDestination.CaseDetail(caseId)
        return current
    }

    fun backToList(): AppDestination {
        current = AppDestination.CaseList
        return current
    }
}

data class StageTwoUiState(
    val destination: AppDestination = AppDestination.CaseList,
    val query: String = "",
    val cases: List<CaseSummary> = emptyList(),
    val listLoading: Boolean = false,
    val listError: String? = null,
    val form: CaseFormState = CaseFormState(),
    val formError: String? = null,
    val saving: Boolean = false,
    val detail: BaziCase? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val message: String? = null,
)

class StageTwoViewModel(
    private val caseRepository: CaseRepository,
    private val createCase: CreateCaseUseCase,
    private val navigator: StageTwoNavigator = StageTwoNavigator(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(StageTwoUiState())
    val state: StateFlow<StageTwoUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        refreshCases()
    }

    fun refreshCases() {
        searchJob?.cancel()
        val query = mutableState.value.query
        searchJob = viewModelScope.launch {
            mutableState.update { it.copy(listLoading = true, listError = null) }
            try {
                val cases = caseRepository.search(query)
                mutableState.update {
                    it.copy(cases = cases, listLoading = false, listError = null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        listLoading = false,
                        listError = "命例列表读取失败，请点击重试。",
                    )
                }
            }
        }
    }

    fun updateQuery(query: String) {
        mutableState.update { it.copy(query = query) }
        refreshCases()
    }

    fun openCreate() {
        mutableState.update {
            it.copy(
                destination = navigator.openCreate(),
                formError = null,
                message = null,
            )
        }
    }

    fun updateForm(transform: (CaseFormState) -> CaseFormState) {
        mutableState.update {
            it.copy(form = transform(it.form), formError = null)
        }
    }

    fun submitCase() {
        if (mutableState.value.saving) return
        val form = mutableState.value.form
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, formError = null) }
            when (val result = createCase(form)) {
                is CreateCaseResult.Created -> {
                    mutableState.update {
                        it.copy(
                            destination = navigator.backToList(),
                            query = "",
                            form = CaseFormState(),
                            saving = false,
                            message = "命例已完成排盘并保存。",
                        )
                    }
                    refreshCases()
                }
                is CreateCaseResult.ValidationFailed -> mutableState.update {
                    it.copy(saving = false, formError = result.message)
                }
                is CreateCaseResult.CalculationFailed -> mutableState.update {
                    it.copy(
                        saving = false,
                        formError = "排盘失败：${result.message} 输入内容已保留，可修改后重试。",
                    )
                }
                is CreateCaseResult.AlreadyExists -> mutableState.update {
                    it.copy(
                        saving = false,
                        formError = "保存冲突：该命例已经存在，未覆盖原记录。",
                    )
                }
                is CreateCaseResult.RevisionConflict -> mutableState.update {
                    it.copy(
                        saving = false,
                        formError = "保存冲突：命例已被更新，未覆盖较新的记录。",
                    )
                }
                is CreateCaseResult.StorageFailed -> mutableState.update {
                    it.copy(saving = false, formError = result.message)
                }
            }
        }
    }

    fun openDetail(caseId: String) {
        mutableState.update {
            it.copy(
                destination = navigator.openDetail(caseId),
                detail = null,
                detailLoading = true,
                detailError = null,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                val detail = caseRepository.findById(caseId)
                mutableState.update {
                    if (detail == null) {
                        it.copy(
                            detailLoading = false,
                            detailError = "未找到该命例，记录可能已被移除。",
                        )
                    } else {
                        it.copy(detail = detail, detailLoading = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        detailLoading = false,
                        detailError = "命例详情读取失败，请返回列表后重试。",
                    )
                }
            }
        }
    }

    fun backToList() {
        mutableState.update {
            it.copy(
                destination = navigator.backToList(),
                detail = null,
                detailError = null,
                formError = null,
            )
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val useCase = CreateCaseUseCase(
                baziEngine = container.baziEngine,
                caseRepository = container.caseRepository,
            )
            return StageTwoViewModel(container.caseRepository, useCase) as T
        }
    }
}
