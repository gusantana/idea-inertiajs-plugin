package nz.hailwood.inertiajs.props

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.IndexingTestUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import nz.hailwood.inertiajs.settings.InertiaSettingsService
import java.io.File
import java.nio.file.Files

class InertiaUndefinedPagePropInspectionTest : HeavyPlatformTestCase() {

    private lateinit var projectRoot: File

    override fun setUp() {
        super.setUp()

        projectRoot = Files.createTempDirectory("inertia-inspection").toFile()
        val rootVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectRoot)
            ?: error("Cannot open $projectRoot")

        val rootModel = ModuleRootManager.getInstance(myModule).modifiableModel
        rootModel.addContentEntry(rootVirtualFile)
        ApplicationManager.getApplication().runWriteAction { rootModel.commit() }

        myProject.getService(InertiaSettingsService::class.java).customInertiaPagesRoot =
            File(projectRoot, "resources/js/Pages").path.replace('\\', '/')
    }

    override fun tearDown() {
        try {
            projectRoot.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testReportsPropMissingFromSvelteProps() {
        val array = propsArrayWith(
            page = "Users/Index.svelte",
            props = "'users' => [], 'missing' => []",
        )

        assertEquals(listOf("missing"), undeclaredProps(array))
    }

    fun testKeepsDeclaredProps() {
        val array = propsArrayWith(
            page = "Users/Index.svelte",
            props = "'users' => [], 'roles' => []",
        )

        assertEmpty(undeclaredProps(array))
    }

    fun testSkipsPageWithRestProps() {
        val array = propsArrayWith(
            page = "Users/Index.svelte",
            props = "'users' => [], 'missing' => []",
            script = "let { users, ...rest } = ${'$'}props();",
        )

        assertEmpty(undeclaredProps(array))
    }

    fun testSkipsNonSveltePages() {
        val array = propsArrayWith(
            page = "Users/Index.tsx",
            props = "'users' => [], 'missing' => []",
            script = "export default function Index({ users }) { return null }",
        )

        assertEmpty(undeclaredProps(array))
    }

    private fun propsArrayWith(
        page: String,
        props: String,
        script: String = "let { users, roles } = ${'$'}props();",
    ): ArrayCreationExpression {
        val pageFile = addFile("resources/js/Pages/$page")
        val controller = addFile("app/Http/Controllers/PageController.php")

        ApplicationManager.getApplication().runWriteAction {
            VfsUtil.saveText(pageFile, if (page.endsWith(".svelte")) "<script>$script</script>" else script)
            VfsUtil.saveText(controller, "<?php inertia('${page.substringBeforeLast('.')}', [$props]);")
        }

        IndexingTestUtil.waitUntilIndexesAreReady(myProject)

        val psiFile = PsiManager.getInstance(myProject).findFile(controller) ?: error("No PSI for $controller")
        return PsiTreeUtil.findChildOfType(psiFile, ArrayCreationExpression::class.java)
            ?: error("No array in $controller")
    }

    private fun undeclaredProps(array: ArrayCreationExpression): List<String> =
        InertiaUndefinedPagePropInspection().undeclaredProps(array).map { it.contents }

    private fun addFile(relativePath: String): VirtualFile {
        val file = File(projectRoot, relativePath)
        file.parentFile.mkdirs()
        file.writeText("")

        return VfsUtil.findFileByIoFile(file, true) ?: error("Cannot create $relativePath")
    }
}
