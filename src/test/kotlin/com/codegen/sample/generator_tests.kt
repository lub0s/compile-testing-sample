package com.codegen.sample

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


@KotlinPoetMetadataPreview
class GeneratedTests {

  @Rule
  @JvmField
  var temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun `annotated class isn't a data class`() {
    val kotlinSource = SourceFile.kotlin(
      "file1.kt", """
        package com.tests.summable
        
        import com.codegen.sample.IntSummable

          @IntSummable
          class FooSummable(
            val bar: Int = 234,
            val baz: Int = 123
          )
    """
    )

    fun compileSource(source: SourceFile) = KotlinCompilation().apply {
      sources = listOf(source)
      annotationProcessors = listOf(SummableProcessor())
      workingDir = temporaryFolder.root
      inheritClassPath = true
      verbose = false
    }.compile()

    val compilationResult = compile(kotlinSource)

    Assertions.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    Assertions.assertThat(compilationResult.messages).contains("@Summable can't be applied to FooSummable: must be a data class")
  }

  @Test
  fun `validate file content for FooSummable`() {
    val kotlinSource = SourceFile.kotlin(
      "file2.kt", """
      package com.tests.summable
      
      import com.codegen.sample.IntSummable

          @IntSummable
          data class FooAlsoSummable(
            val bar: Int = 123,
            val baz: Int = 123
          )
    """
    )

    fun compileSource(source: SourceFile) = KotlinCompilation().apply {
      sources = listOf(source)
      annotationProcessors = listOf(SummableProcessor())
      workingDir = temporaryFolder.root
      inheritClassPath = true
      verbose = false
    }.compile()

    val compilationResult = compile(kotlinSource)

    Assertions.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    Assertions.assertThat(compilationResult.generatedFiles.find { it.name == "FooAlsoSummable.kt" }).hasContent(
      """
         |package com.tests.summable
         |
         |import kotlin.Int
         |
         |fun FooAlsoSummable.sumInts(): Int {
         |  val sum = bar + baz
         |  return sum
         |}
      """.trimMargin()
    )
  }

  private fun compile(source: SourceFile) = KotlinCompilation().apply {
    sources = listOf(source)
    annotationProcessors = listOf(SummableProcessor())
    workingDir = temporaryFolder.root
    inheritClassPath = true
    verbose = false
  }.compile()

}
