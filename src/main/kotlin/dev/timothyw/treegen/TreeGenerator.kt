package dev.timothyw.treegen

import com.intellij.openapi.diagnostic.logger
import dev.timothyw.treegen.FileUtils.formattedFileSize
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class TreeGenerator {
    private val log = logger<TreeGenerator>()

    fun generateTree(path: Path, config: TreeConfig): String {
        println("treegen plugin - starting tree generation with patterns: ${config.customIgnorePatterns}")

        val gitIgnoreHandler = GitIgnoreHandler()
        val usingGitIgnore = if (config.useGitIgnore) {
            val foundGitIgnore = gitIgnoreHandler.loadGitIgnoreFiles(path)
            if (foundGitIgnore) {
                println("using gitignore patterns from .gitignore files")
            } else {
                println("no .gitignore files found")
            }
            foundGitIgnore
        } else {
            println("gitignore disabled by user")
            false
        }

        return buildTreeRecursively(path, "", config, gitIgnoreHandler, usingGitIgnore)
    }

    private fun buildTreeRecursively(
        path: Path,
        prefix: String = "",
        config: TreeConfig,
        gitIgnoreHandler: GitIgnoreHandler,
        usingGitIgnore: Boolean,
        currentDepth: Int = 0
    ): String {
        val builder = StringBuilder()
        val entries = getFilteredEntries(path, config, gitIgnoreHandler, usingGitIgnore)
        val (dirs, files) = entries.partition { it.isDirectory() }

        if (prefix.isEmpty()) builder.append("${path.name}/\n")

        appendDirectories(builder, dirs, files, prefix, config, gitIgnoreHandler, usingGitIgnore, currentDepth)
        appendFiles(builder, files, prefix, config)

        return builder.toString()
    }

    private fun getFilteredEntries(
        path: Path,
        config: TreeConfig,
        gitIgnoreHandler: GitIgnoreHandler,
        usingGitIgnore: Boolean
    ): List<Path> {
        val customPatterns = config.customIgnorePatterns.map { pattern ->
            try {
                pattern.toRegex(RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                log.warn("treegen plugin - invalid regex pattern: $pattern, using as literal")
                Regex.escape(pattern).toRegex(RegexOption.IGNORE_CASE)
            }
        }

        return path.listDirectoryEntries()
            .filterNot { entry ->
                val name = entry.name

                val isIgnored = customPatterns.any { it.containsMatchIn(name) }
                val isHidden = name.startsWith(".") && !config.showHidden
                val isGitIgnored = usingGitIgnore && gitIgnoreHandler.isIgnored(entry)

                isIgnored || isHidden || isGitIgnored
            }
            .sortedBy { it.name }
    }

    private fun appendDirectories(
        builder: StringBuilder,
        dirs: List<Path>,
        files: List<Path>,
        prefix: String,
        config: TreeConfig,
        gitIgnoreHandler: GitIgnoreHandler,
        usingGitIgnore: Boolean,
        currentDepth: Int
    ) {
        dirs.forEachIndexed { index, dir ->
            val isLast = index == dirs.lastIndex && files.isEmpty()
            val newPrefix = prefix + if (isLast) "    " else "│   "
            val sizeInfo = if (config.includeSizes) " (${FileUtils.calculateDirSize(dir)})" else ""

            builder.append("$prefix├── ${dir.name}/$sizeInfo\n")
            builder.append(buildTreeRecursively(dir, newPrefix, config, gitIgnoreHandler, usingGitIgnore, currentDepth + 1))
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