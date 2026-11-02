package nz.hailwood.inertiajs.navigation

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class InertiaCallTest : BasePlatformTestCase() {

    fun testGlobalHelperWithPositionalComponent() {
        val parameterList = parameterListOf("<?php inertia('Users/Index');")

        assertEquals(0, InertiaCall.componentPosition(parameterList))
        assertSame(literalOf(parameterList, 0), InertiaCall.componentArgument(parameterList, 0))
        assertNull(InertiaCall.propsArgument(parameterList, 0))
    }

    fun testGlobalHelperWithNamedComponent() {
        val parameterList = parameterListOf("<?php inertia(component: 'Users/Index');")

        assertEquals(0, InertiaCall.componentPosition(parameterList))
        assertSame(literalOf(parameterList, 0), InertiaCall.componentArgument(parameterList, 0))
    }

    fun testNamedComponentWinsOverPosition() {
        val parameterList = parameterListOf("<?php inertia(props: ['users' => []], component: 'Users/Index');")

        val component = InertiaCall.componentArgument(parameterList, 0)

        assertSame(literalOf(parameterList, 1), component)
    }

    fun testPropsArgumentFollowsComponent() {
        val parameterList = parameterListOf("<?php inertia('Users/Index', ['users' => []]);")

        assertSame(parameterList.parameters[1], InertiaCall.propsArgument(parameterList, 0))
    }

    fun testUnrelatedCallIsNotAnInertiaCall() {
        val parameterList = parameterListOf("<?php \$router->dispatch('Users/Index');")

        assertNull(InertiaCall.componentPosition(parameterList))
    }

    private fun parameterListOf(code: String): ParameterList {
        val file = myFixture.configureByText("test.php", code)
        return PsiTreeUtil.findChildOfType(file, ParameterList::class.java)
            ?: error("No parameter list in $code")
    }

    private fun literalOf(parameterList: ParameterList, index: Int): StringLiteralExpression =
        parameterList.parameters[index] as StringLiteralExpression
}
