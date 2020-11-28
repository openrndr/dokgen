import org.openrndr.dokgen.annotations.Application
import org.openrndr.dokgen.annotations.Text


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