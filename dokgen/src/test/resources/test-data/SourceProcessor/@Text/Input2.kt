package `test-data`.SourceProcessor.`@Text`

import org.openrndr.dokgen.annotations.Text


val foo = "bar"
fun main(args: Array<String>) {
    @Text
    """
        hello world
        $foo
    """
}
