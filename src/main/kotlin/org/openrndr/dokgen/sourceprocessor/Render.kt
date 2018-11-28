package org.openrndr.dokgen.sourceprocessor

import org.openrndr.dokgen.Doc

fun appTemplate(pkg: String, imports: List<String>, body: String): String {
    return """
package $pkg

${imports.joinToString("\n")}

fun main(args: Array<String>) {
${body.prependIndent(List(4) { " " }.joinToString(""))}
}
    """
}

fun renderDoc(doc: Doc, title: String? = null): String {
    val strDoc = doc.elements.fold("") { acc, el ->
        val str = when (el) {
            is Doc.Element.Code -> {
                """
                 |```kotlin
                 |${el.value}
                 |```
               """.trimMargin()
            }
            is Doc.Element.Markdown -> {
                el.text.trimIndent()
            }
            is Doc.Element.Media.Image -> {
                //"![${el.src.trim()}](${el.src.trim()})"
                """
               |<img src="${el.src.trim()}"/>
                """.trimMargin()
            }
            is Doc.Element.Media.Video -> {
                """
                |<video controls>
                |    <source src="${el.src.trim()}" type="video/mp4"></source>
                |</video>
                |""".trimMargin()
            }
        }
        acc + "\n" + str + "\n"
    }
    return title?.let {
        "# $title\n$strDoc"
    } ?: strDoc
}