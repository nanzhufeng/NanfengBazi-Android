package com.nanzhufeng.nanfengbazi.data.exchange

import androidx.room.withTransaction
import com.nanzhufeng.nanfengbazi.data.backup.RESTORED_ATTACHMENTS_DIRECTORY_NAME
import com.nanzhufeng.nanfengbazi.data.backup.RESTORE_JOURNAL_DIRECTORY_NAME
import com.nanzhufeng.nanfengbazi.data.backup.RESTORE_STAGING_DIRECTORY_NAME
import com.nanzhufeng.nanfengbazi.data.backup.RestoreExpectedCase
import com.nanzhufeng.nanfengbazi.data.backup.RestoreJournal
import com.nanzhufeng.nanfengbazi.data.backup.RestoreJournalState
import com.nanzhufeng.nanfengbazi.data.backup.casePayloadSha256
import com.nanzhufeng.nanfengbazi.data.backup.writeRestoreJournal
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.DomainJson
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

interface SingleCaseBundleOperations {
    suspend fun export(
        caseId: String,
        output: OutputStream,
        attachmentRoot: Path,
        appVersion: String,
        protection: SingleCaseProtection,
    ): SingleCaseExportResult

    fun suggestedFileName(case: BaziCase): String

    fun suggestedEncryptedFileName(case: BaziCase): String

    suspend fun preview(
        input: InputStream,
        workRoot: Path,
        password: CharArray? = null,
    ): SingleCasePreviewResult

    suspend fun commitImport(
        preview: SingleCasePreview,
        decision: SingleCaseImportDecision,
        input: InputStream,
        workRoot: Path,
        attachmentRoot: Path,
        password: CharArray? = null,
    ): SingleCaseImportResult

    suspend fun commitMerge(
        plan: SingleCaseMergePlan,
        input: InputStream,
        workRoot: Path,
        attachmentRoot: Path,
        password: CharArray? = null,
    ): SingleCaseImportResult
}

