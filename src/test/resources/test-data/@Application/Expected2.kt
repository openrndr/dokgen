package test

fun main(args: Array<String>) {
    run {
        println("hello world")
        run {
            println("this should still be included in the exported application source")
        }
    }
}
