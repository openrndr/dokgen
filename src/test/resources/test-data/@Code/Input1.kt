import org.openrndr.docgen.annotations.Code


fun main(args: Array<String>) {
    @Code("This is some code")
    fun foo(): String {
        return "bar"
    }
}
