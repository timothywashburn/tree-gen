package dev.timothyw.treegen

import com.intellij.openapi.diagnostic.logger
import org.eclipse.jgit.ignore.IgnoreNode
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path

/**
 * Handles gitignore pattern matching using the JGit library
 */
class GitIgnoreHandler {
    private val log = logger<GitIgnoreHandler>()
    private val ignoreNodes = mutableMapOf<Path, IgnoreNode>()

    /**
     * Loads gitignore files from the given path and its parent directories
     *
     * @param rootPath The path to start searching from
     * @return true if any gitignore files were found and loaded
     */
    fun loadGitIgnoreFiles(rootPath: Path): Boolean {
        var currentDir = rootPath.toFile()
        var foundFiles = false

        while (currentDir != null) {
            val gitIgnoreFile = File(currentDir, ".gitignore")
            if (gitIgnoreFile.exists() && gitIgnoreFile.isFile) {
                try {
                    FileInputStream(gitIgnoreFile).use { inputStream ->
                        val ignoreNode = IgnoreNode()
                        ignoreNode.parse(inputStream)
                        if (ignoreNode.rules.isNotEmpty()) {
                            ignoreNodes[currentDir.toPath()] = ignoreNode
                            foundFiles = true
                            println("loaded gitignore from ${gitIgnoreFile.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    log.warn("Failed to parse .gitignore file at ${gitIgnoreFile.absolutePath}", e)
                }
            }
            currentDir = currentDir.parentFile
        }

        return foundFiles
    }

    /**
     * Checks if a file or directory should be ignored according to any loaded gitignore rules
     *
     * @param path The path to check
     * @return true if the path should be ignored
     */
    fun isIgnored(path: Path): Boolean {
        val pathFile = path.toFile()
        val isDirectory = pathFile.isDirectory

        if (pathFile.name == ".gitignore") return false

        for ((baseDirPath, ignoreNode) in ignoreNodes) {
            try {
                if (!path.startsWith(baseDirPath)) continue

                val relativePath = baseDirPath.relativize(path).toString().replace(File.separatorChar, '/')

                val result = ignoreNode.checkIgnored(relativePath, isDirectory)
                if (result != null) return result
            } catch (e: Exception) {
                log.warn("Error checking gitignore rules for $path", e)
            }
        }

        return false
    }
}