package com.codegen.sample

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic


@AutoService(Processor::class)
@KotlinPoetMetadataPreview
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class SummableProcessor : AbstractProcessor() {

  private lateinit var messager: Messager
  private lateinit var elementUtils: Elements
  private lateinit var typeUtils: Types
  private lateinit var filer: Filer
  private lateinit var classInspector: ClassInspector

  private var processingEnvironment: ProcessingEnvironment? = null
  private val annotationClass = IntSummable::class.java

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)
    this.processingEnvironment = processingEnv
    this.typeUtils = processingEnv.typeUtils
    this.elementUtils = processingEnv.elementUtils
    this.filer = processingEnv.filer
    this.messager = processingEnv.messager
    this.classInspector = ElementsClassInspector.create(elementUtils, typeUtils)
  }

  override fun getSupportedAnnotationTypes() = mutableSetOf(annotationClass.name)

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    if (roundEnv.errorRaised()) {
      return false
    }

    roundEnvLoop@ for (summableDataClass in roundEnv.getElementsAnnotatedWith(annotationClass)) {
      val typeMetadata = summableDataClass.kotlinMetedata
      if (typeMetadata == null) {
        messager.printMessage(
          Diagnostic.Kind.ERROR,
          "@Summable can't be applied to ${summableDataClass.simpleString}: must be a Kotlin class",
          summableDataClass
        )

        continue@roundEnvLoop
      }

      val kmClass = try {
        typeMetadata.toImmutableKmClass()
      } catch (e: UnsupportedOperationException) {
        messager.printMessage(
          Diagnostic.Kind.ERROR,
          "@Summable can't be applied to ${summableDataClass.simpleString}: must be a Class type",
          summableDataClass
        )

        continue@roundEnvLoop
      }

      if (!kmClass.isData) {
        messager.printMessage(
          Diagnostic.Kind.ERROR,
          "@Summable can't be applied to ${summableDataClass.simpleString}: must be a data class",
          summableDataClass
        )

        continue@roundEnvLoop
      }

      val typeSpec = kmClass.toTypeSpec(classInspector)
      val summableParams = typeSpec.resolveParameters()

      val fileSpec = FileSpec.builder(summableDataClass.packageName, summableDataClass.simpleString)

      fileSpec.addFunction(
        FunSpec.builder("sumInts")
          .receiver(summableDataClass.asType().asTypeName())
          .returns(Int::class)
          .addStatement("val sum = ${summableParams?.joinToString(" + ", transform = Parameter::name)}")
          .addStatement("return sum")
          .build()
      )

      fileSpec
        .build()
        .writeTo(filer)
    }

    return true
  }

}
