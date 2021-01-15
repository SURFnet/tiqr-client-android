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

package org.tiqr.data.util.extension

import java.util.*

/**
 * Convert this [ByteArray] into a hexadecimal string representation
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

/**
 * Convert this [String] in hexadecimal representation into a [ByteArray]
 */
fun String.hexAsByteArray(): ByteArray = chunked(2).map { it.toLowerCase(Locale.ROOT).toInt(16).toByte() }.toByteArray()

/**
 * The default implementation to convert a [ByteArray] to a [CharArray]
 */
fun ByteArray.toCharArray(): CharArray = String(this).toCharArray()

/**
 * The implementation on Android < 9 to convert a [ByteArray] to a [CharArray]
 * TODO: add proper tests
 */
fun ByteArray.toCharArrayFallback(): CharArray {
    return JavaByteArray.byteArrayToCharArray(this)
}

    val v = CharArray(byteCount)
    var idx = offset
    val last = offset + byteCount
    var s = 0

    outer@ while (idx < last) {
        val b0: Int = this[idx++].toInt()
        if (b0 and 0x80 == 0) {
            // 0xxxxxxx
            // Range:  U-00000000 - U-0000007F
            val va = b0 and 0xff
            v[s++] = va.toChar()
        }
        else if (b0 and 0xe0 == 0xc0
                || b0 and 0xf0 == 0xe0
                || b0 and 0xf8 == 0xf0
                || b0 and 0xfc == 0xf8
                || b0 and 0xfe == 0xfc) {
            val utfCount = when {
                // 110xxxxx (10xxxxxx)+
                b0 and 0xf0 == 0xe0 -> 2 // Range:  U-00000800 - U-0000FFFF (count == 2)
                b0 and 0xf8 == 0xf0 -> 3 // Range:  U-00010000 - U-001FFFFF (count == 3)
                b0 and 0xfc == 0xf8 -> 4 // Range:  U-00200000 - U-03FFFFFF (count == 4)
                b0 and 0xfe == 0xfc -> 5 // Range:  U-04000000 - U-7FFFFFFF (count == 5)
                else -> 1                // Range:  U-00000080 - U-000007FF (count == 1)
            }
            if (idx + utfCount > last) {
                v[s++] = REPLACEMENT_CHAR
                continue
            }
            // Extract usable bits from b0
            var va = b0 and (0x1f shr utfCount - 1)
            for (i in 0 until utfCount) {
                val b: Int = this[idx++].toInt()
                if ((b and 0xc0) != 0x80) {
                    v[s++] = REPLACEMENT_CHAR
                    idx-- // Put the input char back
                    continue@outer
                }
                // Push new bits in from the right side
                va = va shl 6
                va = va or b and 0x3f
            }
            // Note: Java allows overlong char
            // specifications To disallow, check that val
            // is greater than or equal to the minimum
            // value for each count:
            //
            // count    min value
            // -----   ----------
            //   1           0x80
            //   2          0x800
            //   3        0x10000
            //   4       0x200000
            //   5      0x4000000
            // Allow surrogate values (0xD800 - 0xDFFF) to
            // be specified using 3-byte UTF values only
            if (utfCount != 2 && va >= 0xD800 && va <= 0xDFFF) {
                v[s++] = REPLACEMENT_CHAR
                continue
            }
            // Reject chars greater than the Unicode maximum of U+10FFFF.
            if (va > 0x10FFFF) {
                v[s++] = REPLACEMENT_CHAR
                continue
            }
            // Encode chars from U+10000 up as surrogate pairs
            if (va < 0x10000) {
                v[s++] = va.toChar()
            } else {
                val x = va and 0xffff
                val u = va shr 16 and 0x1f
                val w = u - 1 and 0xffff
                val hi = 0xd800 or (w shl 6) or (x shr 10)
                val lo = 0xdc00 or (x and 0x3ff)
                v[s++] = hi.toChar()
                v[s++] = lo.toChar()
            }
        }
        else {
            // Illegal values 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
            v[s++] = REPLACEMENT_CHAR
        }
    }
    if (s == byteCount) {
        // We guessed right, so we can use our temporary array as-is.
        value = v
    } else {
        // Our temporary array was too big, so reallocate and copy.
        value = CharArray(s)
        System.arraycopy(v, 0, value, 0, s)
    }
    return value
}