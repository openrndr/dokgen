package org.openrndr.dokgen.annotations


@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Application

@Target(
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.SOURCE)
annotation class Code(val text: String) {
    @Target(AnnotationTarget.EXPRESSION)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Block(val text: String)
}

@Target(
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE
)
@Retention(AnnotationRetention.SOURCE)
annotation class Text


class Media {
    @Target(
        AnnotationTarget.EXPRESSION,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.FIELD,
        AnnotationTarget.LOCAL_VARIABLE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Video

    @Target(
        AnnotationTarget.EXPRESSION,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.FIELD,
        AnnotationTarget.LOCAL_VARIABLE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Image
}


@Target(
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.SOURCE)
annotation class Exclude

