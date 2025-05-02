package dev.timothyw.treegen

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import dev.timothyw.treegen.FileUtils.formattedFileSize
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class TreeGenerator {
    private val log = logger<TreeGenerator>()

    fun generateTree(path: Path, config: TreeConfig): String {
        println("TreeGen Plugin - Starting tree generation with patterns: ${config.customIgnorePatterns}")
        val intellijExcludedDirs = getIntelliJExcludedDirectories()
        println("intellij excluded directories: $intellijExcludedDirs")

        return buildTreeRecursively(path, "", config, intellijExcludedDirs)
    }

    private fun buildTreeRecursively(
        path: Path,
        prefix: String = "",
        config: TreeConfig,
        intellijExcludedDirs: List<String>,
        currentDepth: Int = 0
    ): String {
        val builder = StringBuilder()
        val entries = getFilteredEntries(path, config, intellijExcludedDirs)
        val (dirs, files) = entries.partition { it.isDirectory() }

        if (prefix.isEmpty()) builder.append("${path.name}/\n")

        appendDirectories(builder, dirs, files, prefix, config, intellijExcludedDirs, currentDepth)
        appendFiles(builder, files, prefix, config)

        return builder.toString()
    }

    private fun getFilteredEntries(
        path: Path,
        config: TreeConfig,
        intellijExcludedDirs: List<String>
    ): List<Path> {
        val customPatterns = config.customIgnorePatterns.map { pattern ->
            try {
                pattern.toRegex(RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                log.warn("TreeGen Plugin - Invalid regex pattern: $pattern, using as literal")
                Regex.escape(pattern).toRegex(RegexOption.IGNORE_CASE)
            }
        }

        return path.listDirectoryEntries()
            .filterNot { entry ->
                val name = entry.name
                val absolutePath = path.resolve(entry).toString()

                val isIgnored = customPatterns.any { it.containsMatchIn(name) }
                val isHidden = name.startsWith(".") && !config.showHidden
                val isIntelliJExcluded = entry.isDirectory() &&
                        intellijExcludedDirs.any { excludedPath -> absolutePath.startsWith(excludedPath) }

                isIgnored || isHidden || isIntelliJExcluded
            }
            .sortedBy { it.name }
    }

    private fun appendDirectories(
        builder: StringBuilder,
        dirs: List<Path>,
        files: List<Path>,
        prefix: String,
        config: TreeConfig,
        intellijExcludedDirs: List<String>,
        currentDepth: Int
    ) {
        dirs.forEachIndexed { index, dir ->
            val isLast = index == dirs.lastIndex && files.isEmpty()
            val newPrefix = prefix + if (isLast) "    " else "│   "
            val sizeInfo = if (config.includeSizes) " (${FileUtils.calculateDirSize(dir)})" else ""

            builder.append("$prefix├── ${dir.name}/$sizeInfo\n")
            builder.append(buildTreeRecursively(dir, newPrefix, config, intellijExcludedDirs, currentDepth + 1))
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

    private fun getIntelliJExcludedDirectories(): List<String> {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project == null) {
            println("no project available, skipping intellij exclusions")
            return emptyList()
        }
        println("found project: ${project.name}")

        return try {
            val excludedUrls = ModuleManager.getInstance(project).modules
                .flatMap { module ->
                    ModuleRootManager.getInstance(module).excludeRootUrls.toList()
                }
                .distinct()

            excludedUrls.mapNotNull { url ->
                try {
                    val path = VfsUtil.urlToPath(url)
                    File(path).canonicalPath
                } catch (e: Exception) {
                    log.warn("failed to convert excluded url to path: $url", e)
                    null
                }
            }
        } catch (e: Exception) {
            log.warn("error getting intellij excluded directories", e)
            emptyList()
        }
    }
}