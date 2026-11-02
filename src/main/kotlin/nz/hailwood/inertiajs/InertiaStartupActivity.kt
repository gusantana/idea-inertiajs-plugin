package nz.hailwood.inertiajs

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import nz.hailwood.inertiajs.settings.InertiaSettingsService
import nz.hailwood.inertiajs.settings.ShowSettingsAction
import java.io.File

class InertiaStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val pagesRootMissing = readAction {
            project.isInertia &&
                VfsUtil.findFileByIoFile(File(InertiaSettingsService.inertiaPagesRoot(project)), true) == null
        }

        if (!pagesRootMissing) return

        Notification(
            "Inertia Plugin",
            "Default Inertia.js pages root not found",
            "Please configure the path appropriately in settings.",
            NotificationType.WARNING
        )
            .addAction(ShowSettingsAction())
            .notify(project)
    }

}
