package nz.hailwood.inertiajs.props

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import nz.hailwood.inertiajs.navigation.InertiaCall
import nz.hailwood.inertiajs.navigation.InertiaPageResolver

/**
 * Suggests the props declared by the `$props()` of the target Svelte 5 page while typing the keys of the
 * props array in `Inertia::render('Page', ['<caret>' => ...])`.
 */
class InertiaPropCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
            InertiaPropCompletionProvider(),
        )
    }
}

private class InertiaPropCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val literal = PsiTreeUtil.getParentOfType(parameters.position, StringLiteralExpression::class.java, false)
            ?: return

        // Only complete array keys, not array values: Inertia::render('Page', ['key' => 'value'])
        val hashElement = literal.parent as? ArrayHashElement ?: return
        if (hashElement.key !== literal) return

        val array = hashElement.parent as? ArrayCreationExpression ?: return
        val parameterList = array.parent as? ParameterList ?: return

        val componentPosition = InertiaCall.componentPosition(parameterList) ?: return
        val component = InertiaCall.componentArgument(parameterList, componentPosition) as? StringLiteralExpression
            ?: return

        val page = InertiaPageResolver.resolve(literal.project, component.contents) ?: return
        val propNames = InertiaPropsExtractor.sveltePropNames(page)
        if (propNames.isEmpty()) return

        val valuePrefix = valuePrefix(literal, parameters.offset)
        val prefixed = result.withPrefixMatcher(PlainPrefixMatcher(valuePrefix))

        propNames.forEach { propName ->
            prefixed.addElement(
                LookupElementBuilder.create(propName)
                    .withTypeText(page.name, true)
            )
        }
    }

    /** The already typed part of the string literal value, without the quotes. */
    private fun valuePrefix(literal: StringLiteralExpression, caretOffset: Int): String {
        val valueRange = literal.valueRange
        val offsetInLiteral = caretOffset - literal.textRange.startOffset - valueRange.startOffset
        val length = offsetInLiteral.coerceIn(0, literal.contents.length)
        return literal.contents.substring(0, length)
    }
}
