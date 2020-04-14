/*
 * Copyright (c) 2010-2020 SURFnet bv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of SURFnet bv nor the names of its contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tiqr.data.api.adapter

import com.squareup.moshi.*
import java.io.IOException
import kotlin.reflect.KClass

/**
 * [JsonAdapter] to map integers to [Enum]'s which implement [ValueEnum]
 */
class EnumValueJsonAdapter<T : ValueEnum> private constructor(
        private val enumType: Class<T>,
        private val fallbackValue: T?,
        private val useFallbackValue: Boolean = fallbackValue != null
) : JsonAdapter<T>() {
    private val constants: Array<out T>? = enumType.enumConstants
    private val enumValues: Map<Int, T> = constants?.associateTo(hashMapOf()) { it.value to it } ?: hashMapOf()

    companion object {
        fun <T : ValueEnum> create(enumType: Class<T>, unknownFallback: T? = null): EnumValueJsonAdapter<T> {
            return EnumValueJsonAdapter(enumType, unknownFallback)
        }
    }

    /**
     * Create a new adapter for this enum with a fallback value to use when the JSON value does not
     * match any of the enum's constants. Note that this value will not be used when the JSON value is
     * null, absent, or not a string. Also, the string values are case-sensitive, and this fallback
     * value will be used even on case mismatches.
     */
    fun withUnknownFallback(fallbackValue: T?): EnumValueJsonAdapter<T> = EnumValueJsonAdapter(enumType, fallbackValue, true)

    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): T? {
        val value = reader.nextInt()
        if (enumValues.containsKey(value)) {
            return enumValues[value]
        }

        val path = reader.path
        if (!useFallbackValue) {
            throw JsonDataException("Unknown value of enum ${enumType.name} ($value) at path $path")
        }
        return fallbackValue
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: T?) {
        if (value == null) {
            throw NullPointerException("value was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.value(value.value)
    }

    override fun toString() = "EnumJsonAdapter(" + enumType.name + ")"
}

interface ValueEnum {
    val value: Int
}

fun <T : ValueEnum> Moshi.Builder.addValueEnum(kClass: KClass<T>, unknownFallback: T? = null): Moshi.Builder {
    return add(kClass.java, EnumValueJsonAdapter.create(kClass.java, unknownFallback).nullSafe())
}