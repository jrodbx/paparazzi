package app.cash.paparazzi.gradle.reporting

import org.gradle.api.internal.tasks.testing.junit.result.TestResultsProvider
import org.gradle.api.internal.tasks.testing.report.AllTestResults
import org.gradle.api.internal.tasks.testing.report.DefaultTestReport
import org.gradle.api.internal.tasks.testing.report.TestReporter
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.time.Time
import java.io.File

public class PaparazziTestReporter : TestReporter {


  override fun generateReport(testResultsProvider: TestResultsProvider, reportDir: File) {
    LOG.info("Generating HTML test report...")
    val clock = Time.startTimer()
//    val model: AllTestResults = loadModelFromProvider(resultsProvider)
//    generateFiles(model, testResultsProvider, reportDir)
    LOG.info(
      "Finished generating test html results ({}) into: {}",
      clock.elapsed,
      reportDir
    )

    TODO("Not yet implemented")
  }

  private companion object {
    private val LOG: Logger = Logging.getLogger(DefaultTestReport::class.java)
  }
}
