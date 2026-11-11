package com.stealzalut.mobile.core.storage

import android.os.Build
import com.stealzalut.mobile.core.diagnostics.AppLog
import com.stealzalut.mobile.core.diagnostics.LogLevel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object AtomicJsonFile {
    fun readText(file: File): String {
        val backupFile = backupFile(file)
        if (!file.exists() && backupFile.exists() && !backupFile.renameTo(file)) {
            throw IOException("Unable to restore the previous ${file.name} file.")
        }
        return file.readText(StandardCharsets.UTF_8)
    }

    fun writeText(file: File, content: String) {
        val parent = file.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("Unable to create the storage directory.")
        }

        val tempFile = File(parent, "${file.name}.tmp")
        FileOutputStream(tempFile).use { output ->
            output.write(content.toByteArray(StandardCharsets.UTF_8))
            output.fd.sync()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
                return
            } catch (_: AtomicMoveNotSupportedException) {
                replaceWithBackup(tempFile, file)
                return
            }
        }
        replaceWithBackup(tempFile, file)
    }

    fun quarantineCorrupt(file: File, cause: Exception) {
        val suffix = System.currentTimeMillis()
        var index = 0
        var quarantinedFile: File
        do {
            val extra = if (index == 0) "" else "_$index"
            quarantinedFile = File(file.parentFile, "${file.name}.corrupt_$suffix$extra")
            index++
        } while (quarantinedFile.exists())

        if (!file.renameTo(quarantinedFile)) {
            throw IOException("Unable to preserve corrupted ${file.name}.", cause)
        }
        AppLog.log(
            level = LogLevel.ERROR,
            tag = "AtomicJsonFile",
            message = "Corrupt JSON file quarantined",
            fields = mapOf("file" to quarantinedFile.name)
        )
    }

    private fun replaceWithBackup(tempFile: File, targetFile: File) {
        val backup = backupFile(targetFile)
        if (backup.exists() && !backup.delete()) {
            throw IOException("Unable to clear the previous ${targetFile.name} backup.")
        }
        if (targetFile.exists() && !targetFile.renameTo(backup)) {
            throw IOException("Unable to preserve the previous ${targetFile.name} file.")
        }
        if (!tempFile.renameTo(targetFile)) {
            if (backup.exists() && !backup.renameTo(targetFile)) {
                throw IOException("Unable to replace ${targetFile.name} or restore its backup.")
            }
            throw IOException("Unable to replace ${targetFile.name}.")
        }
        backup.delete()
    }

    private fun backupFile(file: File): File = File(file.parentFile, "${file.name}.bak")
}
