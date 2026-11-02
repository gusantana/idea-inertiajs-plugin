package nz.hailwood.inertiajs.navigation

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl
import com.jetbrains.php.lang.psi.resolve.types.PhpType

class InertiaPageReferenceProvider : PsiReferenceProvider() {
    private val inertiaPhpTypes = listOf(
        PhpType().add("\\Inertia\\Inertia")
    )

    private val laravelRoutePhpTypes = listOf(
        PhpType().add("\\Route"),
        PhpType().add("\\Illuminate\\Support\\Facades\\Route")
    )

    private fun isInertiaClassReference(methodRef: MethodReferenceImpl): Boolean {
        return methodRef.name == "render" && inertiaPhpTypes.any {
            it.isConvertibleFrom(PhpType().add(methodRef.classReference), PhpIndex.getInstance(methodRef.project))
        }
    }

    private fun isLaravelRouteClassReference(methodRef: MethodReferenceImpl): Boolean {
        return methodRef.name == "inertia" && laravelRoutePhpTypes.any {
            it.isConvertibleFrom(PhpType().add(methodRef.classReference), PhpIndex.getInstance(methodRef.project))
        }
    }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val literalExpression = element as? StringLiteralExpression ?: return PsiReference.EMPTY_ARRAY

        val parameterList = literalExpression.parent as? ParameterList ?: return PsiReference.EMPTY_ARRAY
        val functionCallContext = parameterList.context

        // Position of the `$component` argument in a positional call:
        // - Inertia::render($component, $props)
        // - inertia($component, $props, $rootView)
        // - Route::inertia($uri, $component, $props)
        val componentPosition = when {
            functionCallContext is FunctionReferenceImpl && functionCallContext.name == "inertia" -> 0
            functionCallContext is MethodReferenceImpl && isInertiaClassReference(functionCallContext) -> 0
            functionCallContext is MethodReferenceImpl && isLaravelRouteClassReference(functionCallContext) -> 1
            else -> return PsiReference.EMPTY_ARRAY
        }

        // PHP 8 named arguments: Inertia::render(component: 'Page'), Route::inertia(component: 'Page', uri: '/url').
        // getParameter(name, index) returns the named argument when present, otherwise the positional one.
        val componentArgument = parameterList.getParameter("component", componentPosition)

        if (componentArgument !== literalExpression) {
            return PsiReference.EMPTY_ARRAY
        }

        return arrayOf(InertiaPageReference(element, element.valueRange))
    }
}
