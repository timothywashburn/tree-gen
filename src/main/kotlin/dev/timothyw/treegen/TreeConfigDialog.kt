package dev.timothyw.treegen

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class TreeConfigDialog(project: Project) : DialogWrapper(project) {
    private val log = Logger.getInstance(TreeConfigDialog::class.java)

    private val showHiddenCheckBox = JBCheckBox("Show hidden files", false)
    private val includeSizesCheckBox = JBCheckBox("Show file sizes", false)
    private val generateFileCheckBox = JBCheckBox("Generate file in project root", false)
    private val maxDepthField = JBTextField("999")
    private val ignorePatternField = JBTextField()

    init {
        title = "Tree Generator Settings"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row { showHiddenCheckBox() }
        row { includeSizesCheckBox() }
        row { generateFileCheckBox() }
        row("Max Depth:") { maxDepthField() }
        row("Additional Ignore Patterns (comma-separated):") { ignorePatternField() }
    }

    fun getConfig(): TreeConfig {
        val patterns = ignorePatternField.text
        log.warn("TreeGen Plugin - Ignore patterns entered: '$patterns'")

        return TreeConfig(
            showHidden = showHiddenCheckBox.isSelected,
            includeSizes = includeSizesCheckBox.isSelected,
            generateFile = generateFileCheckBox.isSelected,
            maxDepth = maxDepthField.text.toIntOrNull() ?: -1,
            customIgnorePatterns = patterns
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
                .also { log.warn("TreeGen Plugin - Processed patterns: $it") }
        )
    }
}