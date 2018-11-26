package org.openrndr.docgen


data class Doc(
    val elements: MutableList<Element> = mutableListOf()
) {
    sealed class Element {
        class Code(val value: String) : Element()
        class Markdown(val text: String) : Element()
        sealed class Media : Element() {
            class Image(val src: String) : Media()
            class Video(val src: String) : Media()
        }
    }
}