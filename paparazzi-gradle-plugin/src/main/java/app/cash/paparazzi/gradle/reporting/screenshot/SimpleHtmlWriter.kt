package app.cash.paparazzi.gradle.reporting.screenshot

import java.io.IOException
import java.io.Writer

/**
 *
 * A streaming HTML writer.
 */
internal class SimpleHtmlWriter(
  writer: Writer,
  indent: String? = null
) : SimpleMarkupWriter(writer, indent) {

  init {
    writeHtmlHeader()
  }

  @Throws(IOException::class)
  private fun writeHtmlHeader() {
    writeRaw("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">")
  }
}
