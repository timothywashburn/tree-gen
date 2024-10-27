package dev.timothyw.treegen

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

object FileUtils {
    fun formatSize(size: Long): String {
        val units = listOf("B", "KB", "MB", "GB")
        var value = size.toDouble()
        var unitIndex = 0

        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return "%.1f %s".format(value, units[unitIndex])
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalPathApi::class)
    fun calculateDirSize(dir: Path): String {
        var totalSize = 0L
        dir.walk().forEach { path ->
            if (path.isRegularFile()) {
                totalSize += path.toFile().length()
            }
        }
        return formatSize(totalSize)
    }

    fun Path.formattedFileSize(): String = formatSize(toFile().length())
}