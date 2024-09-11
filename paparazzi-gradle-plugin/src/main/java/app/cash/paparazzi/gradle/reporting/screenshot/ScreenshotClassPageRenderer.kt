package app.cash.paparazzi.gradle.reporting.screenshot

import java.io.IOException

internal class ScreenshotClassPageRenderer : PageRenderer<ClassTestResults>() {
  private val imagePanelRenderer = ImagePanelRenderer()

  override fun renderBreadcrumbs(htmlWriter: SimpleHtmlWriter) {
    htmlWriter.startElement("div")
      .attribute("class", "breadcrumbs")
      .startElement("a")
      .attribute("href", "index.html")
      .characters("all")
      .endElement()
      .characters(" > ")
      .startElement("a")
      .attribute(
        "href",
        String.format("%s.html", results.getPackageResults().getFilename())
      )
      .characters(results.getPackageResults().name)
      .endElement()
      .characters(String.format(" > %s", results.simpleName))
      .endElement()
  }

  override fun renderFailures(htmlWriter: SimpleHtmlWriter) {
    for (test in results.failures) {
      val testName = test.name
      htmlWriter.startElement("div")
        .attribute("class", "test")
        .startElement("a")
        .attribute("name", test.id.toString())
        .characters("")
        .endElement() //browsers don't understand <a name="..."/>
        .startElement("h3")
        .attribute("class", test.statusClass)
        .characters(testName)
        .endElement()
      if (test.screenshotImages != null) {
        imagePanelRenderer.render(test.screenshotImages!!, htmlWriter)
      }
      htmlWriter.endElement()
    }
  }

  override fun renderErrors(htmlWriter: SimpleHtmlWriter) {
    for (test in results.errors) {
      val testName = test.name
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

      if (test.screenshotImages != null) {
        imagePanelRenderer.render(test.screenshotImages!!, htmlWriter)
      }
      htmlWriter.endElement()
    }
  }

  fun renderTests(htmlWriter: SimpleHtmlWriter?) {
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
      if (test.screenshotImages != null) {
        imagePanelRenderer.render(test.screenshotImages!!, htmlWriter)
      }
      htmlWriter.endElement()
    }
  }

  override fun registerTabs() {
    addErrorTab()
    addFailuresTab()
    addTab("Tests", object : ErroringAction<SimpleHtmlWriter>() {
      @Throws(IOException::class)
      override fun doExecute(objectToExecute: SimpleHtmlWriter) {
        renderTests(objectToExecute)
      }
    })
    //TODO: add variant tabs
  }
}
