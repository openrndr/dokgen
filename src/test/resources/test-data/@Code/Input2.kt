import org.openrndr.dokgen.annotations.Code


fun main(args: Array<String>) {
    @Code.Block("This is some code")
    run {
        println("hello world")
    }
}