class SingleCaseBundleService(
    private val database: NanfengBaziDatabase,
    private val clock: Clock = Clock.systemUTC(),
    secureRandom: SecureRandom = SecureRandom(),
    passwordKdfIterations: Int = PasswordCrypto.DEFAULT_KDF_ITERATIONS,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) : SingleCaseBundleOperations {
    private val repository: CaseRepository = RoomCaseRepository(database)
    private val exchange = SingleCaseExchangeService(
        repository = repository,
        clock = clock,
        idGenerator = idGenerator,
    )
    private val encryption = SingleCaseBundleEncryption(
        secureRandom,
        passwordKdfIterations,
    )

    override suspend fun export(
        caseId: String,
        output: OutputStream,
        attachmentRoot: Path,
        appVersion: String,
        protection: SingleCaseProtection,
    ): SingleCaseExportResult {
        if (caseId.isBlank()) {
            return rejectedExport("CASE_ID_REQUIRED", "导出单命例必须提供稳定 ID。")
        }
        if (appVersion.isBlank()) {
            return rejectedExport("APP_VERSION_REQUIRED", "单命例包必须记录 App 版本。")
        }
        val caseData = try {
            repository.findById(caseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return rejectedExport("CASE_READ_FAILED", "读取待导出命例失败。")
        } ?: return rejectedExport("CASE_NOT_FOUND", "没有找到要导出的命例。")
        if (caseData.attachments.isEmpty()) {
            return rejectedExport(
                "NO_ATTACHMENTS_TO_BUNDLE",
                "该命例没有图片附件，请使用单命例 JSON 导出。",
            )
        }
        if (protection is SingleCaseProtection.PasswordProtected && protection.password.isEmpty()) {
            return rejectedExport("PASSWORD_REQUIRED", "密码加密命例包必须提供密码。")
        }
        return try {
            val attachmentSources = caseData.attachments.mapIndexed { index, attachment ->
                val source = resolveContained(attachmentRoot, attachment.relativePath)
                    ?: return rejectedExport(
                        "ATTACHMENT_PATH_INVALID",
                        "附件路径不在 App 私有附件目录内。",
                    )
                if (
                    !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) ||
                    Files.size(source) != attachment.byteSize ||
                    sha256(source) != attachment.sha256
                ) {
                    return rejectedExport(
                        "ATTACHMENT_MISMATCH",
                        "附件文件缺失，或大小、SHA-256 与命例记录不一致。",
                    )
                }
                BundleAttachmentSource(
                    entry = SingleCaseBundleAttachmentEntry(
                        attachmentId = attachment.id,
                        path = "$ATTACHMENTS_DIRECTORY/${index.toString().padStart(6, '0')}.bin",
                        byteSize = attachment.byteSize,
                        sha256 = attachment.sha256,
                    ),
                    source = source,
                )
            }
            val document = SingleCaseDocument(
                formatVersion = SingleCaseExchangeService.FORMAT_VERSION,
                appVersion = appVersion,
                databaseSchemaVersion = NanfengBaziDatabase.SCHEMA_VERSION,
                exportedAt = clock.instant().toString(),
                attachmentMode = SingleCaseAttachmentMode.BUNDLED_BINARIES,
                payloadSha256 = sha256(canonicalPayload(caseData)),
                caseData = caseData,
            )
            val documentBytes = DomainJson.encodeToString(document).encodeToByteArray()
            require(documentBytes.size <= SingleCaseExchangeService.MAX_DOCUMENT_BYTES)
            val manifest = SingleCaseBundleManifest(
                formatVersion = BUNDLE_FORMAT_VERSION,
                documentPath = DOCUMENT_PATH,
                documentByteSize = documentBytes.size.toLong(),
                documentSha256 = sha256(documentBytes),
                attachments = attachmentSources.map { it.entry },
            )
            val manifestBytes = DomainJson.encodeToString(manifest).encodeToByteArray()
            val digestingOutput = DigestingCountingOutputStream(output)
            val protectedOutput = when (protection) {
                SingleCaseProtection.UnencryptedSensitiveDataConfirmed -> digestingOutput
                is SingleCaseProtection.PasswordProtected ->
                    encryption.encryptingStream(digestingOutput, protection.password)
            }
            ZipOutputStream(protectedOutput.buffered()).use { zip ->
                zip.putNextEntry(stableZipEntry(MANIFEST_PATH))
                zip.write(manifestBytes)
                zip.closeEntry()
                zip.putNextEntry(stableZipEntry(DOCUMENT_PATH))
                zip.write(documentBytes)
                zip.closeEntry()
                attachmentSources.forEach { attachment ->
                    zip.putNextEntry(stableZipEntry(attachment.entry.path))
                    Files.newInputStream(attachment.source).buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            SingleCaseExportResult.Success(
                suggestedFileName = when (protection) {
                    SingleCaseProtection.UnencryptedSensitiveDataConfirmed ->
                        suggestedFileName(caseData)
                    is SingleCaseProtection.PasswordProtected ->
                        suggestedEncryptedFileName(caseData)
                },
                byteSize = digestingOutput.byteCount,
                sha256 = digestingOutput.sha256(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            rejectedExport("EXPORT_IO_ERROR", "单命例附件包导出失败，请保留诊断代码并重试。")
        }
    }

    override fun suggestedFileName(case: BaziCase): String =
        "${safeAlias(case)}_南枫八字命例包.nfbcase"

    override fun suggestedEncryptedFileName(case: BaziCase): String =
        "${safeAlias(case)}_南枫八字命例包_加密.nfbcase"

    override suspend fun preview(
        input: InputStream,
        workRoot: Path,
        password: CharArray?,
    ): SingleCasePreviewResult {
        Files.createDirectories(workRoot)
        val stagingRoot = Files.createTempDirectory(workRoot, "single-case-bundle-preview-")
        return try {
            val prepared = prepareInput(input, password, stagingRoot)
            if (prepared is PreparedBundleInput.Rejected) {
                return SingleCasePreviewResult.Rejected(prepared.code, prepared.message)
            }
            prepared as PreparedBundleInput.Success
            val extracted = prepared.input.use { extractSafely(it, stagingRoot) }
                ?: return rejectedPreview(
                    "INVALID_BUNDLE",
                    "命例包路径、条目数量或展开大小不符合安全限制。",
                )
            val validated = validateExtracted(extracted)
            if (validated is ValidatedBundle.Rejected) {
                return rejectedPreview(validated.code, validated.message)
            }
            validated as ValidatedBundle.Success
            val basePreview = exchange.preview(
                ByteArrayInputStream(validated.documentBytes),
            )
            if (basePreview is SingleCasePreviewResult.Rejected) {
                return basePreview
            }
            basePreview as SingleCasePreviewResult.Success
            SingleCasePreviewResult.Success(
                basePreview.preview.copy(
                    containsAttachmentBinaries = true,
                    bundleManifestSha256 = sha256(
                        DomainJson.encodeToString(validated.manifest).encodeToByteArray(),
                    ),
                    protection = if (prepared.encrypted) {
                        SingleCaseDocumentProtection.PASSWORD_PROTECTED
                    } else {
                        SingleCaseDocumentProtection.UNENCRYPTED
                    },
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SerializationException) {
            rejectedPreview("INVALID_BUNDLE_METADATA", "命例包清单或命例 JSON 无效。")
        } catch (_: IllegalArgumentException) {
            rejectedPreview("INVALID_CASE_DATA", "命例包字段或引用关系不合法。")
        } catch (_: Exception) {
            rejectedPreview("READ_FAILED", "无法读取单命例附件包。")
        } finally {
            deleteRecursively(stagingRoot)
        }
    }

    override suspend fun commitImport(
        preview: SingleCasePreview,
        decision: SingleCaseImportDecision,
        input: InputStream,
        workRoot: Path,
        attachmentRoot: Path,
        password: CharArray?,
    ): SingleCaseImportResult {
        if (decision == SingleCaseImportDecision.SKIP) {
            return SingleCaseImportResult.Skipped(preview.document.caseData.id)
        }
        Files.createDirectories(workRoot)
        val stagingRoot = Files.createTempDirectory(workRoot, "single-case-bundle-commit-")
        return try {
            val source = readCommitSource(input, password, stagingRoot)
            if (source is CommitSource.Rejected) {
                return SingleCaseImportResult.Rejected(source.code, source.message)
            }
            source as CommitSource.Success
            source.matches(preview)?.let {
                return SingleCaseImportResult.Rejected(it.code, it.message)
            }
            val transactionId = UUID.randomUUID().toString()
            val attachmentIds = generateAttachmentIds(
                preview.document.caseData.attachments,
            )
            val importedAttachments = preview.document.caseData.attachments.map { attachment ->
                val newId = attachmentIds.getValue(attachment.id)
                attachment.copy(
                    id = newId,
                    relativePath =
                        "$RESTORED_ATTACHMENTS_DIRECTORY_NAME/$transactionId/$newId",
                )
            }
            val prepared = exchange.prepareBundledImport(
                preview = preview,
                attachmentIdMapping = attachmentIds,
                importedAttachments = importedAttachments,
            )
            if (prepared is PreparedBundledImportResult.Rejected) {
                return SingleCaseImportResult.Rejected(prepared.code, prepared.message)
            }
            prepared as PreparedBundledImportResult.Success
            val attachmentFiles = buildAttachmentFiles(
                source = source.validated,
                sourceAttachments = preview.document.caseData.attachments,
                restoredAttachments = importedAttachments,
            )
            executeCaseCommit(
                preview = preview,
                attachmentRoot = attachmentRoot,
                transactionId = transactionId,
                attachmentFiles = attachmentFiles,
                caseData = prepared.caseData,
                expectedCommittedCase = prepared.caseData.copy(revision = 1),
                expectedRevision = 0,
                previousPayloadSha256 = null,
            ) { writeResult ->
                when (writeResult) {
                    is CaseWriteResult.Created -> SingleCaseImportResult.Imported(
                        caseId = writeResult.caseId,
                        revision = writeResult.revision,
                    )
                    is CaseWriteResult.AlreadyExists,
                    is CaseWriteResult.RevisionConflict,
                    is CaseWriteResult.Updated,
                    -> throw BundleCommitRejectedException(
                        "GENERATED_ID_CONFLICT",
                        "新命例身份发生冲突，数据库与附件事务已回滚。",
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (rejected: BundleCommitRejectedException) {
            SingleCaseImportResult.Rejected(rejected.code, rejected.safeMessage)
        } catch (_: Exception) {
            SingleCaseImportResult.Rejected(
                "IMPORT_FAILED_ROLLED_BACK",
                "单命例附件包导入失败，数据库与本次附件已回滚。",
            )
        } finally {
            deleteRecursively(stagingRoot)
        }
    }

    override suspend fun commitMerge(
        plan: SingleCaseMergePlan,
        input: InputStream,
        workRoot: Path,
        attachmentRoot: Path,
        password: CharArray?,
    ): SingleCaseImportResult {
        val preview = plan.preparation.sourcePreview
        Files.createDirectories(workRoot)
        val stagingRoot = Files.createTempDirectory(workRoot, "single-case-bundle-merge-")
        return try {
            val source = readCommitSource(input, password, stagingRoot)
            if (source is CommitSource.Rejected) {
                return SingleCaseImportResult.Rejected(source.code, source.message)
            }
            source as CommitSource.Success
            source.matches(preview)?.let {
                return SingleCaseImportResult.Rejected(it.code, it.message)
            }
            val transactionId = UUID.randomUUID().toString()
            val target = repository.findById(plan.preparation.targetCaseId)
                ?: return SingleCaseImportResult.Rejected(
                    "TARGET_NOT_FOUND",
                    "所选本地目标命例已不存在，未写入数据。",
                )
            val requiredAttachmentIds = exchange.requiredAttachmentIdsForRestore(
                source = preview.document.caseData,
                target = target,
                modules = plan.modules,
            )
            val sourceAttachments = preview.document.caseData.attachments.filter {
                it.id in requiredAttachmentIds
            }
            if (sourceAttachments.size != requiredAttachmentIds.size) {
                return SingleCaseImportResult.Rejected(
                    "ATTACHMENT_REFERENCE_INVALID",
                    "所选合并内容存在无法解析的附件引用，未写入数据。",
                )
            }
            val attachmentIds = generateAttachmentIds(sourceAttachments)
            val importedAttachments = sourceAttachments.map { attachment ->
                val newId = attachmentIds.getValue(attachment.id)
                attachment.copy(
                    id = newId,
                    relativePath =
                        "$RESTORED_ATTACHMENTS_DIRECTORY_NAME/$transactionId/$newId",
                )
            }
            val prepared = exchange.prepareBundledMerge(
                plan = plan,
                attachmentIdMapping = attachmentIds,
                importedAttachments = importedAttachments,
            )
            if (prepared is PreparedBundledMergeResult.Rejected) {
                return SingleCaseImportResult.Rejected(prepared.code, prepared.message)
            }
            prepared as PreparedBundledMergeResult.Success
            val attachmentFiles = buildAttachmentFiles(
                source = source.validated,
                sourceAttachments = sourceAttachments,
                restoredAttachments = importedAttachments,
            )
            executeCaseCommit(
                preview = preview,
                attachmentRoot = attachmentRoot,
                transactionId = transactionId,
                attachmentFiles = attachmentFiles,
                caseData = prepared.caseData,
                expectedCommittedCase = prepared.expectedCommittedCase,
                expectedRevision = prepared.targetRevision,
                previousPayloadSha256 = prepared.previousPayloadSha256,
            ) { writeResult ->
                when (writeResult) {
                    is CaseWriteResult.Updated -> SingleCaseImportResult.Merged(
                        caseId = writeResult.caseId,
                        revision = writeResult.revision,
                        addedCounts = prepared.addedCounts,
                    )
                    is CaseWriteResult.RevisionConflict -> throw BundleCommitRejectedException(
                        "TARGET_CHANGED",
                        "本地目标命例已有较新修订，数据库与附件事务已回滚。",
                    )
                    is CaseWriteResult.AlreadyExists,
                    is CaseWriteResult.Created,
                    -> throw BundleCommitRejectedException(
                        "MERGE_WRITE_CONTRACT_FAILED",
                        "合并没有按更新合同完成，数据库与附件事务已回滚。",
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (rejected: BundleCommitRejectedException) {
            SingleCaseImportResult.Rejected(rejected.code, rejected.safeMessage)
        } catch (_: Exception) {
            SingleCaseImportResult.Rejected(
                "MERGE_FAILED_ROLLED_BACK",
                "单命例附件合并失败，数据库与本次附件已回滚。",
            )
        } finally {
            deleteRecursively(stagingRoot)
        }
    }

    private suspend fun executeCaseCommit(
        preview: SingleCasePreview,
        attachmentRoot: Path,
        transactionId: String,
        attachmentFiles: List<PreparedBundleAttachmentFile>,
        caseData: BaziCase,
        expectedCommittedCase: BaziCase,
        expectedRevision: Long,
        previousPayloadSha256: String?,
        success: (CaseWriteResult) -> SingleCaseImportResult,
    ): SingleCaseImportResult {
        var stageRoot: Path? = null
        var finalRoot: Path? = null
        var journalPath: Path? = null
        var journal: RestoreJournal? = null
        var committed = false
        try {
            if (attachmentFiles.isNotEmpty()) {
                Files.createDirectories(attachmentRoot)
                val restoreJournal = RestoreJournal(
                    transactionId = transactionId,
                    state = RestoreJournalState.PREPARED,
                    finalDirectory =
                        "$RESTORED_ATTACHMENTS_DIRECTORY_NAME/$transactionId",
                    expectedCases = listOf(
                        RestoreExpectedCase(
                            caseId = expectedCommittedCase.id,
                            payloadSha256 = casePayloadSha256(expectedCommittedCase),
                            previousPayloadSha256 = previousPayloadSha256,
                        ),
                    ),
                )
                journal = restoreJournal
                journalPath = writeRestoreJournal(attachmentRoot, restoreJournal)
                val stageParent = attachmentRoot.resolve(RESTORE_STAGING_DIRECTORY_NAME)
                Files.createDirectories(stageParent)
                val transactionStage = stageParent.resolve(transactionId)
                Files.createDirectory(transactionStage)
                stageRoot = transactionStage
                attachmentFiles.forEach { attachment ->
                    val target = transactionStage.resolve(attachment.fileName)
                    Files.copy(attachment.source, target)
                    check(
                        Files.size(target) == attachment.byteSize &&
                            sha256(target) == attachment.sha256,
                    ) { "暂存附件校验失败" }
                }
                val finalParent = attachmentRoot.resolve(
                    RESTORED_ATTACHMENTS_DIRECTORY_NAME,
                )
                Files.createDirectories(finalParent)
                val transactionFinal = finalParent.resolve(transactionId)
                check(!Files.exists(transactionFinal)) { "附件事务目录已存在" }
                moveDirectory(transactionStage, transactionFinal)
                stageRoot = null
                finalRoot = transactionFinal
                val moved = restoreJournal.copy(state = RestoreJournalState.FILES_MOVED)
                journal = moved
                journalPath = writeRestoreJournal(attachmentRoot, moved)
            }

            val result = database.withTransaction {
                exchange.revalidateBundledPreview(preview)?.let {
                    throw BundleCommitRejectedException(it.code, it.message)
                }
                val writeResult = repository.save(
                    caseData,
                    expectedRevision = expectedRevision,
                )
                success(writeResult)
            }
            committed = true
            journal?.let { restoreJournal ->
                runCatching {
                    writeRestoreJournal(
                        attachmentRoot,
                        restoreJournal.copy(state = RestoreJournalState.DB_COMMITTED),
                    )
                    Files.deleteIfExists(
                        attachmentRoot
                            .resolve(RESTORE_JOURNAL_DIRECTORY_NAME)
                            .resolve("${restoreJournal.transactionId}.json"),
                    )
                }
            }
            return result
        } finally {
            if (!committed) {
                finalRoot?.let(::deleteRecursively)
                journalPath?.let(Files::deleteIfExists)
            }
            stageRoot?.let(::deleteRecursively)
        }
    }

    private fun readCommitSource(
        input: InputStream,
        password: CharArray?,
        stagingRoot: Path,
    ): CommitSource {
        val prepared = prepareInput(input, password, stagingRoot)
        if (prepared is PreparedBundleInput.Rejected) {
            return CommitSource.Rejected(prepared.code, prepared.message)
        }
        prepared as PreparedBundleInput.Success
        val extracted = prepared.input.use { extractSafely(it, stagingRoot) }
            ?: return CommitSource.Rejected(
                "INVALID_BUNDLE",
                "命例包路径、条目数量或展开大小不符合安全限制。",
            )
        val validated = validateExtracted(extracted)
        if (validated is ValidatedBundle.Rejected) {
            return CommitSource.Rejected(validated.code, validated.message)
        }
        return CommitSource.Success(
            validated = validated as ValidatedBundle.Success,
            encrypted = prepared.encrypted,
        )
    }

    private fun generateAttachmentIds(
        attachments: List<SourceAttachment>,
    ): Map<String, String> {
        val used = attachments.mapTo(mutableSetOf()) { it.id }
        return attachments.associate { attachment ->
            attachment.id to nextGeneratedId(used)
        }
    }

    private fun nextGeneratedId(used: MutableSet<String>): String {
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val candidate = idGenerator().trim()
            if (candidate.matches(GENERATED_ID_PATTERN) && used.add(candidate)) {
                return candidate
            }
        }
        throw IllegalArgumentException("Unable to generate attachment id")
    }

    private fun buildAttachmentFiles(
        source: ValidatedBundle.Success,
        sourceAttachments: List<SourceAttachment>,
        restoredAttachments: List<SourceAttachment>,
    ): List<PreparedBundleAttachmentFile> {
        val manifestById = source.manifest.attachments.associateBy { it.attachmentId }
        val restoredByOriginal = sourceAttachments.zip(restoredAttachments).associate {
            (original, restored) -> original.id to restored
        }
        return sourceAttachments.map { attachment ->
            val manifest = manifestById.getValue(attachment.id)
            val restored = restoredByOriginal.getValue(attachment.id)
            PreparedBundleAttachmentFile(
                source = source.extracted.getValue(manifest.path),
                fileName = restored.id,
                byteSize = restored.byteSize,
                sha256 = restored.sha256,
            )
        }
    }

    private fun validateExtracted(
        extracted: Map<String, Path>,
    ): ValidatedBundle {
        val manifestPath = extracted[MANIFEST_PATH]
            ?: return rejectedValidated("MANIFEST_MISSING", "命例包缺少 manifest.json。")
        val manifest = DomainJson.decodeFromString<SingleCaseBundleManifest>(
            readUtf8(manifestPath),
        )
        if (
            manifest.formatVersion != BUNDLE_FORMAT_VERSION ||
            manifest.documentPath != DOCUMENT_PATH
        ) {
            return rejectedValidated("UNSUPPORTED_BUNDLE_VERSION", "命例包格式版本不受支持。")
        }
        if (
            manifest.documentByteSize !in 1..SingleCaseExchangeService.MAX_DOCUMENT_BYTES.toLong() ||
            !manifest.documentSha256.matches(SHA256_PATTERN)
        ) {
            return rejectedValidated("INVALID_BUNDLE_METADATA", "命例包文档元数据无效。")
        }
        if (
            manifest.attachments.size > MAX_ATTACHMENT_COUNT ||
            manifest.attachments.map { it.attachmentId }.distinct().size !=
            manifest.attachments.size ||
            manifest.attachments.map { it.path }.distinct().size != manifest.attachments.size
        ) {
            return rejectedValidated("INVALID_BUNDLE_METADATA", "命例包附件清单重复或超限。")
        }
        val expectedPaths = buildSet {
            add(MANIFEST_PATH)
            add(DOCUMENT_PATH)
            addAll(manifest.attachments.map { it.path })
        }
        if (extracted.keys != expectedPaths) {
            return rejectedValidated(
                "UNDECLARED_BUNDLE_ENTRY",
                "命例包包含未声明文件或缺少已声明文件。",
            )
        }
        val documentPath = extracted.getValue(DOCUMENT_PATH)
        if (
            Files.size(documentPath) != manifest.documentByteSize ||
            sha256(documentPath) != manifest.documentSha256
        ) {
            return rejectedValidated("DOCUMENT_HASH_MISMATCH", "命例 JSON 大小或哈希不一致。")
        }
        val documentBytes = Files.readAllBytes(documentPath)
        val document = DomainJson.decodeFromString<SingleCaseDocument>(
            documentBytes.decodeToString(),
        )
        if (document.attachmentMode != SingleCaseAttachmentMode.BUNDLED_BINARIES) {
            return rejectedValidated(
                "ATTACHMENT_MODE_MISMATCH",
                "命例 JSON 未声明附件二进制已随包导出。",
            )
        }
        val attachmentsById = document.caseData.attachments.associateBy { it.id }
        if (manifest.attachments.mapTo(mutableSetOf()) { it.attachmentId } != attachmentsById.keys) {
            return rejectedValidated(
                "ATTACHMENT_MANIFEST_MISMATCH",
                "命例包附件清单与命例引用不一致。",
            )
        }
        manifest.attachments.forEach { entry ->
            if (
                !entry.path.startsWith("$ATTACHMENTS_DIRECTORY/") ||
                !isSafeEntryName(entry.path) ||
                entry.byteSize < 0 ||
                !entry.sha256.matches(SHA256_PATTERN)
            ) {
                return rejectedValidated("INVALID_BUNDLE_METADATA", "附件清单字段无效。")
            }
            val attachment = attachmentsById.getValue(entry.attachmentId)
            val file = extracted.getValue(entry.path)
            if (
                entry.byteSize != attachment.byteSize ||
                entry.sha256 != attachment.sha256 ||
                Files.size(file) != entry.byteSize ||
                sha256(file) != entry.sha256
            ) {
                return rejectedValidated(
                    "ATTACHMENT_HASH_MISMATCH",
                    "附件大小或 SHA-256 与命例清单不一致。",
                )
            }
        }
        return ValidatedBundle.Success(
            manifest = manifest,
            document = document,
            documentBytes = documentBytes,
            extracted = extracted,
        )
    }

    private fun prepareInput(
        input: InputStream,
        password: CharArray?,
        stagingRoot: Path,
    ): PreparedBundleInput {
        val prefix = ByteArray(SingleCaseBundleEncryption.MAGIC_BYTES.size)
        var count = 0
        while (count < prefix.size) {
            val read = input.read(prefix, count, prefix.size - count)
            if (read < 0) break
            count += read
        }
        if (
            count == SingleCaseBundleEncryption.MAGIC_BYTES.size &&
            prefix.contentEquals(SingleCaseBundleEncryption.MAGIC_BYTES)
        ) {
            if (password == null || password.isEmpty()) {
                return PreparedBundleInput.Rejected(
                    "PASSWORD_REQUIRED",
                    "该单命例附件包已加密，请输入密码。",
                )
            }
            val target = stagingRoot.resolve("decrypted-single-case-bundle.zip")
            return try {
                encryption.decryptAfterMagic(input, password, target)
                PreparedBundleInput.Success(Files.newInputStream(target), encrypted = true)
            } catch (_: Exception) {
                PreparedBundleInput.Rejected(
                    "DECRYPTION_FAILED",
                    "密码错误、文件已损坏或加密参数不受支持。",
                )
            }
        }
        return PreparedBundleInput.Success(
            SequenceInputStream(ByteArrayInputStream(prefix, 0, count), input),
            encrypted = false,
        )
    }

    private fun extractSafely(
        input: InputStream,
        stagingRoot: Path,
    ): Map<String, Path>? {
        val extracted = linkedMapOf<String, Path>()
        var totalBytes = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                if (extracted.size >= MAX_ENTRY_COUNT) return null
                val name = entry.name
                if (!isSafeEntryName(name) || extracted.containsKey(name)) return null
                val target = resolveContained(stagingRoot, name) ?: return null
                Files.createDirectories(target.parent)
                var entryBytes = 0L
                Files.newOutputStream(target).buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        if (
                            entryBytes > MAX_SINGLE_ENTRY_BYTES ||
                            totalBytes > MAX_TOTAL_BYTES
                        ) {
                            return null
                        }
                        output.write(buffer, 0, count)
                    }
                }
                if (name.endsWith(".json") && entryBytes > MAX_JSON_BYTES) return null
                extracted[name] = target
                zip.closeEntry()
            }
        }
        return extracted
    }

    private fun rejectedExport(code: String, message: String) =
        SingleCaseExportResult.Rejected(code, message)

    private fun rejectedPreview(code: String, message: String) =
        SingleCasePreviewResult.Rejected(code, message)

    private fun rejectedValidated(code: String, message: String) =
        ValidatedBundle.Rejected(code, message)

    private data class BundleAttachmentSource(
        val entry: SingleCaseBundleAttachmentEntry,
        val source: Path,
    )

    private sealed interface PreparedBundleInput {
        data class Success(
            val input: InputStream,
            val encrypted: Boolean,
        ) : PreparedBundleInput

        data class Rejected(
            val code: String,
            val message: String,
        ) : PreparedBundleInput
    }

    private sealed interface CommitSource {
        data class Success(
            val validated: ValidatedBundle.Success,
            val encrypted: Boolean,
        ) : CommitSource {
            fun matches(preview: SingleCasePreview): BundledPreviewRejection? {
                val manifestSha256 = sha256(
                    DomainJson.encodeToString(validated.manifest).encodeToByteArray(),
                )
                val expectedProtection = if (encrypted) {
                    SingleCaseDocumentProtection.PASSWORD_PROTECTED
                } else {
                    SingleCaseDocumentProtection.UNENCRYPTED
                }
                return if (
                    validated.document != preview.document ||
                    manifestSha256 != preview.bundleManifestSha256 ||
                    expectedProtection != preview.protection
                ) {
                    BundledPreviewRejection(
                        "SOURCE_CHANGED_AFTER_PREVIEW",
                        "提交时读取的命例包已不是生成预览的同一份来源。",
                    )
                } else {
                    null
                }
            }
        }

        data class Rejected(
            val code: String,
            val message: String,
        ) : CommitSource
    }

    private sealed interface ValidatedBundle {
        data class Success(
            val manifest: SingleCaseBundleManifest,
            val document: SingleCaseDocument,
            val documentBytes: ByteArray,
            val extracted: Map<String, Path>,
        ) : ValidatedBundle

        data class Rejected(
            val code: String,
            val message: String,
        ) : ValidatedBundle
    }

    private data class PreparedBundleAttachmentFile(
        val source: Path,
        val fileName: String,
        val byteSize: Long,
        val sha256: String,
    )

    private class BundleCommitRejectedException(
        val code: String,
        val safeMessage: String,
    ) : Exception(safeMessage)

    companion object {
        const val BUNDLE_FORMAT_VERSION = 1
        private const val MANIFEST_PATH = "manifest.json"
        private const val DOCUMENT_PATH = "case.json"
        private const val ATTACHMENTS_DIRECTORY = "attachments"
        private const val MAX_ATTACHMENT_COUNT = 2_000
        private const val MAX_ENTRY_COUNT = 2_010
        private const val MAX_JSON_BYTES = 16L * 1024 * 1024
        private const val MAX_SINGLE_ENTRY_BYTES = 512L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 1024L * 1024 * 1024
        private const val MAX_ID_GENERATION_ATTEMPTS = 100
        private val GENERATED_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,128}")
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

private class DigestingCountingOutputStream(
    private val delegate: OutputStream,
) : OutputStream() {
    private val digest = MessageDigest.getInstance("SHA-256")
    var byteCount: Long = 0
        private set

    override fun write(value: Int) {
        delegate.write(value)
        digest.update(value.toByte())
        byteCount += 1
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        delegate.write(bytes, offset, length)
        digest.update(bytes, offset, length)
        byteCount += length
    }

    override fun flush() = delegate.flush()

    override fun close() = delegate.close()

    fun sha256(): String = digest.digest().toHex()
}

private fun canonicalPayload(caseData: BaziCase): ByteArray =
    DomainJson.encodeToString(BaziCase.serializer(), caseData).encodeToByteArray()

private fun safeAlias(caseData: BaziCase): String = caseData.alias
    .trim()
    .replace(INVALID_BUNDLE_FILE_NAME_PATTERN, "_")
    .trim('.', ' ')
    .take(48)
    .ifBlank { caseData.id.take(48) }

private fun stableZipEntry(path: String): ZipEntry = ZipEntry(path).apply {
    time = 0L
}

private fun moveDirectory(source: Path, target: Path) {
    try {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source, target)
    }
}

private fun isSafeEntryName(name: String): Boolean {
    if (name.isBlank() || name.startsWith("/") || name.startsWith("\\")) return false
    if ('\\' in name || ':' in name) return false
    return name.split('/').none { it.isBlank() || it == "." || it == ".." }
}

private fun resolveContained(root: Path, relative: String): Path? {
    val normalizedRoot = root.toAbsolutePath().normalize()
    val resolved = normalizedRoot.resolve(relative).normalize()
    return resolved.takeIf { it.startsWith(normalizedRoot) }
}

private fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().toHex()
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun readUtf8(path: Path): String = Files.readAllBytes(path).decodeToString()

private fun deleteRecursively(root: Path) {
    if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return
    Files.walk(root).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}

private val INVALID_BUNDLE_FILE_NAME_PATTERN = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
