package nz.hailwood.inertiajs.props

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression

class InertiaPropsExtractorTest : BasePlatformTestCase() {

    fun testReadsStringKeysFromPropsArray() {
        val array = propsArrayOf("<?php Inertia::render('Page', ['users' => [], 'roles' => []]);")

        assertEquals(listOf("users", "roles"), InertiaPropsExtractor.phpPropKeys(array))
    }

    fun testSkipsArrayItemsWithoutKey() {
        val array = propsArrayOf("<?php Inertia::render('Page', ['users', 'roles' => []]);")

        assertEquals(listOf("roles"), InertiaPropsExtractor.phpPropKeys(array))
    }

    fun testReturnsEmptyWithoutArrayArgument() {
        assertEmpty(InertiaPropsExtractor.phpPropKeys(null))
    }

    fun testParsesSveltePropsPattern() {
        assertEquals(
            listOf("users", "roles", "page-title"),
            InertiaPropsExtractor.parsePatternKeys(" users, roles: userRoles, 'page-title': pageTitle, ...rest "),
        )
    }

    fun testIgnoresDefaultsAndRestInPattern() {
        assertEquals(
            listOf("users", "posts"),
            InertiaPropsExtractor.parsePatternKeys(" users, posts = [], ...rest "),
        )
    }

    fun testReadsPropsFromSvelteComponent() {
        val file = myFixture.configureByText(
            "Page.svelte",
            "<script>let { users, roles: userRoles } = ${'$'}props();</script>",
        )

        assertEquals(listOf("users", "roles"), InertiaPropsExtractor.sveltePropNames(file))
    }

    fun testReturnsEmptyWithoutPropsCall() {
        val file = myFixture.configureByText("Legacy.svelte", "<script>export let users;</script>")

        assertEmpty(InertiaPropsExtractor.sveltePropNames(file))
    }

    private fun propsArrayOf(code: String): ArrayCreationExpression {
        val file = myFixture.configureByText("test.php", code)
        return PsiTreeUtil.findChildOfType(file, ArrayCreationExpression::class.java)
            ?: error("No array in $code")
    }
}
