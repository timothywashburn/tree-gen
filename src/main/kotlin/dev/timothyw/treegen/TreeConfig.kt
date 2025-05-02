package dev.timothyw.treegen

data class TreeConfig(
    val showHidden: Boolean = false,
    val ignoreExcluded: Boolean = true,
    val includeSizes: Boolean = false,
    val generateFile: Boolean = false,
    val customIgnorePatterns: Set<String> = emptySet()
)