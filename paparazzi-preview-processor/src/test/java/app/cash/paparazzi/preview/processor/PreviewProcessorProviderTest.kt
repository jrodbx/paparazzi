@file:OptIn(ExperimentalCompilerApi::class)

package app.cash.paparazzi.preview.processor

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspAllWarningsAsErrors
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PreviewProcessorProviderTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val previewProcessor = PreviewProcessorProvider()
  private val codegenRoot: File
    get() = temporaryFolder.root.resolve("debug/ksp/sources/kotlin")

  @Test
  fun empty() {
    val compilation = prepareCompilation(
      SourceFile.kotlin(
        name = "SamplePreview.kt",
        contents = """
          package test

          import androidx.compose.runtime.Composable
          import androidx.compose.ui.tooling.preview.Preview
          import app.cash.paparazzi.annotations.Paparazzi

          @Preview
          @Composable
          fun SamplePreview() = Unit
          """.trimIndent()
      )
    )
    val result = compilation.compile()

    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

    // Assertion for variant has "sources" as we can't specify the variant name in testing KSP
//    assertThat(codegenRoot.resolve("paparazziVariant.kt").readText())
//      .isEqualTo("sources")

    assertThat(codegenRoot.resolve("PaparazziPreviews.kt").readText())
      .isEqualTo(
        """
        internal val paparazziPreviews = listOf<app.cash.paparazzi.annotations.PaparazziPreviewData>(
          app.cash.paparazzi.annotations.PaparazziPreviewData.Empty,
        )
        """.trimIndent()
      )
  }

  @Test
  fun default() {
    val compilation = prepareCompilation(
      SourceFile.kotlin(
        "SamplePreview.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.tooling.preview.Preview
        import app.cash.paparazzi.annotations.Paparazzi

        @Paparazzi
        @Preview
        @Composable
        fun SamplePreview() = Unit
        """.trimIndent()
      )
    )
    val result = compilation.compile()

    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

//    // Assertion for variant has "sources" as we can't specify the variant name in testing KSP
//    assertThat(codegenRoot.resolve("paparazziVariant.kt").readText())
//      .isEqualTo("sources")

    assertThat(codegenRoot.resolve("PaparazziPreviews.kt").readText())
      .isEqualTo(
        """
        internal val paparazziPreviews = listOf<app.cash.paparazzi.annotations.PaparazziPreviewData>(
          app.cash.paparazzi.annotations.PaparazziPreviewData.Default(
            composable = { test.SamplePreview() },
          ),
        )
        """.trimIndent()
      )
  }


  @Test
  fun privatePreview() {
    val compilation = prepareCompilation(
      SourceFile.kotlin(
        "SamplePreview.kt",
        """
         package test

         import androidx.compose.runtime.Composable
         import androidx.compose.ui.tooling.preview.Preview
         import app.cash.paparazzi.annotations.Paparazzi

         @Paparazzi
         @Preview
         @Composable
         private fun SamplePreview() = Unit
         """.trimIndent()
      )
    )
    val result = compilation.compile()

    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(codegenRoot.resolve("PaparazziPreviews.kt").readText())
      .isEqualTo(
        """
          internal val paparazziPreviews = listOf<app.cash.paparazzi.annotations.PaparazziPreviewData>(
            app.cash.paparazzi.annotations.PaparazziPreviewData.Error(
              message = "test.SamplePreview is private. Make it internal or public to generate a snapshot.",
            ),
          )
        """.trimIndent()
      )
  }

  @Test
  fun previewParameters() {
    val compilation = prepareCompilation(
      SourceFile.kotlin(
        "SamplePreview.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.tooling.preview.Preview
        import androidx.compose.ui.tooling.preview.PreviewParameter
        import androidx.compose.ui.tooling.preview.PreviewParameterProvider
        import app.cash.paparazzi.annotations.Paparazzi

        @Paparazzi
        @Preview
        @Composable
        fun SamplePreview(
          @PreviewParameter(SamplePreviewParameter::class) text: String,
        ) = Unit

        object SamplePreviewParameter: PreviewParameterProvider<String> {
          override val values: Sequence<String> = sequenceOf("test")
        }
        """
      )
    )
    val result = compilation.compile()

    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

    assertThat(codegenRoot.resolve("PaparazziPreviews.kt").readText())
      .isEqualTo(
        """
          internal val paparazziPreviews = listOf<app.cash.paparazzi.annotations.PaparazziPreviewData>(
            app.cash.paparazzi.annotations.PaparazziPreviewData.Error(
              message = "test.SamplePreview preview uses PreviewParameters which aren't currently supported.",
            ),
          )
        """.trimIndent()
      )
  }

  @Test
  fun multiplePreviews() {
    val compilation = prepareCompilation(
      SourceFile.kotlin(
        "SamplePreview.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import androidx.compose.ui.tooling.preview.Preview
        import app.cash.paparazzi.annotations.Paparazzi

        @Paparazzi
        @Preview
        @Preview(
           name = "Night Pixel 4",
           uiMode = 0x20, // uiMode maps to android.content.res.Configuration.UI_MODE_NIGHT_YES
           device = "id:pixel_4"
        )
        @Composable
        fun SamplePreview() = Unit
        """.trimIndent()
      )
    )
    val result = compilation.compile()

    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

    assertThat(codegenRoot.resolve("PaparazziPreviews.kt").readText())
      .isEqualTo(
        """
          internal val paparazziPreviews = listOf<app.cash.paparazzi.annotations.PaparazziPreviewData>(
            app.cash.paparazzi.annotations.PaparazziPreviewData.Default(
              composable = { test.SamplePreview() },
            ),
            app.cash.paparazzi.annotations.PaparazziPreviewData.Default(
              composable = { test.SamplePreview() },
            ),
          )
        """.trimIndent()
      )
  }

  private fun prepareCompilation(vararg sourceFiles: SourceFile): KotlinCompilation =
    KotlinCompilation()
      .apply {
        workingDir = File(temporaryFolder.root, "debug")
        inheritClassPath = true
        sources = sourceFiles.asList() + composeSources + paparazziAnnotation
        verbose = false

        kspAllWarningsAsErrors = true
        kspArgs["app.cash.paparazzi.preview.namespace"] = "test"
        kspIncremental = true
        symbolProcessorProviders = listOf(previewProcessor)
      }

  val composeSources =
    listOf(
      SourceFile.kotlin(
        "Composable.kt",
        """
          package androidx.compose.runtime

          @Retention(AnnotationRetention.BINARY)
          @Target(
              AnnotationTarget.FUNCTION,
              AnnotationTarget.TYPE,
              AnnotationTarget.TYPE_PARAMETER,
              AnnotationTarget.PROPERTY_GETTER
          )
          annotation class Composable
        """
      ),
      SourceFile.kotlin(
        "PreviewAnnotation.kt",
        """
          package androidx.compose.ui.tooling.preview

          @Retention(AnnotationRetention.BINARY)
          @Target(
              AnnotationTarget.ANNOTATION_CLASS,
              AnnotationTarget.FUNCTION
          )
          @Repeatable
          annotation class Preview(
            val name: String = "",
            val group: String = "",
            val apiLevel: Int = -1,
            val widthDp: Int = -1,
            val heightDp: Int = -1,
            val locale: String = "",
            val fontScale: Float = 1f,
            val showSystemUi: Boolean = false,
            val showBackground: Boolean = false,
            val backgroundColor: Long = 0,
            val uiMode: Int = 0,
            val device: String = "",
            val wallpaper: Int = 0,
          )
        """
      ),
      SourceFile.kotlin(
        "PreviewParameter.kt",
        """
        package androidx.compose.ui.tooling.preview

        import kotlin.jvm.JvmDefaultWithCompatibility
        import kotlin.reflect.KClass

        @JvmDefaultWithCompatibility
        interface PreviewParameterProvider<T> {
            val values: Sequence<T>
            val count get() = values.count()
        }

        annotation class PreviewParameter(
            val provider: KClass<out PreviewParameterProvider<*>>,
            val limit: Int = Int.MAX_VALUE
        )
      """.trimIndent()
      )
    )

  val paparazziAnnotation =
    SourceFile.kotlin(
      "Paparazzi.kt",
      """
        package app.cash.paparazzi.annotations
        @Target(AnnotationTarget.FUNCTION)
        @Retention(AnnotationRetention.BINARY)
        annotation class Paparazzi
      """
    )
}
