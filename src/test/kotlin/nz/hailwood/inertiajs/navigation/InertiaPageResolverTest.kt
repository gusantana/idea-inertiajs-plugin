package nz.hailwood.inertiajs.navigation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.IndexingTestUtil
import nz.hailwood.inertiajs.settings.InertiaSettingsService
import java.io.File
import java.nio.file.Files

/**
 * Uses a real directory on disk as the project content root, because [InertiaPageResolver] resolves the
 * pages root through [com.intellij.openapi.vfs.VfsUtil.findFileByIoFile] and [InertiaPageResolver.allPages]
 * relies on the filename index.
 */
class InertiaPageResolverTest : HeavyPlatformTestCase() {

    private lateinit var projectRoot: File

    override fun setUp() {
        super.setUp()

        projectRoot = Files.createTempDirectory("inertia-project").toFile()
        val rootVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectRoot)
            ?: error("Cannot open $projectRoot")

        val rootModel = ModuleRootManager.getInstance(myModule).modifiableModel
        rootModel.addContentEntry(rootVirtualFile)
        ApplicationManager.getApplication().runWriteAction { rootModel.commit() }

        myProject.getService(InertiaSettingsService::class.java).customInertiaPagesRoot =
            File(projectRoot, "resources/js/Pages").path.replace('\\', '/')

        addFile("resources/js/Pages/Users/Index.tsx")
        addFile("resources/js/Pages/User.tsx")
        addFile("resources/js/Components/Button.tsx")

        IndexingTestUtil.waitUntilIndexesAreReady(myProject)
    }

    override fun tearDown() {
        try {
            projectRoot.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testResolvesPageByName() {
        val resolved = InertiaPageResolver.resolve(myProject, "Users/Index")

        assertNotNull(resolved)
        assertEquals("Index.tsx", resolved!!.name)
    }

    fun testDoesNotResolveMissingPage() {
        assertNull(InertiaPageResolver.resolve(myProject, "Users/Missing"))
    }

    fun testDoesNotResolveFilesOutsideThePagesRoot() {
        assertNull(InertiaPageResolver.resolve(myProject, "Components/Button"))
    }

    fun testListsOnlyPagesUnderThePagesRoot() {
        val names = InertiaPageResolver.allPages(myProject).map { it.virtualFile.name }

        assertEquals(setOf("Index.tsx", "User.tsx"), names.toSet())
    }

    private fun addFile(relativePath: String): VirtualFile {
        val file = File(projectRoot, relativePath)
        file.parentFile.mkdirs()
        file.writeText("")

        return VfsUtil.findFileByIoFile(file, true) ?: error("Cannot create $relativePath")
    }
}
