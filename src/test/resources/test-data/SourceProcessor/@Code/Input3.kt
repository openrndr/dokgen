@file:Suppress("UNUSED_EXPRESSION")

package `test-data`.SourceProcessor.`@Code`

import `test-data`.SourceProcessor.`@Text`.foo
import org.openrndr.dokgen.annotations.Code


@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class MyAnnotation

fun main(args: Array<String>) {
    class Foo(var value: String = "foo")

    val foo = Foo()

    ""

    @Code {
        foo.apply {

        }
    }()
}