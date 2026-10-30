package com.stealzalut.mobile.data.cache

import com.stealzalut.mobile.core.AppConstants
import java.io.File

/**
 * LRU Cache Manager for StealZaLUT temporary preview files.
 * Ensures temporary preview files written to cacheDir/stealzalut_previews/
 * do not exceed 50 MB in aggregate size or persist older than 24 hours.
 */
class LruCacheManager(
    val previewDir: File,
    private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MS,
    private val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES
) {

    init {
        if (!previewDir.exists()) {
            previewDir.mkdirs()
        }
    }

    /**
     * Creates a new uniquely named preview file within the previews cache directory.
     * Triggers an automatic cleanup check before returning.
     */
    fun createPreviewFile(prefix: String = "prev_", extension: String = "jpg"): File {
        cleanupOldOrExcessFiles()
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return File(previewDir, "${prefix}${timestamp}_${random}.$extension")
    }

    /**
     * Prunes files that are older than [maxAgeMs] and ensures the total directory
     * size does not exceed [maxBytes] using a Least-Recently-Used (oldest modified first) strategy.
     */
    @Synchronized
    fun cleanupOldOrExcessFiles(
        maxAgeMs: Long = maxAgeMillis,
        maxBytes: Long = maxSizeBytes
    ) {
        if (!previewDir.exists() || !previewDir.isDirectory) return

        val now = System.currentTimeMillis()
        val files = previewDir.listFiles()?.filter { it.isFile } ?: return

        // 1. Remove files older than 24 hours
        val remainingFiles = mutableListOf<File>()
        for (file in files) {
            val age = now - file.lastModified()
            if (age > maxAgeMs) {
                file.delete()
            } else {
                remainingFiles.add(file)
            }
        }

        // 2. If total size still exceeds maxBytes, prune LRU (oldest lastModified first)
        var totalSize = remainingFiles.sumOf { it.length() }
        if (totalSize > maxBytes) {
            // Sort ascending: oldest files first
            remainingFiles.sortBy { it.lastModified() }
            for (file in remainingFiles) {
                if (totalSize <= maxBytes * 0.8) {
                    // Stop once we've brought the cache below 80% to prevent continuous trashing
                    break
                }
                val fileSize = file.length()
                if (file.delete()) {
                    totalSize -= fileSize
                }
            }
        }
    }

    /**
     * Returns total disk space occupied by the previews directory in Megabytes.
     */
    fun getCacheSizeMb(): Double {
        if (!previewDir.exists()) return 0.0
        val totalBytes = previewDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        return totalBytes / (1024.0 * 1024.0)
    }

    /**
     * Clears all preview files in the directory.
     */
    fun clearAll(): Boolean {
        if (!previewDir.exists()) return true
        val files = previewDir.listFiles() ?: return true
        var allDeleted = true
        for (file in files) {
            if (file.isFile) {
                if (!file.delete()) allDeleted = false
            } else if (file.isDirectory) {
                if (!file.deleteRecursively()) allDeleted = false
            }
        }
        return allDeleted
    }

    companion object {
        const val DEFAULT_MAX_AGE_MS: Long = AppConstants.CACHE_EXPIRATION_MILLIS
        const val DEFAULT_MAX_SIZE_BYTES: Long = AppConstants.MAX_CACHE_SIZE_BYTES
        const val PREVIEWS_DIR_NAME: String = "stealzalut_previews"
    }
}
