package dev.timothyw.treegen

import com.intellij.openapi.diagnostic.Logger
import dev.timothyw.treegen.FileUtils.formattedFileSize
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class TreeGenerator {
    private val log = Logger.getInstance(TreeGenerator::class.java)

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
        log.warn("TreeGen Plugin - Starting tree generation with patterns: ${config.customIgnorePatterns}")
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
        val defaultPatterns = DEFAULT_IGNORE_PATTERNS.map { pattern ->
            try {
                pattern.toRegex(RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                Regex.escape(pattern).toRegex(RegexOption.IGNORE_CASE)
            }
        }

        val customPatterns = config.customIgnorePatterns.map { pattern ->
            try {
                pattern.toRegex(RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                log.warn("TreeGen Plugin - Invalid regex pattern: $pattern, using as literal")
                Regex.escape(pattern).toRegex(RegexOption.IGNORE_CASE)
            }
        }

        val allPatterns = defaultPatterns + customPatterns

        return path.listDirectoryEntries().filter { entry ->
            val name = entry.name
            log.warn("TreeGen Plugin - Checking file: $name")

            val shouldExclude = allPatterns.any { regex ->
                val matches = regex.containsMatchIn(name)
                log.warn("TreeGen Plugin - Testing if pattern '${regex.pattern}' matches '$name': $matches")
                matches
            }

            val shouldInclude = !shouldExclude && (config.showHidden || !name.startsWith("."))
            log.warn("TreeGen Plugin - Final decision for $name: ${if (shouldInclude) "include" else "exclude"}")

            shouldInclude
        }.sortedBy { it.name }
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