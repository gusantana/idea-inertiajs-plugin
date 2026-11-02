package nz.hailwood.inertiajs.navigation

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import org.junit.Ignore

@Ignore("TODO: habilitar na Fase 5 do port (ver skill svelte5-inertia3)")
class InertiaPageReferenceProviderTest : BasePlatformTestCase() {

    private val inertiaStubs = """
        <?php

        namespace Inertia {
            class Inertia {
                public static function render(${'$'}component, ${'$'}props = [], ${'$'}rootView = 'app') {}
            }
        }

        namespace {
            class Route {
                public static function inertia(${'$'}uri, ${'$'}component = null, ${'$'}props = []) {}
            }

            function inertia(${'$'}component = null, ${'$'}props = [], ${'$'}rootView = 'app') {}
        }
    """.trimIndent()

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("vendor/inertia-stubs.php", inertiaStubs)
        myFixture.addFileToProject("resources/js/Pages/Users/Index.tsx", "export default function Index() { return null }")
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    fun testRenderWithPositionalArgument() {
        assertPageResolves("<?php \\Inertia\\Inertia::render('Users/Index');")
    }

    fun testRenderWithNamedArgument() {
        assertPageResolves("<?php \\Inertia\\Inertia::render(component: 'Users/Index');")
    }

    fun testRenderWithNamedArgumentOutOfOrder() {
        assertPageResolves("<?php \\Inertia\\Inertia::render(props: ['users' => []], component: 'Users/Index');")
    }

    fun testInertiaHelper() {
        assertPageResolves("<?php inertia('Users/Index');")
    }

    fun testRouteInertiaWithPositionalArguments() {
        assertPageResolves("<?php \\Route::inertia('/users', 'Users/Index');")
    }

    fun testRouteInertiaWithNamedArguments() {
        assertPageResolves("<?php \\Route::inertia(uri: '/users', component: 'Users/Index');")
    }

    fun testRouteInertiaDoesNotResolveTheUriAsPage() {
        val literal = configureLiteral("<?php \\Route::inertia('/users');")

        assertEmpty(literal.references.filterIsInstance<InertiaPageReference>())
    }

    private fun assertPageResolves(code: String) {
        val literal = configureLiteral(code)
        val reference = literal.references.filterIsInstance<InertiaPageReference>().firstOrNull()

        assertNotNull("No Inertia page reference on ${literal.text}", reference)
        val resolved: PsiElement? = reference!!.resolve()
        assertNotNull(resolved)
        assertEquals("Index.tsx", resolved!!.containingFile.name)
    }

    private fun configureLiteral(code: String): StringLiteralExpression {
        val file = myFixture.configureByText("test.php", code)
        return PsiTreeUtil.findChildOfType(file, StringLiteralExpression::class.java)
            ?: error("No string literal in $code")
    }
}
