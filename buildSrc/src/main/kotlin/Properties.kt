package org.tiqr.authenticator.buildsrc

import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Read the global 'gradle.properties' file
 */
object Properties {
    private val props: Properties?

    init {
        val globalGradlePath = "${System.getProperty("user.home")}${File.separator}.gradle${File.separator}"
        val globalGradleFile = File("${globalGradlePath}gradle.properties")

        props = try {
            Properties().apply {
                load(FileInputStream(globalGradleFile))
            }
        } catch (e: Exception) {
            null
        }
    }

    val gradleAndroidPluginVersion = getOrNull("gradleAndroidPluginVersion")

    /**
     * Get the gradle property for given [key], or null
     */
    fun getOrNull(key: String): String? = props?.getProperty(key)
}