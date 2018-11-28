package org.openrndr.dokgen

import org.openrndr.dokgen.sourceprocessor.SourceProcessor
import java.io.File
import java.nio.file.Path

fun File.relativeDir(file: File): Path {
    return toPath().relativize(file.toPath()).parent ?: File("").toPath()
}

object DokGen {
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

    fun generateIndex(sourcesRoot: File): String {

        fun go(root: File, depth: Int): String {
            val indent = List(depth) { "  " }.joinToString("")
            return root.listFiles().map { file ->
                val title = file.nameWithoutExtension.filter {
                    it.isLetter()
                }.capitalize()
                val result = if (file.isDirectory) {
                    "- $title" + "\n" + go(file, depth + 1)
                } else {
                    val link = file.relativeTo(sourcesRoot).toString().replace("kt", "md")
                    "- [$title]($link)"
                }
                result.prependIndent(indent)
            }.joinToString("\n")
        }

        return go(sourcesRoot, 0)
    }


    fun processSources(
        sourceFiles: List<File>,
        sourcesRoot: File,
        mdOutputDir: File,
        examplesOutputDir: File,
        webRootUrl: String?
    ) {


        sourceFiles.forEach { file ->
            println("processing $file")
            val packageDirective =
                determinePackageDirective(
                    sourcesRoot,
                    file
                )


            val srcFileName = file.nameWithoutExtension
            val relativeDir = sourcesRoot.relativeDir(file).toString()


            val mkLink = webRootUrl?.let { it ->
                { index: Int ->
                    val paddedIndex = "$index".padStart(3, '0')
                    "$it/examples/$relativeDir/$srcFileName$paddedIndex.kt"
                }
            }

            val fileContents = file.readText()
            val result = SourceProcessor.process(
                fileContents,
                packageDirective = packageDirective,
                mkLink = mkLink
            )

            val docsOutDir = File(mdOutputDir, relativeDir)
            docsOutDir.mkdirs()

            val md = File(docsOutDir, "$srcFileName.md")
            md.writeText(result.doc)


            val samplesOutDir = File(examplesOutputDir, relativeDir)
            samplesOutDir.mkdirs()
            result.appSources.forEachIndexed { index, s ->
                val paddedIndex = "$index".padStart(3, '0')
                val sampleOutFile = File(samplesOutDir, "$srcFileName$paddedIndex.kt")
                sampleOutFile.writeText(s)
            }
        }
        val index = generateIndex(sourcesRoot)
        File(mdOutputDir, "_sidebar.md").writeText(index)
    }

    fun getExampleClasses(sourceFiles: List<File>, sourcesRoot: File): List<String> {
        return sourceFiles.filter {
            it.extension == "kt"
        }.map { file ->
            val pd = determinePackageDirective(sourcesRoot, file).filter { ch -> ch != '`' }
            "$pd.${file.nameWithoutExtension}Kt"
        }
    }
}
