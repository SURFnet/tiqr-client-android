package org.tiqr.authenticator.buildsrc

import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Read properties from the global and/or project 'gradle.properties' file
 */
object Properties {
    private const val GRADLE_PROPS_FILE = "gradle.properties"

    private val globalProps: Properties?
    private val projectProps: Properties?

    init {
        val globalGradlePath = "${System.getProperty("user.home")}${File.separator}.gradle${File.separator}"
        val globalGradleFile = File(globalGradlePath + GRADLE_PROPS_FILE)
        globalProps = loadProperties(globalGradleFile)

        val projectGradlePath = "${System.getProperty("user.dir")}${File.separator}"
        val projectGradleFile = File(projectGradlePath + GRADLE_PROPS_FILE)
        projectProps = loadProperties(projectGradleFile)
    }

    val gradleAndroidPluginVersion
        get() = getOrNull("gradleAndroidPluginVersion")

    /**
     * Get the gradle property for given [key], or null
     */
    fun getOrNull(key: String): String? = globalProps?.getProperty(key) ?: projectProps?.getProperty(key)

    /**
     * Try to load the [Properties] from the given [file].
     */
    private fun loadProperties(file: File): Properties? {
        return try {
            Properties().apply {
                load(FileInputStream(file))
            }
        } catch (e: Exception) {
            null
        }
    }
}