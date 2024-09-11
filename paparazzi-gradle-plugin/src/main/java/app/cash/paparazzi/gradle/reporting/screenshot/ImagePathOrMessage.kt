package app.cash.paparazzi.gradle.reporting.screenshot

/**
 * In case of empty image path, the message will contain the text to be displayed instead
 */
internal sealed class ImagePathOrMessage {
  data class ImagePath(val path: String) : ImagePathOrMessage()
  data class ErrorMessage(val message: String) : ImagePathOrMessage()
}
