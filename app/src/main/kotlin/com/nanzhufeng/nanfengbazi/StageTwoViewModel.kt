package com.nanzhufeng.nanfengbazi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
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
    data class EditCase(val caseId: String) : AppDestination
    data class EditTextRecord(
        val caseId: String,
        val recordId: String?,
    ) : AppDestination
    data class EditEvent(
        val caseId: String,
        val eventId: String?,
    ) : AppDestination
}

class StageTwoNavigator {
    private val stack = mutableListOf<AppDestination>(AppDestination.CaseList)
    val current: AppDestination
        get() = stack.last()

    fun openCreate(): AppDestination {
        return push(AppDestination.CreateCase)
    }

    fun openDetail(caseId: String): AppDestination {
        return push(AppDestination.CaseDetail(caseId))
    }

    fun openEditCase(caseId: String): AppDestination {
        return push(AppDestination.EditCase(caseId))
    }

    fun openTextRecord(caseId: String, recordId: String?): AppDestination {
        return push(AppDestination.EditTextRecord(caseId, recordId))
    }

    fun openEvent(caseId: String, eventId: String?): AppDestination {
        return push(AppDestination.EditEvent(caseId, eventId))
    }

    fun back(): AppDestination {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        return current
    }

    fun backToList(): AppDestination {
        stack.clear()
        stack += AppDestination.CaseList
        return current
    }

    private fun push(destination: AppDestination): AppDestination {
        stack += destination
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
    val editForm: CaseFormState = CaseFormState(),
    val recordDraft: TextRecordDraft = TextRecordDraft(),
    val eventDraft: EventDraft = EventDraft(),
    val mutationSaving: Boolean = false,
    val mutationError: String? = null,
    val message: String? = null,
)

class StageTwoViewModel(
    private val caseRepository: CaseRepository,
    private val createCase: CreateCaseUseCase,
    private val editCase: EditCaseUseCase,
    private val textRecords: TextRecordUseCase,
    private val caseEvents: CaseEventUseCase,
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

    fun openEditCase() {
        val detail = mutableState.value.detail ?: return
        val form = detail.toEditableForm()
        if (form == null) {
            mutableState.update {
                it.copy(message = "当前编辑器暂不支持农历命例，请等待农历能力阶段。")
            }
            return
        }
        mutableState.update {
            it.copy(
                destination = navigator.openEditCase(detail.id),
                editForm = form,
                mutationError = null,
            )
        }
    }

    fun updateEditForm(transform: (CaseFormState) -> CaseFormState) {
        mutableState.update {
            it.copy(editForm = transform(it.editForm), mutationError = null)
        }
    }

    fun saveEditedCase() {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val form = mutableState.value.editForm
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = editCase(detail.id, detail.revision, form)
            finishMutation(
                result = result,
                caseId = detail.id,
                successMessage = "命例资料已重新排盘并保存；旧计算快照仍保留。",
            )
        }
    }

    fun openTextRecord(recordId: String? = null) {
        val detail = mutableState.value.detail ?: return
        val record = recordId?.let { id ->
            detail.textRecords.firstOrNull { it.id == id } ?: return
        }
        mutableState.update {
            it.copy(
                destination = navigator.openTextRecord(detail.id, recordId),
                recordDraft = if (record == null) {
                    TextRecordDraft()
                } else {
                    TextRecordDraft(record.type, record.content)
                },
                mutationError = null,
            )
        }
    }

    fun updateRecordDraft(transform: (TextRecordDraft) -> TextRecordDraft) {
        mutableState.update {
            it.copy(recordDraft = transform(it.recordDraft), mutationError = null)
        }
    }

