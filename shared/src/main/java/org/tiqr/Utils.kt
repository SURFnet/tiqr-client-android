package org.tiqr

import android.util.Log
import androidx.annotation.VisibleForTesting
import org.tiqr.service.Token
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URLEncoder
import kotlin.experimental.and

object Utils {

    private val TAG = "org.tiqr.KUtils"
    private const val NOT_FOUND = "NOT FOUND"
    private val REPLACEMENT_CHAR = 0xfffd.toChar()

    fun urlConnectionResponseAsString(connection: HttpURLConnection): String? {
        return try {
            when (connection.responseCode) {
                in 200..299 -> connection.inputStream
                else -> connection.errorStream
            }.bufferedReader().use { it.readText() }  // defaults to UTF-8
        } catch (e: Exception) {
            Log.e(TAG, "Error reading InputStream", e)
            null
        }
    }

    fun urlConnectionResponse(connection: HttpURLConnection): Token = testableReadResponse(TestableConnectionWrapper(connection))

    @VisibleForTesting
    fun testableReadResponse(connection: TestableConnectionWrapper): Token = try {
        if (connection.responseCode in 200..299) {
            // defaults to UTF-8
            val validToken = connection.readResponse()
            if (NOT_FOUND == validToken) {
                Token.Invalid
            } else {
                Token.Valid(validToken)
            }
        } else {
            Token.Invalid
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error reading InputStream", e)
        Token.Invalid
    }

    fun keyValueMapToByteArray(keyValueMap: Map<String, String>): ByteArray {
        try {
            val stringBuilder = StringBuilder()
            for (entry in keyValueMap.entries) {
                if (stringBuilder.length != 0) stringBuilder.append('&')
                stringBuilder.append(URLEncoder.encode(entry.key, "UTF-8"))
                stringBuilder.append('=')
                stringBuilder.append(URLEncoder.encode(entry.value, "UTF-8"))
            }
            return stringBuilder.toString().toByteArray(charset("UTF-8"))

        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("UTF-8 encoding is not supported!", e)
        }
    }

    fun byteArrayToCharArray(data: ByteArray): CharArray {
        val value: CharArray
        val offset = 0
        val byteCount = data.size
        val v = CharArray(byteCount)
        var idx = offset
        val last = offset + byteCount
        var s = 0
        outer@ while (idx < last) {
            val b0 = data[idx++]
            if (b0 and 0x80.toByte() == 0.toByte()) {
                // 0xxxxxxx
                // Range:  U-00000000 - U-0000007F
                val `val`: Int = (b0 and 0xff.toByte()).toInt()
                v[s++] = `val`.toChar()
            } else if (b0 and 0xe0.toByte() == 0xc0.toByte() || b0 and 0xf0.toByte() == 0xe0.toByte() ||
                    b0 and 0xf8.toByte() == 0xf0.toByte() || b0 and 0xfc.toByte() == 0xf8.toByte() || b0 and 0xfe.toByte() == 0xfc.toByte()) {
                var utfCount = 1
                if (b0 and 0xf0.toByte() == 0xe0.toByte()) utfCount = 2 else if (b0 and 0xf8.toByte() == 0xf0.toByte()) utfCount = 3 else if (b0 and 0xfc.toByte() == 0xf8.toByte()) utfCount = 4 else if (b0 and 0xfe.toByte() == 0xfc.toByte()) utfCount = 5
                // 110xxxxx (10xxxxxx)+
                // Range:  U-00000080 - U-000007FF (count == 1)
                // Range:  U-00000800 - U-0000FFFF (count == 2)
                // Range:  U-00010000 - U-001FFFFF (count == 3)
                // Range:  U-00200000 - U-03FFFFFF (count == 4)
                // Range:  U-04000000 - U-7FFFFFFF (count == 5)
                if (idx + utfCount > last) {
                    v[s++] = REPLACEMENT_CHAR
                    continue
                }
                // Extract usable bits from b0
                var `val`: Int = b0.toInt() and (0x1f shr utfCount - 1)
                for (i in 0 until utfCount) {
                    val b = data[idx++]
                    if (b and 0xc0.toByte() != 0x80.toByte()) {
                        v[s++] = REPLACEMENT_CHAR
                        idx-- // Put the input char back
                        continue@outer
                    }
                    // Push new bits in from the right side
                    `val` = `val` shl 6
                    `val` = `val` or (b and 0x3f.toByte()).toInt()
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
                if (utfCount != 2 && `val` >= 0xD800 && `val` <= 0xDFFF) {
                    v[s++] = REPLACEMENT_CHAR
                    continue
                }
                // Reject chars greater than the Unicode maximum of U+10FFFF.
                if (`val` > 0x10FFFF) {
                    v[s++] = REPLACEMENT_CHAR
                    continue
                }
                // Encode chars from U+10000 up as surrogate pairs
                if (`val` < 0x10000) {
                    v[s++] = `val`.toChar()
                } else {
                    val x = `val` and 0xffff
                    val u = `val` shr 16 and 0x1f
                    val w = u - 1 and 0xffff
                    val hi = 0xd800 or (w shl 6) or (x shr 10)
                    val lo = 0xdc00 or (x and 0x3ff)
                    v[s++] = hi.toChar()
                    v[s++] = lo.toChar()
                }
            } else {
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


    /***
     * This method is kept here just for compatibility reasons. It does not work correctly,
     * use the method above!
     * We only use this to log some users in who have been using this method in version 3.0.6.
     * Again, DO NOT USE!
     ***/
    @Deprecated("Only for backwards compatibility")
    fun byteArrayToCharArray306Wrong(data: ByteArray): CharArray {
        val value: CharArray
        val offset = 0
        val byteCount = data.size

        val v = CharArray(byteCount)
        var idx = offset
        val last = offset + byteCount
        var s = 0
        outer@ while (idx < last) {
            val b0 = data[idx++]
            if (b0.toInt().and(0x80) == 0) {
                // 0xxxxxxx
                // Range:  U-00000000 - U-0000007F
                val `val` = b0.toInt().and(0xff)
                v[s++] = `val`.toChar()
            } else if (b0.toInt().and(0xe0) == 0xc0 || (b0.toInt().and(0xf0) == 0xe0) ||
                    b0.toInt().and(0xf8) == 0xf0 || b0.toInt().and(0xfc) == 0xf8 || b0.toInt().and(0xfe) == 0xfc) {
                var utfCount = 1
                if (b0.toInt().and(0xf0) == 0xe0)
                    utfCount = 2
                else if (b0.toInt().and(0xf8) == 0xf0)
                    utfCount = 3
                else if (b0.toInt().and(0xfc) == 0xf8)
                    utfCount = 4
                else if (b0.toInt().and(0xfe) == 0xfc) utfCount = 5
                // 110xxxxx (10xxxxxx)+
                // Range:  U-00000080 - U-000007FF (count == 1)
                // Range:  U-00000800 - U-0000FFFF (count == 2)
                // Range:  U-00010000 - U-001FFFFF (count == 3)
                // Range:  U-00200000 - U-03FFFFFF (count == 4)
                // Range:  U-04000000 - U-7FFFFFFF (count == 5)
                if (idx + utfCount > last) {
                    v[s++] = REPLACEMENT_CHAR
                    continue
                }
                // Extract usable bits from b0
                var `val` = (b0.toInt().and(0x1f.shr(utfCount - 1).toInt())).toByte()
                for (i in 0 until utfCount) {
                    val b = data[idx++]
                    if (b.toInt().and(0xc0) != 0x80) {
                        v[s++] = REPLACEMENT_CHAR
                        idx-- // Put the input char back
                        continue@outer
                    }
                    // Push new bits in from the right side
                    `val` = `val`.toInt().shl(6).toByte()
                    `val` = (`val`.toInt().or(b.toInt().and(0x3f).toInt())).toByte()
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
                if (utfCount != 2 && `val` >= 0xD800 && `val` <= 0xDFFF) {
                    v[s++] = REPLACEMENT_CHAR
                    continue
                }
                // Reject chars greater than the Unicode maximum of U+10FFFF.
                if (`val` > 0x10FFFF) {
                    v[s++] = REPLACEMENT_CHAR
                    continue
                }
                // Encode chars from U+10000 up as surrogate pairs
                if (`val` < 0x10000) {
                    v[s++] = `val`.toChar()
                } else {
                    val x = `val`.toInt().and(0xffff)
                    val u = `val`.toInt().shr(16).and(0x1f)
                    val w = (u - 1).and(0xffff)
                    val hi = 0xd800.or(w.shl(6).or(x.shr(10)))
                    val lo = 0xdc00.or(x.and(0x3ff))
                    v[s++] = hi.toChar()
                    v[s++] = lo.toChar()
                }
            } else {
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

}

open class TestableConnectionWrapper(private val httpConnection: HttpURLConnection) {
    open val responseCode = httpConnection.responseCode

    @Throws(Exception::class)
    open fun readResponse(): String = httpConnection.inputStream.bufferedReader().use { it.readText() }
}