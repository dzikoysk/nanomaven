package com.reposilite.configuration.shared

import com.github.victools.jsonschema.generator.SchemaGenerator
import com.reposilite.ReposiliteObjectMapper.DEFAULT_OBJECT_MAPPER
import com.reposilite.journalist.Journalist
import com.reposilite.plugin.api.Facade
import com.reposilite.status.FailureFacade
import panda.std.Result
import panda.std.asError
import panda.std.ok
import panda.std.reactive.MutableReference
import kotlin.reflect.KClass

class SharedConfigurationFacade(
    private val journalist: Journalist,
    private val schemaGenerator: SchemaGenerator,
    private val failureFacade: FailureFacade,
    private val sharedSettingsProvider: SharedSettingsProvider,
    private val sharedConfigurationProvider: SharedConfigurationProvider
) : Facade {

    private val configHandlers = mutableMapOf<String, SharedSettingsReference<*>>()

    init {
        sharedSettingsProvider.domains.forEach { (type, settings) ->
            registerSettingsWatcher(
                DefaultSharedSettingsReference(
                    type = type,
                    schemaGenerator = schemaGenerator,
                    getter = { settings.get() },
                    setter = { settings.update(it) }
                )
            )
        }
    }

    private fun <T : SharedSettings> registerSettingsWatcher(handler: SharedSettingsReference<T>): SharedSettingsReference<T> =
        handler.also {
            if (it.name in configHandlers) throw IllegalArgumentException("There are already settings with that name! Please report to the plugin author.")
            configHandlers[it.name] = it
        }

    class SharedSettingsUpdateException(
        val errors: Collection<Pair<SharedSettingsReference<*>, Exception>>
    ) : IllegalStateException("Cannot load shared configuration from file (${errors.size} errors):\n${errors.joinToString(System.lineSeparator())}")

    internal fun loadSharedSettingsFromString(content: String): Result<Unit, SharedSettingsUpdateException> {
        val updateResult = Result.attempt { DEFAULT_OBJECT_MAPPER.readTree(content) }
            .map { node -> names().filter { node.has(it) }.associateWith { node.get(it) } }
            .orElseGet { emptyMap() }
            .mapKeys { (name) -> getSettingsReference<SharedSettings>(name)!! }
            .mapValues { (ref, obj) -> DEFAULT_OBJECT_MAPPER.readValue(obj.toString(), ref.type.java) }
            .map { (ref, settings) -> ref to ref.update(settings) }

        updateResult
            .filter { (_, result) -> result.isOk }
            .joinToString(separator = ", ") { (ref) -> "'${ref.name}'" }
            .let { journalist.logger.info("Domains $it have been loaded from ${sharedConfigurationProvider.name()}") }

        val failures = updateResult
            .filter { (_, result) -> result.isErr }

        failures.forEach { (ref, result) ->
            journalist.logger.error("Shared configuration | Cannot update '${ref.name}' due to ${result.error}")
            journalist.logger.debug("Shared configuration | Source:")
            journalist.logger.debug(content)
            failureFacade.throwException("Shared configuration", result.error)
        }

        return failures
            .map { (ref, result) -> ref to result.error }
            .takeIf { it.isNotEmpty() }
            ?.let { SharedSettingsUpdateException(it).asError() }
            ?: ok()
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : SharedSettings> updateSharedSettings(name: String, body: S): Result<S, out Exception>? =
        getSettingsReference<S>(name)
            ?.update(body)
            ?.peek { sharedConfigurationProvider.updateConfiguration(renderConfiguration()) }

    private fun renderConfiguration(): String =
        names()
            .associateWith { getSettingsReference<SharedSettings>(it)!!.get() }
            .let { DEFAULT_OBJECT_MAPPER.writeValueAsString(it) }

    inline fun <reified T : SharedSettings> getDomainSettings(): MutableReference<T> =
        getDomainSettings(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <S : SharedSettings> getDomainSettings(settingsClass: KClass<S>): MutableReference<S> =
        sharedSettingsProvider.domains[settingsClass] as MutableReference<S>

    @Suppress("UNCHECKED_CAST")
    fun <S : SharedSettings> getSettingsReference(name: String): SharedSettingsReference<S>? =
        configHandlers[name] as? SharedSettingsReference<S>

    fun names(): Collection<String> =
        configHandlers.keys

}
