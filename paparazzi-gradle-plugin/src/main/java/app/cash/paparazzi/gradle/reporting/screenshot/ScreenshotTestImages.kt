package app.cash.paparazzi.gradle.reporting.screenshot

internal data class ScreenshotTestImages(
  val reference: ImagePathOrMessage,
  val actual: ImagePathOrMessage,
  val diff: ImagePathOrMessage
)
