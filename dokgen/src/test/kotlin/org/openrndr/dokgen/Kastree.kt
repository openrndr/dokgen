package org.openrndr.dokgen

import kastree.ast.psi.Converter
import kastree.ast.psi.Parser
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object Kastree : Spek({
    describe("a simple source code") {

        val code = """
package docs.intro

import org.openrndr.dokgen.annotations.*

fun main() {

}
        """

        it("should parse without errors") {
            val file = Parser.parseFile(code)

        }
        it("should parse psi without errors") {
            val psiFile = Parser.parsePsiFile(code)
        }
        it("should parse with extra") {
            val extrasMap = Converter.WithExtras()
            val ast = Parser(extrasMap).parseFile(code, throwOnError = true)

        }
    }
})