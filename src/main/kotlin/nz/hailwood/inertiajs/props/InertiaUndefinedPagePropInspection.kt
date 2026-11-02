package nz.hailwood.inertiajs.props

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiManager
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import nz.hailwood.inertiajs.navigation.InertiaCall
import nz.hailwood.inertiajs.navigation.InertiaPageResolver

/**
 * Warns about props passed to an Inertia page that the target Svelte 5 component does not read from `$props()`.
 *
 * Disabled by default: shared props, partial reloads and `$page.props` access make this kind of check noisy.
 */
class InertiaUndefinedPagePropInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is ArrayCreationExpression) return

                undeclaredProps(element).forEach { key ->
                    holder.registerProblem(
                        key,
                        "Prop '${key.contents}' is not declared in the Svelte page",
                        ProblemHighlightType.WEAK_WARNING,
                    )
                }
            }
        }

    /**
     * Keys of the [array] props argument that the target Svelte page does not destructure from `$props()`.
     *
     * Returns an empty list when the call cannot be analyzed safely: not an Inertia call, page is not a
     * Svelte component, the component uses `export let` instead of `$props()`, the destructuring has a
     * `...rest`, or the array has dynamic/spread items.
     */
    internal fun undeclaredProps(array: ArrayCreationExpression): List<StringLiteralExpression> {
        val parameterList = array.parent as? ParameterList ?: return emptyList()
        val componentPosition = InertiaCall.componentPosition(parameterList) ?: return emptyList()
        if (InertiaCall.propsArgument(parameterList, componentPosition) !== array) return emptyList()

        // Spread or computed keys make the comparison meaningless.
        val elements = array.hashElements.toList()
        if (elements.any { it.key !is StringLiteralExpression }) return emptyList()

        val component = InertiaCall.componentArgument(parameterList, componentPosition) as? StringLiteralExpression
            ?: return emptyList()

        val project = array.project
        val pageFile = InertiaPageResolver.resolveFile(project, component.contents) ?: return emptyList()
        if (pageFile.extension != "svelte") return emptyList()

        val page = PsiManager.getInstance(project).findFile(pageFile) ?: return emptyList()
        val svelteProps = InertiaPropsExtractor.svelteProps(page) ?: return emptyList()
        if (svelteProps.hasRest) return emptyList()

        return elements
            .mapNotNull { it.key as? StringLiteralExpression }
            .filter { it.contents !in svelteProps.names }
    }
}
