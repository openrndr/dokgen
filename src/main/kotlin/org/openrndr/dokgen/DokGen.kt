package org.openrndr.dokgen

import org.openrndr.dokgen.sourceprocessor.SourceProcessor
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*

fun File.relativeDir(file: File): Path {
    return toPath().relativize(file.toPath()).parent ?: File("").toPath()
}

object DokGen {
    fun untilFirstNumSequence(string: String): String? {
        val folded = string.fold("") { acc, x ->
            if (acc.isNotEmpty() && acc.last().isDigit() && !x.isDigit()) {
                acc
            } else {
                acc + x.toString()
            }
        }
        return if (folded == string) {
            null
        } else {
            folded
        }
    }

    fun prefix(string: String): String? {
        return string.split("_").firstOrNull()?.let { it + "_" }
    }

    fun String.removeBlankLines(): String {
        return this.split("\n").filter { !it.isBlank() }.joinToString("\n")
    }

    fun splitCamelCased(string: String): List<String> {
        data class Acc(
            val current: String = "",
            val words: List<String> = listOf(),
            val position: Int = 0
        )

        val accumulated = string.fold(Acc()) { acc, cursor ->
            if (acc.position == string.length - 1) {
                acc.copy(
                    words = acc.words + (acc.current + cursor)
                )
            } else {
                if (acc.current.isNotEmpty() && acc.current.last().isLowerCase() && cursor.isUpperCase()) {
                    acc.copy(
                        current = cursor.toString(),
                        words = acc.words + acc.current,
                        position = acc.position + 1
                    )
                } else {
                    acc.copy(
                        current = acc.current + cursor,
                        position = acc.position + 1
                    )
                }
            }
        }
        return accumulated.words
    }

    private fun determinePackageDirective(sourcesRoot: File, sourceFile: File): String {
        val dirPathRelativeToRoot = sourcesRoot.relativeDir(sourceFile)
        val parts = dirPathRelativeToRoot.map { part ->
            val str = part.toString()
            val special = str.any { ch -> !ch.isLetter() }
            if (special) {
                "`$str`"
            } else {
                str
            }
        }
        val relativePackageDirective = parts.joinToString(".")
        return "examples.$relativePackageDirective"
    }


    // traverses the file tree under sourcesRoot transforming it into a markdown representation
    // that is suitable to use as a description of the sidebar in docsify
    fun generateIndex(sourcesRoot: File): String {

        fun go(root: File, depth: Int = 0): String {
            val indent = List(depth) { "  " }.joinToString("")
            val files = root.listFiles()
            val properties = files.find { it.name.endsWith("properties") }?.let {
                val props = Properties()
                props.load(it.inputStream())
                props.stringPropertyNames().associate {
                    it to props.getProperty(it)
                }
            }
            return files.filter { !it.name.endsWith("properties") }
                .sortedBy {
                    untilFirstNumSequence(it.name) ?: it.name
                }.map { file ->
                    val maybeTitleFromProperties = properties?.let { it[file.nameWithoutExtension] }
                    val title = maybeTitleFromProperties ?: file.nameWithoutExtension.run {
                        val name = this

                        val withoutPrefix = prefix(name)?.let { prefix ->
                            name.replace(prefix, "")
                        } ?: name

                        val withoutCharSequence = untilFirstNumSequence(withoutPrefix)?.let { subStr ->
                            withoutPrefix.replace(subStr, " ")
                        } ?: withoutPrefix

                        splitCamelCased(withoutCharSequence).joinToString(" ").replace("_", " ").trim()
                    }

                    // result
                    if (file.isDirectory) {
                        "- $title" + "\n" + go(file, depth + 1)
                    } else {
                        val link = file.relativeTo(sourcesRoot).toString().replace("kt", "md")
                        "- [$title]($link)"
                    }.prependIndent(indent)
                }.joinToString("\n").let {
                        println(it)
                        it
                    }
        }

        return go(sourcesRoot).removeBlankLines()
    }

    fun processSources(
        sourceFiles: List<File>,
        sourcesRoot: File,
        mdOutputDir: File,
        examplesOutputDir: File,
        examplesForExportOutputDir: File,
        webRootUrl: String?
    ) {
        sourceFiles.forEach { file ->
            val ext = file.extension
            val srcFileName = file.nameWithoutExtension
            val relativeDir = sourcesRoot.relativeDir(file).toString()
            val docsOutDir = File(mdOutputDir, relativeDir)
            val mdTarget = File(docsOutDir, "$srcFileName.md")
            docsOutDir.mkdirs()

            when (ext) {
                "md" -> {
                    file.copyTo(mdTarget, overwrite = true)
                }
                "kt" -> {
                    val packageDirective =
                            determinePackageDirective(
                                    sourcesRoot,
                                    file
                            )

                    val makeLink = webRootUrl?.let { it ->
                        { index: Int ->
                            val paddedIndex = "$index".padStart(3, '0')
                            "$it/examples/$relativeDir/$srcFileName$paddedIndex.kt"
                        }
                    }


                    val fileContents = file.readText().replace("\r\n","\n")
                    val result = SourceProcessor.process(
                            fileContents,
                            packageDirective = packageDirective,
                            mkLink = makeLink
                    )

                    mdTarget.writeText(result.doc)

                    val examplesOutDir = File(examplesOutputDir, relativeDir)
                    examplesOutDir.mkdirs()
                    result.appSources.forEachIndexed { index, s ->
                        val paddedIndex = "$index".padStart(3, '0')
                        val sampleOutFile = File(examplesOutDir, "$srcFileName$paddedIndex.kt")
                        println("writing to $sampleOutFile")
                        sampleOutFile.writeText(s)
                    }

                    val examplesForExportOutDir = File(examplesForExportOutputDir, relativeDir)
                    examplesForExportOutDir.mkdirs()
                    result.appSourcesForExport.forEachIndexed { index, s ->
                        val paddedIndex = "$index".padStart(3, '0')
                        val sampleOutFile = File(examplesForExportOutDir, "$srcFileName$paddedIndex.kt")
                        sampleOutFile.writeText(s)
                    }
                }
            }
        }
        val index = generateIndex(sourcesRoot)
        File(mdOutputDir, "_sidebar.md").writeText(index)
    }

    fun getExamplesClassNames(sourceFiles: List<File>, sourcesRoot: File): List<String> {
        return sourceFiles.filter {
            it.extension == "kt"
        }.map { file ->
            val pd = determinePackageDirective(sourcesRoot, file).filter { ch -> ch != '`' }
            "$pd.${file.nameWithoutExtension}Kt"
        }
    }
}

