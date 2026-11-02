package nz.hailwood.inertiajs.props

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Reads prop names from both sides of an Inertia page:
 *
 * - PHP: the string keys of the array passed as the `$props` argument.
 * - Svelte 5: the names destructured from `$props()`.
 *
 * The Svelte side relies on the JavaScript PSI that the Svelte plugin builds for `<script>` blocks,
 * so it returns an empty list when the file has no JavaScript PSI (for example when the Svelte plugin is not installed).
 */
object InertiaPropsExtractor {

    private const val PROPS_CALL = "\$props"

    /** Keys of the array passed as the props argument of an Inertia call. */
    fun phpPropKeys(propsArgument: PsiElement?): List<String> {
        val array = propsArgument as? ArrayCreationExpression ?: return emptyList()
        return array.hashElements.mapNotNull { element ->
            (element.key as? StringLiteralExpression)?.contents
        }
    }

    /**
     * Prop names destructured from `$props()` in a Svelte 5 component.
     *
     * `let { users, roles: userRoles, 'page-title': pageTitle } = $props();`
     * resolves to `[users, roles, page-title]`.
     */
    fun sveltePropNames(page: PsiFile): List<String> = svelteProps(page)?.names ?: emptyList()

    /** Destructured props of a Svelte 5 component, or null when it does not use `$props()`. */
    fun svelteProps(page: PsiFile): SvelteProps? {
        val propsCall = PsiTreeUtil.collectElementsOfType(page, JSCallExpression::class.java)
            .firstOrNull { it.methodExpression?.text == PROPS_CALL }
            ?: return null

        val pattern = destructuringPatternBefore(propsCall) ?: return null

        return SvelteProps(
            names = parsePatternKeys(pattern),
            hasRest = splitTopLevel(pattern, ',').any { it.trim().startsWith("...") },
        )
    }

    data class SvelteProps(val names: List<String>, val hasRest: Boolean)

    private fun destructuringPatternBefore(call: PsiElement): String? {
        var scope: PsiElement = call

        repeat(5) {
            val parent = scope.parent ?: return null
            scope = parent

            val callStart = call.textOffset - scope.textOffset
            if (callStart <= 0 || callStart > scope.textLength) return@repeat

            val before = scope.text.substring(0, callStart)
            val open = before.lastIndexOf('{')
            if (open < 0) return@repeat

            val close = findClosingBrace(before, open)
            if (close < 0) return@repeat

            // The destructuring pattern is followed by the assignment operator: `{ a, b } = $props()`.
            if (!before.substring(close + 1).contains('=')) return@repeat

            return before.substring(open + 1, close)
        }

        return null
    }

    internal fun parsePatternKeys(pattern: String): List<String> =
        splitTopLevel(pattern, ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("...") }
            .mapNotNull { entry ->
                val colon = topLevelIndexOf(entry, ':')
                val rawKey = if (colon >= 0) entry.substring(0, colon) else entry.substringBefore('=').trim()
                normalizedKey(rawKey)
            }

    private fun normalizedKey(rawKey: String): String? {
        val key = rawKey.trim()
        if (key.isEmpty()) return null

        val quoted = key.length > 1 && (key.first() == '\'' || key.first() == '"')
        val value = if (quoted) key.substring(1, key.length - 1) else key

        return value.takeIf { quoted || value.matches(IDENTIFIER) }
    }

    private val IDENTIFIER = Regex("[A-Za-z_$][A-Za-z0-9_$]*")

    private fun splitTopLevel(text: String, separator: Char): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var quote: Char? = null
        var start = 0

        text.forEachIndexed { index, char ->
            when {
                quote != null -> if (char == quote) quote = null
                char == '\'' || char == '"' || char == '`' -> quote = char
                char in OPENING -> depth++
                char in CLOSING -> depth--
                char == separator && depth == 0 -> {
                    parts.add(text.substring(start, index))
                    start = index + 1
                }
            }
        }

        parts.add(text.substring(start))
        return parts
    }

    private fun topLevelIndexOf(text: String, target: Char): Int {
        var depth = 0
        var quote: Char? = null

        text.forEachIndexed { index, char ->
            when {
                quote != null -> if (char == quote) quote = null
                char == '\'' || char == '"' || char == '`' -> quote = char
                char in OPENING -> depth++
                char in CLOSING -> depth--
                char == target && depth == 0 -> return index
            }
        }

        return -1
    }

    private fun findClosingBrace(text: String, openIndex: Int): Int {
        var depth = 0
        var quote: Char? = null

        for (index in openIndex until text.length) {
            val char = text[index]
            when {
                quote != null -> if (char == quote) quote = null
                char == '\'' || char == '"' || char == '`' -> quote = char
                char == '{' -> depth++
                char == '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }

        return -1
    }

    private val OPENING = setOf('{', '[', '(')
    private val CLOSING = setOf('}', ']', ')')
}
