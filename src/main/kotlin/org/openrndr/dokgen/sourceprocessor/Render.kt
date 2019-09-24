package org.openrndr.dokgen.sourceprocessor

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
                // docsify tries to be smart and does some magic to the links which breaks them
                // so rendering directly into an html img tag instead of markdown
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
        "$acc \n $str \n"
        //acc + "\n" + str + "\n"
    }
    return title?.let {
        "# $title\n$strDoc"
    } ?: strDoc
}