@file:Suppress("UNUSED_EXPRESSION")

import org.openrndr.dokgen.annotations.Application
import org.openrndr.dokgen.annotations.Media


fun main(args: Array<String>) {
    @Application
    run {
        println("hello world")

        @Media.Image
        """
            image.png
        """

        @Media.Video
        """
            video.png
        """
    }
}