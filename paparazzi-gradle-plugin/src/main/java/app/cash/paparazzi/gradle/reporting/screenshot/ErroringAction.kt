package app.cash.paparazzi.gradle.reporting.screenshot

import org.gradle.api.Action


/**
 * Action adapter/implementation for action code that may throw exceptions.
 *
 * Implementations implement doExecute() (instead of execute()) which is allowed to throw checked
 * exceptions.
 * Any checked exceptions thrown will be wrapped as unchecked exceptions and re-thrown.
 *
 * @param <T> The type of object which this action accepts.
</T> */
internal abstract class ErroringAction<T> : Action<T> {

  override fun execute(objectToExecute: T) {
    try {
      doExecute(objectToExecute)
    } catch (e: Exception) {
      if (e is RuntimeException) {
        throw e
      }
      throw RuntimeException(e)
    }
  }

  @Throws(Exception::class)
  protected abstract fun doExecute(objectToExecute: T)
}
