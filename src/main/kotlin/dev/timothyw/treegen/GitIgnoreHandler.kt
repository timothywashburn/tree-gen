package dev.timothyw.treegen

import com.intellij.openapi.diagnostic.logger
import org.eclipse.jgit.ignore.IgnoreNode
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path

class GitIgnoreHandler {
    private val log = logger<GitIgnoreHandler>()
    private val ignoreNodes = mutableMapOf<Path, IgnoreNode>()

    /**
     * Loads a .gitignore file from a specific directory if it exists and hasn't been loaded yet
     *
     * @param directoryPath The directory to load .gitignore from
     * @return true if a .gitignore file was loaded
     */
    fun loadGitIgnoreFromDirectory(path: Path): Boolean {
        if (ignoreNodes.containsKey(path)) return false

        val gitIgnoreFile = File(path.toFile(), ".gitignore")
        if (gitIgnoreFile.exists() && gitIgnoreFile.isFile) {
            try {
                FileInputStream(gitIgnoreFile).use { inputStream ->
                    val ignoreNode = IgnoreNode()
                    ignoreNode.parse(inputStream)
                    if (ignoreNode.rules.isNotEmpty()) {
                        ignoreNodes[path] = ignoreNode
                        println("loaded gitignore from ${gitIgnoreFile.absolutePath}")
                        return true
                    }
                }
            } catch (e: Exception) {
                log.warn("Failed to parse .gitignore file at ${gitIgnoreFile.absolutePath}", e)
            }
        }

        return false
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