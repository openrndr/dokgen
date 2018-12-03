package org.openrndr.dokgen

import org.openrndr.dokgen.sourceprocessor.SourceProcessor
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import kotlin.test.assertEquals

fun testData(p: String): String {
    return File("src/test/resources/test-data/SourceProcessor", p).readText()
}

fun String.removeBlankLines(): String {
    return this.split("\n").filter { !it.isBlank() }.joinToString("\n")
}

object SourceProcessorSpec : Spek({

    describe("@Text") {

        describe("simple") {
            val src = testData("@Text/Input.kt")
            val expected = testData("@Text/Expected.md")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("returns the text itself") {
                assertEquals(expected.trim(), result.doc.trim())
            }
        }


        describe("given interpolated text"){
            val src = testData("@Text/Input2.kt")
            val expected = testData("@Text/Expected2.md")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("does not choke on it"){
                assertEquals(expected.trim(), result.doc.trim())
            }
        }
    }

    describe("@Code") {
        describe("Simple") {
            val src = testData("@Code/Input1.kt")
            val expected = testData("@Code/Expected1.md")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("returns text embedded in a markdownConf code block") {
                assertEquals(expected, result.doc.trim())
            }
        }

        describe("Block") {
            val src = testData("@Code/Input2.kt")
            val expected = testData("@Code/Expected2.md")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("returns contents of run block embedded in a markdownConf code block") {
                assertEquals(expected, result.doc.trim())
            }
        }
    }

    describe("@Exclude") {
        val src = testData("@Exclude/Input1.kt")
        val expected = testData("@Exclude/Expected1.md")
        val result =
            SourceProcessor.process(
                src,
                "test"
            )
        it("excludes annotated target from doc") {
            assertEquals(expected, result.doc.trim())
        }
    }

    describe("@Application") {
        describe("simple example") {
            val src = testData("@Application/Input1.kt")
            val expected = testData("@Application/Expected1.kt")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("copies imports, adds package directive and embeds annotated target in main method") {
                assertEquals(expected, result.appSources[0].trim())
            }
        }
        describe("containing @Exclude") {
            val src = testData("@Application/Input2.kt")
            val expected = testData("@Application/Expected2.kt")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("retains annotated target in application source") {
                assertEquals(expected.removeBlankLines(), result.appSources[0].trim().removeBlankLines())
            }
        }
        describe("containing @Text") {
            val src = testData("@Application/Input3.kt")
            val expected = testData("@Application/Expected3.kt")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("@Text is removed from application source") {
                assertEquals(expected.removeBlankLines(), result.appSources[0].trim().removeBlankLines())
            }
        }

        describe("containing @Media") {
            val src = testData("@Application/Input4.kt")
            val expected = testData("@Application/Expected4.kt")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("@Media is removed from application source") {
                assertEquals(expected.removeBlankLines(), result.appSources[0].trim().removeBlankLines())
            }
        }

        describe("can be used together with another annotation") {
            val src = testData("@Application/Input5.kt")
            val expectedDoc = testData("@Application/Expected5-Doc.md")
            val expectedApp = testData("@Application/Expected5-App.kt")
            val result =
                SourceProcessor.process(
                    src,
                    "test"
                )
            it("exports app source") {
                assertEquals(expectedApp.removeBlankLines(), result.appSources[0].removeBlankLines())
            }
            it("exports code doc") {
                assertEquals(expectedDoc.removeBlankLines(), result.doc.removeBlankLines())
            }
        }
    }


    describe("@Media") {
        describe("Image") {
            val src = testData("@Media/Input1.kt")
            val expected = testData("@Media/Expected1.md")
            val result = SourceProcessor.process(
                src,
                "test"
            )
            it("returns an image githubUrl") {
                assertEquals(expected, result.doc.trim())
            }
        }

        describe("Video") {
            val src = testData("@Media/Input2.kt")
            val expected = testData("@Media/Expected2.md")
            val result = SourceProcessor.process(
                src,
                "test"
            )
            it("returns a video tag") {
                assertEquals(expected, result.doc.trim())
            }
        }

        describe("links") {
            val src = testData("@Media/Input3.kt")
            val result = SourceProcessor.process(
                src,
                "test"
            )

            it("are collected") {
                assertEquals(listOf("image.png", "video.mp4"), result.media)
            }
        }
    }

    // TODO this needs all the cases
    describe("full qualified annotation references") {
        val src = testData("FullyQualified/Input.kt")
        val expected = testData("FullyQualified/Expected.md")
        val result = SourceProcessor.process(
            src,
            "test"
        )
        it("should work") {
            assertEquals(expected.trim().removeBlankLines(), result.doc.trim().removeBlankLines())
        }
    }
})