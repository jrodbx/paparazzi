package app.cash.paparazzi.gradle.reporting.screenshot

import java.math.BigDecimal
import java.math.RoundingMode

internal class DurationFormatter {

  fun format(duration: Long): String {
    if (duration == 0L) {
      return "0s"
    }
    var formattedDuration = duration
    val result = StringBuilder()
    val days: Long =
      formattedDuration / MILLIS_PER_DAY
    formattedDuration %= MILLIS_PER_DAY
    if (days > 0) {
      result.append(days)
      result.append("d")
    }
    val hours: Long =
      duration / MILLIS_PER_HOUR
    formattedDuration %= MILLIS_PER_HOUR
    if (hours > 0 || result.isNotEmpty()) {
      result.append(hours)
      result.append("h")
    }
    val minutes: Long =
      formattedDuration / MILLIS_PER_MINUTE
    formattedDuration %= MILLIS_PER_MINUTE
    if (minutes > 0 || result.isNotEmpty()) {
      result.append(minutes)
      result.append("m")
    }
    val secondsScale = if (result.isNotEmpty()) 2 else 3
    result.append(
      BigDecimal.valueOf(formattedDuration)
        .divide(MILLIS_PER_SECOND.toBigDecimal())
        .setScale(secondsScale, RoundingMode.HALF_UP)
    )
    result.append("s")
    return result.toString()
  }

  companion object {
    const val MILLIS_PER_SECOND = 1000
    val MILLIS_PER_MINUTE: Int =
      60 * MILLIS_PER_SECOND
    val MILLIS_PER_HOUR: Int =
      60 * MILLIS_PER_MINUTE
    val MILLIS_PER_DAY: Int =
      24 * MILLIS_PER_HOUR
  }
}
