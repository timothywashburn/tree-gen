package dev.timothyw.treegen

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection
import java.nio.file.Path
import kotlin.io.path.isDirectory

class GenerateTreeAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val app = ApplicationManager.getApplication()
        if (app.isDisposed) {
            e.presentation.isEnabled = false
            return
        }

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabled = virtualFile?.isDirectory == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val app = ApplicationManager.getApplication()
        if (app.isDisposed) {
            return
        }

        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val path = Path.of(virtualFile.path)
        if (!path.isDirectory()) {
            Messages.showErrorDialog(project, "Please select a directory", "Error")
            return
        }

        app.invokeLater({
            val config = TreeConfigDialog(project).let { dialog ->
                if (dialog.showAndGet()) dialog.getConfig() else return@invokeLater
            }

            val tree = TreeGenerator().generateTree(path, config)

            // Copy to clipboard
            CopyPasteManager.getInstance().setContents(StringSelection(tree))

            // Generate file if requested
            if (config.generateFile) {
                generateTreeFile(project, tree)
            }

            app.invokeLater({
                val message = if (config.generateFile) {
                    "Directory tree generated and copied to clipboard! File created in project root."
                } else {
                    "Directory tree copied to clipboard!"
                }
                Messages.showInfoMessage(project, message, "Success")
            }, ModalityState.NON_MODAL)
        }, ModalityState.NON_MODAL)
    }

    private fun generateTreeFile(project: Project, treeContent: String) {
        // Get project base path
        val projectPath = project.basePath ?: return
        val projectRoot = LocalFileSystem.getInstance().findFileByPath(projectPath) ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val treeFile = projectRoot.findChild("directory-tree.txt")
                    ?: projectRoot.createChildData(this, "directory-tree.txt")

                val document = FileDocumentManager.getInstance().getDocument(treeFile)
                document?.setText(treeContent)
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater({
                    Messages.showErrorDialog(
                        project,
                        "Failed to generate tree file: ${e.message}",
                        "Error"
                    )
                }, ModalityState.NON_MODAL)
            }
        }
    }
}