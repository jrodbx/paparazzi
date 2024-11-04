package app.cash.paparazzi.gradle.reporting.gradlek

import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.internal.tasks.testing.junit.result.TestResultsProvider
import org.gradle.api.internal.tasks.testing.report.TestReporter
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.TestResult
import org.gradle.internal.html.SimpleHtmlWriter
import org.gradle.internal.operations.BuildOperationContext
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.operations.BuildOperationRunner
import org.gradle.internal.operations.RunnableBuildOperation
import org.gradle.internal.time.Time
import org.gradle.reporting.HtmlReportBuilder
import org.gradle.reporting.HtmlReportRenderer
import org.gradle.reporting.ReportRenderer
import org.gradle.util.internal.GFileUtils
import sun.misc.Unsafe
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.security.AccessController
import java.security.PrivilegedAction

internal class DefaultTestReport(
  private val buildOperationRunner: BuildOperationRunner,
  private val buildOperationExecutor: BuildOperationExecutor,
  private val isVerifyRun: Property<Boolean>,
  private val failureSnapshotDir: Provider<Directory>,
) : TestReporter {
  init {
    val declaredField = SimpleHtmlWriter::class.java.getFieldReflectively("VALID_HTML_TAGS")
    declaredField.setStaticValue(declaredField.get(null) as Set<String> + "img")
  }

  override fun generateReport(testResultsProvider: TestResultsProvider, reportDir: File) {
    if (isVerifyRun.get() && failureSnapshotDir.get().asFile.exists()) {
      println("failure dir exists")
    } else {
      println("failure dir does not exist")
    }

    LOG.info("Generating HTML test report...")

    val clock = Time.startTimer()
    val model = loadModelFromProvider(testResultsProvider)
    generateFiles(model, testResultsProvider, reportDir)
    LOG.info("Finished generating test html results ({}) into: {}", clock.elapsed, reportDir)
  }

  private fun loadModelFromProvider(resultsProvider: TestResultsProvider): AllTestResults {
    val model = AllTestResults()
    resultsProvider.visitClasses { classResult ->
      model.addTestClass(classResult.id, classResult.className, classResult.classDisplayName)
      val collectedResults = classResult.results
      for (collectedResult in collectedResults) {
        val testResult = model.addTest(
          classResult.id,
          classResult.className,
          classResult.classDisplayName,
          collectedResult.name,
          collectedResult.displayName,
          collectedResult.duration
        )
        if (collectedResult.resultType == TestResult.ResultType.SKIPPED) {
          testResult.setIgnored()
        } else {
          val failures = collectedResult.failures
          for (failure in failures) {
            testResult.addFailure(failure)
          }
        }
      }
    }
    return model
  }

  private fun generateFiles(
    model: AllTestResults,
    resultsProvider: TestResultsProvider,
    reportDir: File
  ) {
    try {
      val htmlRenderer = HtmlReportRenderer()
      buildOperationRunner.run(object : RunnableBuildOperation {
        override fun run(context: BuildOperationContext) {
          // Clean-up old HTML report directories
          GFileUtils.deleteQuietly(File(reportDir, "packages"))
          GFileUtils.deleteQuietly(File(reportDir, "classes"))
        }

        override fun description(): BuildOperationDescriptor.Builder {
          return BuildOperationDescriptor.displayName("Delete old HTML results")
        }
      })

      htmlRenderer.render(
        model,
        object : ReportRenderer<AllTestResults, HtmlReportBuilder>() {
          @Throws(IOException::class)
          override fun render(model: AllTestResults, output: HtmlReportBuilder) {
            buildOperationExecutor.runAll { queue ->
              queue.add(
                generator("index.html", model, OverviewPageRenderer(), output)
              )
              for (packageResults in model.getPackages()) {
                queue.add(
                  generator(packageResults.baseUrl, packageResults, PackagePageRenderer(), output)
                )
                for (classResults in packageResults.getClasses()) {
                  queue.add(
                    generator(
                      classResults.baseUrl,
                      classResults,
                      ClassPageRenderer(
                        resultsProvider,
                        isVerifyRun.get(),
                        failureSnapshotDir.get().asFile
                      ),
                      output
                    )
                  )
                }
              }
            }
          }
        },
        reportDir
      )
    } catch (e: Exception) {
      throw GradleException(String.format("Could not generate test report to '%s'.", reportDir), e)
    }
  }

  class HtmlReportFileGenerator<T : CompositeTestResults> internal constructor(
    private val fileUrl: String,
    private val results: T,
    private val renderer: PageRenderer<T>,
    private val output: HtmlReportBuilder
  ) : RunnableBuildOperation {
    override fun description(): BuildOperationDescriptor.Builder =
      BuildOperationDescriptor.displayName("Generate HTML test report for " + results.title)

    override fun run(context: BuildOperationContext) {
      output.renderHtmlPage(fileUrl, results, renderer)
    }
  }

  companion object {
    private val LOG: Logger = Logging.getLogger(DefaultTestReport::class.java)

    fun <T : CompositeTestResults> generator(
      fileUrl: String,
      results: T,
      renderer: PageRenderer<T>,
      output: HtmlReportBuilder
    ): HtmlReportFileGenerator<T> = HtmlReportFileGenerator(fileUrl, results, renderer, output)
  }


}

internal fun Class<*>.getFieldReflectively(fieldName: String): Field =
  try {
    this.getDeclaredField(fieldName).also { it.isAccessible = true }
  } catch (e: NoSuchFieldException) {
    throw RuntimeException("Field '$fieldName' was not found in class $name.")
  }

internal fun Field.setStaticValue(value: Any) {
  try {
    this.isAccessible = true
    val isFinalModifierPresent = this.modifiers and Modifier.FINAL == Modifier.FINAL
    if (isFinalModifierPresent) {
      AccessController.doPrivileged<Any?>(
        PrivilegedAction {
          try {
            val unsafe = Unsafe::class.java.getFieldReflectively("theUnsafe").get(null) as Unsafe
            val offset = unsafe.staticFieldOffset(this)
            val base = unsafe.staticFieldBase(this)
            unsafe.setFieldValue(this, base, offset, value)
            null
          } catch (t: Throwable) {
            throw RuntimeException(t)
          }
        }
      )
    } else {
      this.set(null, value)
    }
  } catch (ex: SecurityException) {
    throw RuntimeException(ex)
  } catch (ex: IllegalAccessException) {
    throw RuntimeException(ex)
  } catch (ex: IllegalArgumentException) {
    throw RuntimeException(ex)
  }
}

internal fun Unsafe.setFieldValue(field: Field, base: Any, offset: Long, value: Any) =
  when (field.type) {
    Integer.TYPE -> this.putInt(base, offset, (value as Int))
    java.lang.Short.TYPE -> this.putShort(base, offset, (value as Short))
    java.lang.Long.TYPE -> this.putLong(base, offset, (value as Long))
    java.lang.Byte.TYPE -> this.putByte(base, offset, (value as Byte))
    java.lang.Boolean.TYPE -> this.putBoolean(base, offset, (value as Boolean))
    java.lang.Float.TYPE -> this.putFloat(base, offset, (value as Float))
    java.lang.Double.TYPE -> this.putDouble(base, offset, (value as Double))
    Character.TYPE -> this.putChar(base, offset, (value as Char))
    else -> this.putObject(base, offset, value)
  }
