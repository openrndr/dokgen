@file:Suppress("UNUSED_EXPRESSION")

import org.openrndr.dokgen.annotations.Media


fun main(args: Array<String>) {
    @Media.Image
    """
       image.png
    """

    @Media.Video
    """
       video.mp4
    """
}