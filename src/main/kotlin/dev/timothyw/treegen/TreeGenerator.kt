package dev.timothyw.treegen

import dev.timothyw.treegen.FileUtils.formattedFileSize
import java.nio.file.Path
import kotlin.io.path.*

class TreeGenerator {
    companion object {
        private val DEFAULT_IGNORE_PATTERNS = setOf(
            ".git",
            "node_modules",
            "__pycache__",
            ".idea",
            "build",
            "dist",
            ".gradle",
            "target",
            "out"
        )
    }

    fun generateTree(path: Path, config: TreeConfig): String {
        return generateTreeImpl(path, "", config)
    }

    private fun generateTreeImpl(
        path: Path,
        prefix: String = "",
        config: TreeConfig,
        currentDepth: Int = 0
    ): String {
        if (currentDepth == config.maxDepth) {
            return ""
        }

        val builder = StringBuilder()
        val entries = getFilteredEntries(path, config)
        val (dirs, files) = entries.partition { it.isDirectory() }

        if (prefix.isEmpty()) {
            builder.append("${path.name}/\n")
        }

        appendDirectories(builder, dirs, files, prefix, config, currentDepth)
        appendFiles(builder, files, prefix, config)

        return builder.toString()
    }

    private fun getFilteredEntries(path: Path, config: TreeConfig): List<Path> {
        val ignorePatterns = DEFAULT_IGNORE_PATTERNS + config.customIgnorePatterns
        return path.listDirectoryEntries()
            .filter { entry ->
                val shouldInclude = ignorePatterns.none { pattern -> entry.name.contains(pattern) }
                shouldInclude && (config.showHidden || !entry.name.startsWith("."))
            }
            .sortedBy { it.name }
    }

    private fun appendDirectories(
        builder: StringBuilder,
        dirs: List<Path>,
        files: List<Path>,
        prefix: String,
        config: TreeConfig,
        currentDepth: Int
    ) {
        dirs.forEachIndexed { index, dir ->
            val isLast = index == dirs.lastIndex && files.isEmpty()
            val newPrefix = prefix + if (isLast) "    " else "│   "
            val sizeInfo = if (config.includeSizes) " (${FileUtils.calculateDirSize(dir)})" else ""

            builder.append("$prefix├── ${dir.name}/$sizeInfo\n")
            builder.append(generateTreeImpl(dir, newPrefix, config, currentDepth + 1))
        }
    }

    private fun appendFiles(
        builder: StringBuilder,
        files: List<Path>,
        prefix: String,
        config: TreeConfig
    ) {
        files.forEachIndexed { index, file ->
            val isLast = index == files.lastIndex
            val sizeInfo = if (config.includeSizes) " (${file.formattedFileSize()})" else ""
            builder.append("$prefix${if (isLast) "└── " else "├── "}${file.name}$sizeInfo\n")
        }
    }
}