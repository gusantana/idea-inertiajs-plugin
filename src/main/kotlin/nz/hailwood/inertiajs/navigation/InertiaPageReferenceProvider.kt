package nz.hailwood.inertiajs.navigation

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class InertiaPageReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val literalExpression = element as? StringLiteralExpression ?: return PsiReference.EMPTY_ARRAY

        val parameterList = literalExpression.parent as? ParameterList ?: return PsiReference.EMPTY_ARRAY

        // Position of the `$component` argument in a positional call:
        // - Inertia::render($component, $props)
        // - inertia($component, $props, $rootView)
        // - Route::inertia($uri, $component, $props)
        val componentPosition = InertiaCall.componentPosition(parameterList) ?: return PsiReference.EMPTY_ARRAY

        // PHP 8 named arguments: Inertia::render(component: 'Page'), Route::inertia(component: 'Page', uri: '/url').
        // getParameter(name, index) returns the named argument when present, otherwise the positional one.
        val componentArgument = InertiaCall.componentArgument(parameterList, componentPosition)

        if (componentArgument !== literalExpression) {
            return PsiReference.EMPTY_ARRAY
        }

        return arrayOf(InertiaPageReference(element, element.valueRange))
    }
}

