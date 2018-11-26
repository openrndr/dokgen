import org.openrndr.docgen.annotations.Application
import java.io.File


fun main(args: Array<String>) {
    @Application
    run {
        File("hello world")
    }
}