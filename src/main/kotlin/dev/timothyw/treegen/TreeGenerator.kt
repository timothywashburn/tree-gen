package dev.timothyw.treegen

import dev.timothyw.treegen.FileUtils.formattedFileSize
import java.nio.file.Path
import kotlin.io.path.*
import com.intellij.openapi.diagnostic.Logger

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
        log.info("Custom ignore patterns: ${config.customIgnorePatterns}")
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
        val ignorePatterns = DEFAULT_IGNORE_PATTERNS + config.customIgnorePatterns.flatMap { pattern ->
            // Split by comma in case user entered multiple patterns
            pattern.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        log.info("Processing directory: ${path.name}")
        log.info("All ignore patterns: $ignorePatterns")

        return path.listDirectoryEntries().filter { entry ->
            val name = entry.name
            log.info("Checking file: $name")

            val shouldExcludeByPattern = ignorePatterns.any { pattern ->
                try {
                    val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
                    val matches = regex.containsMatchIn(name)
                    log.info("Pattern '$pattern' matches '$name': $matches")
                    matches
                } catch (e: Exception) {
                    log.info("Pattern '$pattern' is not valid regex, using contains")
                    name.contains(pattern, ignoreCase = true)
                }
            }

            val shouldInclude = !shouldExcludeByPattern && (config.showHidden || !name.startsWith("."))
            log.info("Final decision for $name: ${if (shouldInclude) "include" else "exclude"}")

            !shouldExcludeByPattern && (config.showHidden || !name.startsWith("."))
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