package app.cash.paparazzi.preview.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo

public class PreviewProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): PreviewProcessor = PreviewProcessor(environment)
}


public class PreviewProcessor(
  private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
  private var invoked = false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) {
      return emptyList()
    }
    invoked = true

    val allFiles = resolver.getNewFiles()
    if (!allFiles.iterator().hasNext()) return emptyList()

    return resolver.getSymbolsWithAnnotation("androidx.compose.runtime.Composable")
      .findPaparazzi()
      .also { functions ->
        environment.logger.info("PreviewProcessor - found ${functions.count()} function(s)")
        PaparazziPoet(environment.logger).buildFiles(functions).forEach { file ->
          environment.logger.info("PreviewProcessor - writing file: ${file.packageName}.${file.name}.kt")
          file.writeTo(environment.codeGenerator, true)
        }
      }
      .filterNot { it.validate() }
      .toList()
  }
}

internal fun KSAnnotation.declaration() = annotationType.resolve().declaration
internal fun KSAnnotation.qualifiedName() = declaration().qualifiedName?.asString() ?: ""

internal fun KSAnnotation.isPaparazzi() = qualifiedName() == "app.cash.paparazzi.annotations.Paparazzi"
internal fun KSAnnotation.isPreviewParameter() = qualifiedName() == "androidx.compose.ui.tooling.preview.PreviewParameter"

internal fun Sequence<KSAnnotated>.findPaparazzi() =
  filterIsInstance<KSFunctionDeclaration>()
    .filter { it.annotations.hasPaparazzi() && it.functionKind == FunctionKind.TOP_LEVEL }

internal fun Sequence<KSAnnotation>.hasPaparazzi() = filter { it.isPaparazzi() }.count() > 0
