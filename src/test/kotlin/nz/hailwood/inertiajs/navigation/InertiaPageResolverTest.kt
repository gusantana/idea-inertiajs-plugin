package nz.hailwood.inertiajs.navigation

import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Ignore

@Ignore("TODO: habilitar na Fase 5 do port (ver skill svelte5-inertia3)")
class InertiaPageResolverTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("resources/js/Pages/Users/Index.tsx", "export default function Index() { return null }")
        myFixture.addFileToProject("resources/js/Pages/Users/Show.tsx", "export default function Show() { return null }")
        myFixture.addFileToProject("resources/js/Components/Button.tsx", "export default function Button() { return null }")
    }

    fun testResolvesPageByName() {
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val resolved = InertiaPageResolver.resolve(project, "Users/Index")

        assertNotNull(resolved)
        assertEquals("Index.tsx", resolved!!.name)
    }

    fun testDoesNotResolveMissingPage() {
        assertNull(InertiaPageResolver.resolve(project, "Users/Missing"))
    }

    fun testListsOnlyPagesUnderThePagesRoot() {
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val names = InertiaPageResolver.allPages(project).map { it.virtualFile.name }

        assertEquals(listOf("Index.tsx", "Show.tsx"), names)
    }
}
