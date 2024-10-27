package dev.timothyw.treegen

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.nio.file.Path
import kotlin.io.path.isDirectory

class GenerateTreeAction : AnAction() {
    override fun update(e: AnActionEvent) {
        if (!ApplicationManager.getApplication().isInitialized) {
            e.presentation.isEnabled = false
            return
        }

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabled = virtualFile?.isDirectory == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (!ApplicationManager.getApplication().isInitialized) {
            return
        }

        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val path = Path.of(virtualFile.path)
        if (!path.isDirectory()) {
            Messages.showErrorDialog(project, "Please select a directory", "Error")
            return
        }

        ApplicationManager.getApplication().invokeLater({
            val config = TreeConfigDialog(project).let { dialog ->
                if (dialog.showAndGet()) dialog.getConfig() else return@invokeLater
            }

            val tree = TreeGenerator().generateTree(path, config)

            ApplicationManager.getApplication().runWriteAction {
                try {
                    val treeFile = virtualFile.findChild("directory-tree.txt")
                        ?: virtualFile.createChildData(this, "directory-tree.txt")

                    val document = FileDocumentManager.getInstance().getDocument(treeFile)
                    document?.setText(tree)

                    Messages.showInfoMessage(project, "Directory tree generated successfully!", "Success")
                } catch (e: Exception) {
                    Messages.showErrorDialog(
                        project,
                        "Failed to generate directory tree: ${e.message}",
                        "Error"
                    )
                }
            }
        }, ModalityState.NON_MODAL)
    }
}