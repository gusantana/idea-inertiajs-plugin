package nz.hailwood.inertiajs.navigation

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import nz.hailwood.inertiajs.displayPath
import nz.hailwood.inertiajs.settings.InertiaSettingsService
import java.io.File

/**
 * Resolves Inertia page names to files under the configured pages root.
 *
 * Resolution is O(1) (direct VFS lookup by path) instead of walking the whole pages tree,
 * and completion uses [FilenameIndex] instead of iterating the project content.
 */
object InertiaPageResolver {

    /** Extensions considered Inertia pages, in resolution priority order. */
    val pageExtensions: List<String> = listOf("js", "jsx", "ts", "tsx", "vue", "svelte")

    fun resolve(project: Project, pageName: String): PsiFile? {
        val file = resolveFile(project, pageName) ?: return null
        return PsiManager.getInstance(project).findFile(file)
    }

    /** Resolves a page name to its file, without requiring a registered file type (unlike [resolve]). */
    fun resolveFile(project: Project, pageName: String): VirtualFile? {
        val pagesRoot = pagesRoot(project) ?: return null
        val normalized = pageName.trim('/')
        if (normalized.isEmpty()) return null

        for (extension in pageExtensions) {
            val file = findRelativeFile(pagesRoot, "$normalized.$extension") ?: continue
            if (file.isDirectory) continue
            return file
        }

        return null
    }

    fun allPages(project: Project): List<PsiFile> {
        val pagesRoot = pagesRoot(project) ?: return emptyList()
        if (DumbService.isDumb(project)) return emptyList()

        val scope = GlobalSearchScope.projectScope(project)
        val psiManager = PsiManager.getInstance(project)

        return pageExtensions.asSequence()
            .flatMap { FilenameIndex.getAllFilesByExt(project, it, scope).asSequence() }
            .filter { VfsUtil.isAncestor(pagesRoot, it, false) }
            .mapNotNull { psiManager.findFile(it) }
            .sortedBy { it.virtualFile.displayPath(project) }
            .toList()
    }

    fun pagesRoot(project: Project): VirtualFile? =
        VfsUtil.findFileByIoFile(File(InertiaSettingsService.inertiaPagesRoot(project)), true)

    private fun findRelativeFile(root: VirtualFile, relativePath: String): VirtualFile? {
        var current: VirtualFile = root
        for (segment in relativePath.split('/')) {
            if (segment.isEmpty()) continue
            current = current.findChild(segment) ?: return null
        }
        return current
    }
}
