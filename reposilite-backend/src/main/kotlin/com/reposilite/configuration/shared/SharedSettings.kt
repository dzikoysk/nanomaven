package com.reposilite.configuration.shared

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.victools.jsonschema.generator.SchemaGenerator
import panda.std.Result
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

interface SharedSettings : Serializable

interface SharedSettingsReference<S : SharedSettings> {

    val type: KClass<out S>
    val name: String
    val schema: JsonNode

    fun get(): S
    fun update(value: S): Result<S, out Exception>

}

internal class DefaultSharedSettingsReference<T : SharedSettings>(
    override val type: KClass<out T>,
    schemaGenerator: SchemaGenerator,
    private val getter: () -> T,
    private val setter: (T) -> Unit
) : SharedSettingsReference<T> {

    override val name = type.findAnnotation<Doc>()!!.title.sanitizeURLParam()
    override val schema: ObjectNode = schemaGenerator.generateSchema(type.java).also { cleanupScheme(it) }

    override fun get(): T =
        getter()

    override fun update(value: T): Result<T, out Exception> =
        Result.attempt { setter(value) }
            .mapErr { IllegalStateException("Cannot update settings reference $name", it) }
            .map { get() }

    private fun String.sanitizeURLParam(): String =
        lowercase().replace(' ', '_')

}
