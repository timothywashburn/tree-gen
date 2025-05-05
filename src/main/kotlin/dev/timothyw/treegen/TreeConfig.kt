package dev.timothyw.treegen

data class TreeConfig(
    val showHidden: Boolean = false,
    val includeSizes: Boolean = false,
    val generateFile: Boolean = false,
    val useGitIgnore: Boolean = true,
    val customIgnorePatterns: Set<String> = emptySet()
)