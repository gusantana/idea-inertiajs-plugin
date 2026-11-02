package nz.hailwood.inertiajs.navigation

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import nz.hailwood.inertiajs.displayPath

class InertiaPageReference(element: PsiElement, textRange: TextRange) : PsiReferenceBase<PsiElement?>(element, textRange) {

    private val key: String = element.text.substring(textRange.startOffset, textRange.endOffset)

    override fun resolve(): PsiElement? = InertiaPageResolver.resolve(element.project, key)

    override fun getVariants(): Array<LookupElement> = InertiaPageResolver.allPages(element.project).map {
        LookupElementBuilder
            .create(it.virtualFile.displayPath(element.project))
            .withIcon(it.fileType.icon)
    }.toTypedArray()

}
