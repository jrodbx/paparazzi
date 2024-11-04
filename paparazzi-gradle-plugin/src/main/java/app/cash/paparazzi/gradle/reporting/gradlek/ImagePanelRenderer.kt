package app.cash.paparazzi.gradle.reporting.gradlek

import org.gradle.internal.html.SimpleHtmlWriter
import org.gradle.reporting.ReportRenderer

public class ImagePanelRenderer : ReportRenderer<DiffImage, SimpleHtmlWriter>() {
  override fun render(image: DiffImage, htmlWriter: SimpleHtmlWriter) {
    // Wrap in a <span>, to work around CSS problem in IE
    htmlWriter
      .startElement("span")
      .startElement("table")
      .attribute("style", "table-layout: fixed")

    // Render a grid background to better show the diff between images with and without background
    htmlWriter
      .startElement("tbody")
      .attribute("class", "grid")
      .attribute("style", "width: 100%")

    renderImage(image, htmlWriter)

    htmlWriter.endElement().endElement().endElement()
  }

  private fun renderImage(image: DiffImage, htmlWriter: SimpleHtmlWriter) {
    htmlWriter
      .startElement("tr")
      .startElement("td")
      .attribute("style", "width: 100%; padding: 1em")
      .startElement("h4")
      .characters(image.snapshotName)
      .endElement()
      .startElement("img")
      .attribute("src", "data:image/png;base64, ${image.base64EncodedImage}")
      .attribute("style", "max-width: 100%; height: auto;")
      .attribute("alt", image.text)
      .endElement()
      .endElement()
      .endElement()
  }
}

public class DiffImage(
  public val path: String,
  public val snapshotName: String,
  public val base64EncodedImage: String,
  public val testClass: String,
  public val testMethod: String,
) {
  public val text: String
    get() = String.format("Error displaying image at %s", path)
}
