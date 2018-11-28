import org.openrndr.dokgen.annotations.Code


fun main(args: Array<String>) {
    @Code("This is some code")
    fun foo(): String {
        return "bar"
    }
}
