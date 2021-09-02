package com.reposilite.storage

import com.reposilite.storage.infrastructure.FileSystemStorageProviderFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class FileSystemStorageProviderTest : StorageProviderTest() {

    @TempDir
    lateinit var rootDirectory: File

    @BeforeEach
    fun setup() {
        super.storageProvider = FileSystemStorageProviderFactory.of(rootDirectory.toPath(), 1024 * 1024)
    }

}