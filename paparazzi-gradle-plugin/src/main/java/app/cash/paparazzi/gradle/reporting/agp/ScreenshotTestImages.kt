package app.cash.paparazzi.gradle.reporting.agp

internal data class ScreenshotTestImages(val golden: Image, val actual: Image?, val diff: Image?) {
}

/**
 * In case of empty image path, the message will contain the text to be displayed instead
 */
internal data class Image(val path: String, val message: String)
