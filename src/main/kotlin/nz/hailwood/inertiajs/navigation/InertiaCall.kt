package nz.hailwood.inertiajs.navigation

import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl
import com.jetbrains.php.lang.psi.resolve.types.PhpType

/**
 * Recognizes the Inertia page calls and locates their `$component` and `$props` arguments:
 *
 * - `Inertia::render($component, $props)`
 * - `inertia($component, $props, $rootView)`
 * - `Route::inertia($uri, $component, $props)`
 *
 * Named arguments (PHP 8) are resolved by name first, then by position.
 * Only public PHP PSI is used: `ParameterList.getParameters()` plus the `name:` tokens around each argument.
 */
object InertiaCall {
    private const val COMPONENT_ARGUMENT = "component"
    private const val PROPS_ARGUMENT = "props"

    private val inertiaPhpTypes = listOf(PhpType().add("\\Inertia\\Inertia"))
    private val laravelRoutePhpTypes = listOf(
        PhpType().add("\\Route"),
        PhpType().add("\\Illuminate\\Support\\Facades\\Route")
    )

    /** Index of the `$component` argument, or null when [parameterList] is not an Inertia page call. */
    fun componentPosition(parameterList: ParameterList): Int? {
        val call = parameterList.context ?: return null
        return when {
            call is FunctionReferenceImpl && call.name == "inertia" -> 0
            call is MethodReferenceImpl && isInertiaRender(call) -> 0
            call is MethodReferenceImpl && isRouteInertia(call) -> 1
            else -> null
        }
    }

    fun componentArgument(parameterList: ParameterList, position: Int): PsiElement? =
        argument(parameterList, COMPONENT_ARGUMENT, position)

    /** The props argument follows `$component` in all three call forms. */
    fun propsArgument(parameterList: ParameterList, position: Int): PsiElement? =
        argument(parameterList, PROPS_ARGUMENT, position + 1)

    /** The argument passed as `name: value`, falling back to the positional argument at [position]. */
    private fun argument(parameterList: ParameterList, name: String, position: Int): PsiElement? {
        val parameters = parameterList.parameters
        return parameters.firstOrNull { namedArgumentName(it) == name } ?: parameters.getOrNull(position)
    }

    /** Returns `name` for `name: value`, or null for a positional argument. */
    private fun namedArgumentName(argument: PsiElement): String? {
        val colon = previousMeaningfulSibling(argument) ?: return null
        if (colon.text != ":") return null
        return previousMeaningfulSibling(colon)?.text
    }

    private fun previousMeaningfulSibling(element: PsiElement): PsiElement? {
        var sibling = element.prevSibling
        while (sibling != null && sibling.text.isBlank()) {
            sibling = sibling.prevSibling
        }
        return sibling
    }

    private fun isInertiaRender(methodRef: MethodReferenceImpl): Boolean =
        methodRef.name == "render" && matches(methodRef, inertiaPhpTypes)

    private fun isRouteInertia(methodRef: MethodReferenceImpl): Boolean =
        methodRef.name == "inertia" && matches(methodRef, laravelRoutePhpTypes)

    private fun matches(methodRef: MethodReferenceImpl, types: List<PhpType>): Boolean {
        val index = PhpIndex.getInstance(methodRef.project)
        return types.any { it.isConvertibleFrom(PhpType().add(methodRef.classReference), index) }
    }
}