    fun saveTextRecord(recordId: String?) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val draft = mutableState.value.recordDraft
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = textRecords.save(detail.id, detail.revision, recordId, draft)
            finishMutation(result, detail.id, "记录已保存。")
        }
    }

    fun deleteTextRecord(recordId: String) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = textRecords.delete(detail.id, detail.revision, recordId)
            finishMutation(result, detail.id, "记录已删除。")
        }
    }

    fun openEvent(eventId: String? = null) {
        val detail = mutableState.value.detail ?: return
        val event = eventId?.let { id ->
            detail.events.firstOrNull { it.id == id } ?: return
        }
        mutableState.update {
            it.copy(
                destination = navigator.openEvent(detail.id, eventId),
                eventDraft = if (event == null) {
                    EventDraft()
                } else {
                    EventDraft(
                        year = event.year?.toString().orEmpty(),
                        month = event.month?.toString().orEmpty(),
                        day = event.day?.toString().orEmpty(),
                        status = event.status.orEmpty(),
                        rawText = event.rawText,
                    )
                },
                mutationError = null,
            )
        }
    }

    fun updateEventDraft(transform: (EventDraft) -> EventDraft) {
        mutableState.update {
            it.copy(eventDraft = transform(it.eventDraft), mutationError = null)
        }
    }

    fun saveEvent(eventId: String?) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        val draft = mutableState.value.eventDraft
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = caseEvents.save(detail.id, detail.revision, eventId, draft)
            finishMutation(result, detail.id, "关键事件已保存。")
        }
    }

    fun deleteEvent(eventId: String) {
        val detail = mutableState.value.detail ?: return
        if (mutableState.value.mutationSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(mutationSaving = true, mutationError = null) }
            val result = caseEvents.delete(detail.id, detail.revision, eventId)
            finishMutation(result, detail.id, "关键事件已删除。")
        }
    }

    fun navigateBack() {
        mutableState.update {
            it.copy(
                destination = navigator.back(),
                mutationError = null,
                formError = null,
            )
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

    private suspend fun finishMutation(
        result: CaseMutationResult,
        caseId: String,
        successMessage: String,
    ) {
        when (result) {
            is CaseMutationResult.Saved -> {
                val refreshed = try {
                    caseRepository.findById(caseId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (refreshed == null) {
                    mutableState.update {
                        it.copy(
                            mutationSaving = false,
                            mutationError = "更改已保存，但详情刷新失败。请返回列表后重新打开。",
                        )
                    }
                } else {
                    mutableState.update {
                        it.copy(
                            destination = navigator.back(),
                            detail = refreshed,
                            mutationSaving = false,
                            mutationError = null,
                            message = successMessage,
                        )
                    }
                    refreshCases()
                }
            }
            is CaseMutationResult.ValidationFailed -> mutableState.update {
                it.copy(mutationSaving = false, mutationError = result.message)
            }
            CaseMutationResult.NotFound -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "目标记录不存在，未保存任何更改。",
                )
            }
            is CaseMutationResult.RevisionConflict -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "保存冲突：命例已有较新修订（${result.actualRevision}），" +
                        "当前输入仍保留。请返回详情重新打开后再编辑。",
                )
            }
            is CaseMutationResult.CalculationFailed -> mutableState.update {
                it.copy(
                    mutationSaving = false,
                    mutationError = "重新排盘失败：${result.message} 当前输入仍保留。",
                )
            }
            is CaseMutationResult.StorageFailed -> mutableState.update {
                it.copy(mutationSaving = false, mutationError = result.message)
            }
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
            return StageTwoViewModel(
                caseRepository = container.caseRepository,
                createCase = useCase,
                editCase = EditCaseUseCase(
                    baziEngine = container.baziEngine,
                    caseRepository = container.caseRepository,
                ),
                textRecords = TextRecordUseCase(container.caseRepository),
                caseEvents = CaseEventUseCase(container.caseRepository),
            ) as T
        }
    }
}

private fun BaziCase.toEditableForm(): CaseFormState? {
    val dateTime = (birthInput.calendarInput as? BirthCalendarInput.Solar)?.dateTime
        ?: return null
    return CaseFormState(
        alias = alias,
        name = name.value.orEmpty(),
        sex = sexForFortuneDirection,
        year = dateTime.year.toString(),
        month = dateTime.month.toString(),
        day = dateTime.day.toString(),
        hour = dateTime.hour.toString(),
        minute = dateTime.minute.toString(),
        second = dateTime.second.toString(),
    )
}
