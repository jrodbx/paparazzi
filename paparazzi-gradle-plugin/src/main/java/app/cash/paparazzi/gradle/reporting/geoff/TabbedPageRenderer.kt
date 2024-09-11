package app.cash.paparazzi.gradle.reporting.geoff

import org.gradle.reporting.ReportRenderer
import org.gradle.util.GradleVersion
import java.io.IOException
import java.text.DateFormat
import java.util.Date

internal abstract class TabbedPageRenderer<T> : ReportRenderer<T, SimpleHtmlWriter>() {
  private var model: T? = null

  protected fun getModel(): T? {
    return model
  }

  protected abstract fun getTitle(): String
  protected abstract val headerRenderer: ReportRenderer<T, SimpleHtmlWriter>
  protected abstract val contentRenderer: ReportRenderer<T, SimpleHtmlWriter>
  private fun getPageTitle(): String {
    return "Test results - " + getTitle()
  }

  @Throws(IOException::class)
  override fun render(model: T, htmlWriter: SimpleHtmlWriter) {
    this.model = model
    htmlWriter.startElement("head")
      .startElement("meta")
      .attribute("http-equiv", "Content-Type")
      .attribute("content", "text/html; charset=utf-8")
      .endElement()
      .startElement("title")
      .characters(getPageTitle())
      .endElement()
      .startElement("link")
      .attribute("href", "css/base-style.css")
      .attribute("rel", "stylesheet")
      .attribute("type", "text/css")
      .endElement()
      .startElement("link")
      .attribute("href", "css/style.css")
      .attribute("rel", "stylesheet")
      .attribute("type", "text/css")
      .endElement()
      .startElement("script")
      .attribute("src", "js/report.js")
      .attribute("type", "text/javascript")
      .characters("")
      .endElement() // html does not like <a name="..."/>
      .endElement()
    htmlWriter.startElement("body")
      .startElement("div").attribute("id", "content")
      .startElement("h1").characters(getTitle()).endElement()
    headerRenderer.render(model, htmlWriter)
    contentRenderer.render(model, htmlWriter)
    htmlWriter.startElement("div")
      .attribute("id", "footer")
      .startElement("p")
      .characters("Generated by ")
      .startElement("a")
      .attribute("href", "http://www.gradle.org")
      .characters(String.format("Gradle %s", GradleVersion.current().version))
      .endElement()
      .characters(String.format(" at %s", DateFormat.getDateTimeInstance().format(Date())))
      .endElement()
      .endElement()
      .endElement()
  }
}
