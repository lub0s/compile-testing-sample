package com.codegen.sample


import com.squareup.kotlinpoet.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

internal fun TypeName.rawType(): ClassName {
  return when (this) {
    is ClassName -> this
    is ParameterizedTypeName -> rawType
    else -> throw IllegalArgumentException("Cannot get raw type from $this")
  }
}

val Element.kotlinMetedata get() = getAnnotation(Metadata::class.java)
val Element.packageName get() = asType().asTypeName().rawType().packageName
val Element.simpleString get() = simpleName.toString()

fun TypeSpec.resolveParameters() = primaryConstructor?.parameters?.map { paramSpec ->
  Parameter(paramSpec.name, paramSpec.type)
}

data class Parameter(
  val name: String,
  val type: TypeName
)
