package `test-data`.`@Application`

import org.openrndr.dokgen.annotations.Application
import org.openrndr.dokgen.annotations.Code


fun main(args: Array<String>) {
    @Application
    @Code
    run {
        println("hello")
    }
}