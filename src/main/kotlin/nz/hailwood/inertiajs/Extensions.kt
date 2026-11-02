package nz.hailwood.inertiajs

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.php.composer.ComposerConfigUtils
import com.jetbrains.php.composer.ComposerUtils
import nz.hailwood.inertiajs.settings.InertiaSettingsService
import java.io.File

val Project.isInertia: Boolean
    get() = this.isInertiaFromComposer || this.isInertiaFromPackageJson

val Project.isInertiaFromComposer: Boolean
    get() {
        val composerJsonVirtualFile = ComposerUtils.findFileInProject(this, "composer.json") ?: return false
        return ComposerConfigUtils.getInstalledPackagesFromConfig(composerJsonVirtualFile)
            .any { it.name == "inertiajs/inertia-laravel" }
    }

private val inertiaPackages = listOf(
    "@inertiajs/inertia",
    "@inertiajs/react",
    "@inertiajs/vue2",
    "@inertiajs/vue3",
    "@inertiajs/svelte",
    "@inertia/react",
    "@inertia/vue2",
    "@inertia/vue3",
    "@inertia/svelte"
)

private val packageJsonDependencySections = listOf(
    "dependencies",
    "devDependencies",
    "peerDependencies",
    "optionalDependencies"
)

val Project.isInertiaFromPackageJson: Boolean
    get() {
        val packageJsonVirtualFile = PackageJsonUtil.findChildPackageJsonFile(this.guessProjectDir()) ?: return false
        val packageJson = PsiManager.getInstance(this).findFile(packageJsonVirtualFile) as? JsonFile ?: return false
        return packageJson.declaresAnyDependency(inertiaPackages)
    }

private fun JsonFile.declaresAnyDependency(names: List<String>): Boolean {
    val root = topLevelValue as? JsonObject ?: return false
    return packageJsonDependencySections
        .mapNotNull { root.findProperty(it)?.value as? JsonObject }
        .any { section -> names.any { section.findProperty(it) != null } }
}

@Suppress("unused")
val Project.isNotInertia: Boolean
    get() = !this.isInertia

fun VirtualFile.displayPath(project: Project): String {
    val pagesRoot = InertiaSettingsService.inertiaPagesRoot(project).plus("/").replace(File.separator, "/")

    return this.path.removePrefix(pagesRoot).removeSuffix(".${this.extension}")
}