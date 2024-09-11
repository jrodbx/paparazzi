package app.cash.paparazzi.gradle.reporting.screenshot

/**
 * Custom test result based on Gradle's TestResult
 */
internal class TestResult(
  val name: String,
  override val duration: Long,
  val project: String,
  private val flavor: String,
  var screenshotImages: ScreenshotTestImages?,
  val classResults: ClassTestResults
) : TestResultModel(), Comparable<TestResult> {

  val failures: MutableList<TestFailure> = ArrayList()
  val errors: MutableList<TestError> = ArrayList()
  private var ignored = false

  val id: Any
    get() = name

  override val title = String.format("Test %s", name)

  override fun getResultType(): ResultType {
    if (ignored) {
      return ResultType.SKIPPED
    }
    return if (failures.isEmpty() && errors.isEmpty()) ResultType.SUCCESS else ResultType.FAILURE
  }

  override fun getFormattedDuration(): String {
    return if (ignored) "-" else super.getFormattedDuration()
  }

  fun addFailure(
    message: String, stackTrace: String, projectName: String, flavorName: String
  ) {
    classResults.failed(this, projectName, flavorName)
    failures.add(
      TestFailure(
        message,
        stackTrace,
        null
      )
    )
  }

  fun addError(
    message: String, projectName: String, flavorName: String
  ) {
    classResults.error(this, projectName, flavorName)
    errors.add(
      TestError(
        message
      )
    )
  }

  fun ignored(projectName: String, flavorName: String) {
    ignored = true
    classResults.skipped(projectName, flavorName)
  }

  override fun compareTo(other: TestResult): Int {
    var diff: Int = classResults.name.compareTo(other.classResults.name)
    if (diff != 0) {
      return diff
    }
    diff = name.compareTo(other.name)
    if (diff != 0) {
      return diff
    }
    diff = flavor.compareTo(other.flavor)
    if (diff != 0) {
      return diff
    }
    val thisIdentity = System.identityHashCode(this)
    val otherIdentity = System.identityHashCode(other)
    return thisIdentity.compareTo(otherIdentity)
  }

  internal data class TestFailure(
    val message: String,
    val stackTrace: String?,
    val exceptionType: String?
  )

  internal data class TestError(val message: String)
}
