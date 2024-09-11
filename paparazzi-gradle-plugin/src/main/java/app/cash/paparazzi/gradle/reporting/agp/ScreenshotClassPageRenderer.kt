package app.cash.paparazzi.gradle.reporting.agp

import java.io.IOException

internal class ScreenshotClassPageRenderer(reportType: ReportType, isRecordGolden: Boolean): ClassPageRenderer(reportType) {
  private val imagePanelRenderer = ImagePanelRenderer(isRecordGolden)

  override fun renderFailures(htmlWriter: SimpleHtmlWriter) {
    for (test in results.failures) {
      val testName = test.getName()
      htmlWriter.startElement("div")
        .attribute("class", "test")
        .startElement("a")
        .attribute("name", test.id.toString())
        .characters("")
        .endElement() // browsers don't understand <a name="..."/>
        .startElement("h3")
        .attribute("class", test.statusClass)
        .characters(testName)
        .endElement()
      imagePanelRenderer.render(test.screenshotshotImages, htmlWriter)
      htmlWriter.endElement()
    }
  }

  fun renderTests(htmlWriter: SimpleHtmlWriter) {
    // show test images even for a successful test so that users can view what images were saved or see diff if it was below threshold
    for (test in results.results) {
      htmlWriter!!.startElement("div")
        .attribute("class", "test")
        .startElement("a")
        .attribute("name", test.id.toString())
        .characters("")
        .endElement() // browsers don't understand <a name="..."/>
        .startElement("h3")
        .attribute("class", test.statusClass)
        .characters(test.name)
        .endElement()

      imagePanelRenderer.render(test.screenshotshotImages, htmlWriter)
      htmlWriter.endElement()
    }
  }

  override fun registerTabs() {
    addFailuresTab()
    addTab(
      "Tests",
      object : ErroringAction<SimpleHtmlWriter>() {
        @Throws(IOException::class)
        override fun doExecute(writer: SimpleHtmlWriter) {
          renderTests(writer)
        }
      }
    )
    addDeviceAndVariantTabs()
  }
}
