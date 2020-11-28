import org.openrndr.dokgen.annotations.Application
import org.openrndr.dokgen.annotations.Exclude


fun main(args: Array<String>) {
    @Application
    run {
        println("hello world")
        @Exclude
        run {
            println("this should still be included in the exported application source")
        }
    }
}
