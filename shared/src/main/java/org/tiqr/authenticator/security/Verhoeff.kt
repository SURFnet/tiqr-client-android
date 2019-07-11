/*
 * Copyright (c) 2010-2011 SURFnet bv
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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
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

package org.tiqr.authenticator.security

/**
 * Class that implements the Verhoeff Algorithm, a checksum formula for error detection
 *
 * @see http://en.wikipedia.org/wiki/Verhoeff_algorithm
 *
 * @author Felix De Vliegher <felix></felix>@egeniq.com>
 */
object Verhoeff {

    // The multiplication table
    internal var d = arrayOf(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5), intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6), intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7), intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8), intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1), intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2), intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3), intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4), intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0))

    // The permutation table
    internal var p = arrayOf(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4), intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2), intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7), intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0), intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1), intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5), intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8))

    // The inverse table
    internal var inv = intArrayOf(0, 4, 3, 2, 1, 5, 6, 7, 8, 9)

    /**
     * For a given number generates a Verhoeff digit
     *
     * @param    num        The number
     * @return            The Verhoeff checksum digit
     */
    fun verhoeffDigit(num: String): Int {

        var c = 0
        val myArray = StringToReversedIntArray(num)

        for (i in myArray.indices) {
            c = d[c][p[(i + 1) % 8][myArray[i]]]
        }

        return inv[c]
    }

    /**
     * Validates that an entered number is Verhoeff compliant.
     *
     * NB: Make sure the check digit is the last one.
     *
     * @param    num        The number
     * @return    boolean
     */
    fun validateVerhoeff(num: String): Boolean {

        var c = 0
        val myArray = StringToReversedIntArray(num)

        for (i in myArray.indices) {
            c = d[c][p[i % 8][myArray[i]]]
        }

        return c == 0
    }

    /**
     * Converts a string to a reversed integer array
     *
     * @param    num        The number
     * @return
     */
    private fun StringToReversedIntArray(num: String): IntArray {

        var myArray = IntArray(num.length)

        for (i in 0 until num.length) {
            myArray[i] = Integer.parseInt(num.substring(i, i + 1))
        }

        myArray = Reverse(myArray)

        return myArray

    }

    /**
     * Reverses an int array
     *
     * @param    int[]	myArray		The array to be reversed
     * @return    int[]
     */
    private fun Reverse(myArray: IntArray): IntArray {
        val reversed = IntArray(myArray.size)

        for (i in myArray.indices) {
            reversed[i] = myArray[myArray.size - (i + 1)]
        }

        return reversed
    }
}