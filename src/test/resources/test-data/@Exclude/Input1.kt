import org.openrndr.dokgen.annotations.Code
import org.openrndr.dokgen.annotations.Exclude


fun main(args: Array<String>) {

    @Code("here's some code")
    run {
        println("hello world")

        @Exclude
        run {
            println("this should not make it to the doc")
        }
    }
}
