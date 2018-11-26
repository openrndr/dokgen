@file:Suppress("UNUSED_EXPRESSION")

import org.openrndr.docgen.annotations.Application
import org.openrndr.docgen.annotations.Media
import org.openrndr.docgen.annotations.Text


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