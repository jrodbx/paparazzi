package app.cash.paparazzi.preview.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.buildCodeBlock

internal class PaparazziPoet(private val logger: KSPLogger) {
  fun buildFiles(functions: Sequence<KSFunctionDeclaration>): List<FileSpec> = listOf(
    buildAnnotationsFile(
      fileName = "PaparazziPreviews",
      propertyName = "paparazziPreviews",
      functions = functions,
    )
  )

  private fun buildAnnotationsFile(
    fileName: String,
    propertyName: String,
    functions: Sequence<KSFunctionDeclaration>
  ): FileSpec =
    FileSpec.scriptBuilder(fileName)
      .addCode(
        buildCodeBlock {
          addStatement("internal val %L = listOf<%L.PaparazziPreviewData>(", propertyName, PACKAGE_NAME)
          indent()

          if (functions.count() == 0) {
            addEmpty()
          } else {
            functions.forEach { function ->
              when {
                function.getVisibility() == Visibility.PRIVATE -> {
                  addError(
                    function = function,
                    buildErrorMessage = { "$it is private. Make it internal or public to generate a snapshot." }
                  )
                }

                function.hasPreviewParameter() -> {
                  addError(
                    function = function,
                    buildErrorMessage = { "$it preview uses PreviewParameters which aren't currently supported." }
                  )
                }

                else -> {
                  addDefault(
                    function = function,
                  )
                }
              }
            }
          }

          unindent()
          add(")")
        }
      )
      .build()

  private fun CodeBlock.Builder.addEmpty() {
    addStatement("%L.PaparazziPreviewData.Empty,", PACKAGE_NAME)
  }

  private fun CodeBlock.Builder.addDefault(
    function: KSFunctionDeclaration,
  ) {
    addStatement("%L.PaparazziPreviewData.Default(", PACKAGE_NAME)
    indent()
    addStatement("composable = { %L() },", function.qualifiedName?.asString())
    unindent()
    addStatement("),")
  }

  private fun CodeBlock.Builder.addError(
    function: KSFunctionDeclaration,
    buildErrorMessage: (String?) -> String
  ) {
    addStatement("%L.PaparazziPreviewData.Error(", PACKAGE_NAME)
    indent()
    addStatement("message = %S,", buildErrorMessage(function.qualifiedName?.asString()))
    unindent()
    addStatement("),")
  }

  private fun KSFunctionDeclaration.hasPreviewParameter(): Boolean {
    val previewParam = parameters.firstOrNull { param -> param.annotations.any { it.isPreviewParameter() } }
    logger.info("PreviewProcessor - previewParam: " + previewParam?.toString())
    return previewParam != null
  }
}

private const val PACKAGE_NAME = "app.cash.paparazzi.annotations"
