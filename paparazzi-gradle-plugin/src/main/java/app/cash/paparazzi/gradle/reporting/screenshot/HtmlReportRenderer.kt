package app.cash.paparazzi.gradle.reporting.screenshot

import org.gradle.internal.impldep.com.google.common.io.ByteStreams
import org.gradle.reporting.ReportRenderer
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer
import java.net.URL

internal class HtmlReportRenderer {

  private val resources: MutableSet<URL> = HashSet()

  fun requireResource(resource: URL) {
    resources.add(resource)
  }

  fun <T> renderer(renderer: ReportRenderer<T, SimpleHtmlWriter>): TextReportRenderer<T> {
    return renderer(TextReportRendererImpl(renderer))
  }

  private fun <T> renderer(renderer: TextReportRendererImpl<T>): TextReportRenderer<T> {
    return object : TextReportRenderer<T>() {
      @Throws(Exception::class)
      override fun writeTo(model: T, out: Writer) {
        renderer.writeTo(model, out)
      }

      override fun writeTo(model: T, file: File) {
        super.writeTo(model, file)
        for (resource in resources) {
          val name: String = substringAfterLast(resource.path, "/")
          val type: String = substringAfterLast(resource.path, ".")
          val destFile = File(file.getParentFile(), String.format("%s/%s", type, name))
          if (!destFile.exists()) {
            destFile.getParentFile().mkdirs()
            try {
              val urlConnection = resource.openConnection()
              urlConnection.setUseCaches(false)
              var inputStream: InputStream? = null
              try {
                inputStream = urlConnection.getInputStream()
                var outputStream: OutputStream? = null
                try {
                  outputStream = BufferedOutputStream(
                    FileOutputStream(destFile)
                  )
                  if (inputStream != null) {
                    ByteStreams.copy(inputStream, outputStream)
                  }
                } finally {
                  outputStream?.close()
                }
              } finally {
                inputStream?.close()
              }
            } catch (e: IOException) {
              throw RuntimeException(e)
            }
          }
        }
      }
    }
  }

  class TextReportRendererImpl<T>(private val delegate: ReportRenderer<T, SimpleHtmlWriter>) : TextReportRenderer<T>() {

    @Throws(Exception::class)
    public override fun writeTo(model: T, out: Writer) {
      val htmlWriter = SimpleHtmlWriter(out, "")
      htmlWriter.startElement("html")
      delegate.render(model, htmlWriter)
      htmlWriter.endElement()
    }
  }

  companion object {

    /**
     * Returns the substring of a string that follows the last
     * occurrence of a separator.
     *
     * Largely replicated and slightly updated from the
     * `apache.commons.lang.StringUtils` method of the same name.
     *
     * @param string    the String to get a substring from, may not be null
     * @param separator the String to search for, may not be null
     * @return the substring after the last occurrence of the separator or an
     * empty string if not found.
     */
    fun substringAfterLast(string: String, separator: String): String {
      val pos = string.lastIndexOf(separator)
      return if (pos == -1 || pos == string.length - separator.length) {
        ""
      } else string.substring(pos + separator.length)
    }
  }
}
