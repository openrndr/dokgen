import org.openrndr.docgen.annotations.Code


fun main(args: Array<String>) {
    @Code.Block("This is some code")
    run {
        println("hello world")
    }
}
