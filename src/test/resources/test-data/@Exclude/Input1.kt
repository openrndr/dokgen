import org.openrndr.docgen.annotations.Code
import org.openrndr.docgen.annotations.Exclude


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
