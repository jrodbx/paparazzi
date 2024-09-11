package app.cash.paparazzi.gradle.reporting.geoff

internal data class ScreenshotDiffImage(
  val path: String,
  val snapshotName: String,
  val base64EncodedImage: String
)
