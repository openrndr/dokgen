package org.openrndr.dokgen

import org.openrndr.dokgen.DokGen.prefix
import org.openrndr.dokgen.DokGen.splitCamelCased
import org.openrndr.dokgen.DokGen.untilFirstNumSequence
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

fun testFile(p: String): File {
    return File("src/test/resources/test-data/DokGen", p)
}

object DokGenSpek : Spek(
    {

        describe("prefix") {
            it("should find a prefix like 01_ in a string") {
                val input = "001_File"
                val result = prefix(input)
                assertEquals("001_", result)
            }
        }

        describe("untilFirstCharSequence") {
            it("should return substring until end of first char sequence from input string") {
                val input = "ABC0001FooBarBaz";
                val result = untilFirstNumSequence(input)
                assertEquals("ABC0001", result)
            }
            it("should return null if input has no char sequence") {
                val input = "FooBarBaz"
                val result = untilFirstNumSequence(input)
                assertNull(result)
            }
        }


        describe("splitCamelCased") {
            it("should split a camelcased string to words") {
                assertEquals(
                    listOf("Foo", "Bar", "Baz"),
                    splitCamelCased("FooBarBaz")
                )

                assertEquals(
                    listOf("foo", "Bar", "Baz"),
                    splitCamelCased("fooBarBaz")
                )

                assertEquals(
                    listOf("foobarbaz"),
                    splitCamelCased("foobarbaz")
                )
            }
        }


        describe("generateIndex") {
            it("should produce correctly sorted and formatted output") {
                val fileTree = testFile("FileTree")
                val result = DokGen.generateIndex(fileTree)
                val expected = """
                |- Chapter One
                |  - Sub Chapter One
                |      - [Deeply Nested Article](01_ChapterOne/01_SubChapterOne/DeeplyNestedArticle.md)
                |  - Sub Chapter 2
                |  - [Article Mark Down](01_ChapterOne/ArticleMarkDown.md)
                |  - [Article Non Prefixed](01_ChapterOne/ArticleNonPrefixed.md)
                |  - [Article Foo Bar](01_ChapterOne/C001ArticleFooBar.md)
                |  - [Article ${'$'} Baz ? Qux !!!](01_ChapterOne/C002ArticleBazQux.md)
                |- Chapter Two Snake Cased
                |- Chapter Three
                """.trimMargin()

                println(result)
                println("--")
                println(expected)
                assertEquals(expected, result)
            }
        }

    }
)