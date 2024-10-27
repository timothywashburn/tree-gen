package dev.timothyw.treegen

data class TreeConfig(
    val showHidden: Boolean = false,
    val maxDepth: Int = -1,
    val customIgnorePatterns: Set<String> = emptySet(),
    val includeSizes: Boolean = false
)