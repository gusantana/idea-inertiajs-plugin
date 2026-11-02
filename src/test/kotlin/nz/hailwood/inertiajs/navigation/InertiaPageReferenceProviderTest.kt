package nz.hailwood.inertiajs.navigation

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Wiring test for the reference contributor. Only the `inertia()` helper is used here because it is
 * recognized by name; `Inertia::render()` and `Route::inertia()` need a resolved PHP class in the index.
 */
class InertiaPageReferenceProviderTest : BasePlatformTestCase() {

    fun testCreatesReferenceForPositionalComponent() {
        val references = referencesIn("<?php inertia('Users/Index');")

        assertEquals(1, references.size)
        assertEquals("Users/Index", references[0].value)
    }

    fun testCreatesReferenceForNamedComponent() {
        val references = referencesIn("<?php inertia(component: 'Users/Index');")

        assertEquals(1, references.size)
    }

    fun testIgnoresPropsValues() {
        val references = referencesIn("<?php inertia('Users/Index', ['users' => 'Users/Index']);")

        assertEquals(1, references.size)
    }

    fun testIgnoresCallsWithoutComponent() {
        assertEmpty(referencesIn("<?php inertia(props: ['users' => []]);"))
    }

    private fun referencesIn(code: String): List<InertiaPageReference> {
        val file = myFixture.configureByText("test.php", code)
        return PsiTreeUtil.collectElementsOfType(file, StringLiteralExpression::class.java)
            .flatMap { it.references.filterIsInstance<InertiaPageReference>() }
    }
}
