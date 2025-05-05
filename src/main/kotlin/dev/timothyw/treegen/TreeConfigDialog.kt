package dev.timothyw.treegen

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TreeConfigDialog(project: Project) : DialogWrapper(project) {
    private val showHiddenCheckBox = JBCheckBox("Show hidden files", false)
    private val useGitIgnoreCheckBox = JBCheckBox("Use .gitignore patterns", true)
    private val includeSizesCheckBox = JBCheckBox("Show file and folder sizes", false)
    private val generateFileCheckBox = JBCheckBox("Generate file in project root", false)
    private val ignorePatternField = JBTextField()

    init {
        title = "Tree Gen Settings"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        group {
            row {
                cell(showHiddenCheckBox)
            }
            row {
                cell(useGitIgnoreCheckBox)
            }
            row {
                cell(includeSizesCheckBox)
            }
            row {
                cell(generateFileCheckBox)
            }
            row("Additional patterns to ignore (comma-separated)") {}
            row {
                cell(ignorePatternField)
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
        }
    }

    fun getConfig(): TreeConfig = TreeConfig(
        showHidden = showHiddenCheckBox.isSelected,
        includeSizes = includeSizesCheckBox.isSelected,
        generateFile = generateFileCheckBox.isSelected,
        useGitIgnore = useGitIgnoreCheckBox.isSelected,
        customIgnorePatterns = ignorePatternField.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    )
}