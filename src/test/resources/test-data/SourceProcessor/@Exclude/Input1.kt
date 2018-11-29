@file:Suppress("UNUSED_EXPRESSION")

import org.openrndr.dokgen.annotations.Code
import org.openrndr.dokgen.annotations.Exclude
import org.openrndr.dokgen.annotations.Text


fun main(args: Array<String>) {


    @Text
    "here's some code"

    @Code
    run {
        println("hello world")

        @Exclude
        run {
            println("this should not make it to the doc")
        }
    }
}
