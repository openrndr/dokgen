import org.openrndr.dokgen.annotations.Code


fun main(args: Array<String>) {
    @Code
    fun foo(): String {
        return "bar"
    }
}
