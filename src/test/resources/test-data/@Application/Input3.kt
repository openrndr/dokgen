import org.openrndr.docgen.annotations.Application
import org.openrndr.docgen.annotations.Text


fun main(args: Array<String>) {
    @Application
    run {
        println("hello world")

        @Text
        """
            this should not be in the resulting source
        """.trimIndent()
    }
}