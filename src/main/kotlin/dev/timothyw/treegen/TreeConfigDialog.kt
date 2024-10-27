package dev.timothyw.treegen

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class TreeConfigDialog(project: Project) : DialogWrapper(project) {
    private val showHiddenCheckBox = JBCheckBox("Show hidden files", false)
    private val includeSizesCheckBox = JBCheckBox("Show file sizes", false)
    private val maxDepthField = JBTextField("999")
    private val ignorePatternField = JBTextField()

    init {
        title = "Tree Generator Settings"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row { showHiddenCheckBox() }
        row { includeSizesCheckBox() }
        row("Max Depth:") { maxDepthField() }
        row("Additional Ignore Patterns (comma-separated):") { ignorePatternField() }
    }

    fun getConfig(): TreeConfig = TreeConfig(
        showHidden = showHiddenCheckBox.isSelected,
        includeSizes = includeSizesCheckBox.isSelected,
        maxDepth = maxDepthField.text.toIntOrNull() ?: -1,
        customIgnorePatterns = ignorePatternField.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    )
}