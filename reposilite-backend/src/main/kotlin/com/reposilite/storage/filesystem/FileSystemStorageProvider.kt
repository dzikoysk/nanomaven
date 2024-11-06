/*
 * Copyright (c) 2023 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.storage.filesystem

import com.google.common.cache.CacheBuilder
import com.reposilite.journalist.Journalist
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.badRequest
import com.reposilite.shared.notFound
import com.reposilite.shared.toErrorResponse
import com.reposilite.storage.FilesComparator
import com.reposilite.storage.StorageProvider
import com.reposilite.storage.VersionComparator
import com.reposilite.storage.api.DirectoryInfo
import com.reposilite.storage.api.DocumentInfo
import com.reposilite.storage.api.FileDetails
import com.reposilite.storage.api.FileType.DIRECTORY
import com.reposilite.storage.api.FileType.FILE
import com.reposilite.storage.api.Location
import com.reposilite.storage.api.SimpleDirectoryInfo
import com.reposilite.storage.inputStream
import com.reposilite.storage.type
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_OCTET_STREAM
import io.javalin.http.HttpStatus.INSUFFICIENT_STORAGE
import panda.std.Result
import panda.std.asSuccess
import java.io.Closeable
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.readAttributes
import kotlin.io.path.useDirectoryEntries

/**
 * @param rootDirectory root directory of storage space
 */
abstract class FileSystemStorageProvider protected constructor(
    val journalist: Journalist,
    val rootDirectory: Path,
    maxResourceLockLifetimeInSeconds: Int
) : StorageProvider {

    private enum class LockMode {
        READ,
        WRITE
    }

    private class FileAccessor(
        val lock: Semaphore = Semaphore(1),
        val reads: AtomicInteger = AtomicInteger(0),
    )

    private val lockedLocations =
        CacheBuilder.newBuilder()
            .expireAfterAccess(maxResourceLockLifetimeInSeconds.toLong(), SECONDS)
            .concurrencyLevel(4)
            .build<Location, FileAccessor>()

    private fun acquireFileAccessLock(location: Location, lockMode: LockMode): Closeable {
        val accessor = lockedLocations.get(location) { FileAccessor() }

        when (lockMode) {
            LockMode.READ -> if (accessor.reads.incrementAndGet() == 1) accessor.lock.acquire()
            LockMode.WRITE -> accessor.lock.acquire()
        }

        return Closeable {
            when (lockMode) {
                LockMode.READ -> if (accessor.reads.decrementAndGet() == 0) accessor.lock.release()
                LockMode.WRITE -> accessor.lock.release()
            }
        }
    }

    override fun putFile(location: Location, inputStream: InputStream): Result<Unit, ErrorResponse> =
        inputStream.use { data ->
            canHold(data.available().toLong())
                .mapErr { INSUFFICIENT_STORAGE.toErrorResponse("Not enough storage space available: ${it.message}") }
                .flatMap { location.resolveWithRootDirectory() }
                .map { file ->
                    file.createParentDirectories()

                    // TO-FIX: FS locks are not truly respected, there might be a need to enhanced it with .lock file to be sure if it's respected.
                    // In theory people shouldn't redeploy multiple times the same file, but who knows.
                    // Let's try with temporary files.
                    // ~ https://github.com/dzikoysk/reposilite/issues/264

                    val temporaryFile = createTempFile("reposilite-", "-fs-put")

                    temporaryFile.outputStream().use { destination ->
                        data.copyTo(destination)
                    }

                    acquireFileAccessLock(location, LockMode.WRITE).use {
                        temporaryFile.moveTo(file, overwrite = true)
                        Unit
                    }
                }
        }

    private class LockedFilterInputStream(private val inputStream: InputStream, private val lock: Closeable) : FilterInputStream(inputStream) {
        override fun close() {
            lock.use {
                inputStream.close()
            }
        }
    }

    override fun getFile(location: Location): Result<InputStream, ErrorResponse> =
        location.resolveWithRootDirectory()
            .exists()
            .flatMap { resource ->
                val lock = acquireFileAccessLock(location, LockMode.READ)

                resource.inputStream()
                    .onError { lock.close() }
                    .map { LockedFilterInputStream(it, lock) }
            }

    override fun getFileDetails(location: Location): Result<out FileDetails, ErrorResponse> =
        location.resolveWithRootDirectory()
            .exists()
            .flatMap { toFileDetails(it) }

    private fun toFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
        Result.`when`(file.exists(), file, notFound("File not found"))
            .flatMap {
                when (it.type()) {
                    FILE -> toDocumentInfo(it)
                    DIRECTORY -> toDirectoryInfo(it)
                }
            }

    private fun toDocumentInfo(path: Path): Result<DocumentInfo, ErrorResponse> =
        path
            .let { path.readAttributes<BasicFileAttributes>() }
            .let {
                DocumentInfo(
                    name = path.name,
                    contentType = ContentType.getContentTypeByExtension(path.extension) ?: APPLICATION_OCTET_STREAM,
                    contentLength = it.size(),
                    lastModifiedTime = it.lastModifiedTime().toInstant(),
                )
            }.asSuccess()

    private fun toDirectoryInfo(directory: Path): Result<DirectoryInfo, ErrorResponse> =
        DirectoryInfo(
            name = directory.name,
            files =
            directory.useDirectoryEntries { directoryStream ->
                directoryStream.map { toSimpleFileDetails(it).orThrow { error -> IOException(error.message) } }
                    .sortedWith(FilesComparator({ VersionComparator.asVersion(it.name) }, { it.type == DIRECTORY }))
                    .toList()
            }
        ).asSuccess()

    private fun toSimpleFileDetails(file: Path): Result<out FileDetails, ErrorResponse> =
        when (file.type()) {
            FILE -> toDocumentInfo(file)
            DIRECTORY -> SimpleDirectoryInfo(file.name).asSuccess()
        }

    @OptIn(ExperimentalPathApi::class)
    override fun removeFile(location: Location): Result<Unit, ErrorResponse> =
        location.resolveWithRootDirectory().map { rootPath ->
            when {
                rootDirectory.isDirectory() -> rootPath.deleteRecursively()
                else -> rootPath.deleteExisting()
            }
        }

    override fun getFiles(location: Location): Result<List<Location>, ErrorResponse> =
        location.resolveWithRootDirectory()
            .exists()
            .map {
                it.useDirectoryEntries { directoryStream ->
                    directoryStream.filter { path -> path != it }
                        .map { path -> Location.of(rootDirectory, path) }
                        .toList()
                }
            }

    override fun getLastModifiedTime(location: Location): Result<FileTime, ErrorResponse> =
        location.resolveWithRootDirectory()
            .exists()
            .map { it.getLastModifiedTime() }

    override fun getFileSize(location: Location): Result<Long, ErrorResponse> =
        location.resolveWithRootDirectory()
            .exists()
            .map {
                when (it.type()) {
                    FILE -> it.fileSize()
                    DIRECTORY -> -1
                }
            }

    override fun exists(location: Location): Boolean =
        location.resolveWithRootDirectory()
            .exists()
            .fold({ true }, { false })

    override fun usage(): Result<Long, ErrorResponse> =
        getFileSize(Location.empty())

    private fun Result<Path, ErrorResponse>.exists(): Result<Path, ErrorResponse> =
        filter({ it.exists() }) { notFound("File not found") }

    private fun Location.resolveWithRootDirectory(): Result<Path, ErrorResponse> =
        toPath()
            .map { rootDirectory.resolve(it) }
            .mapErr { badRequest(it) }

}
