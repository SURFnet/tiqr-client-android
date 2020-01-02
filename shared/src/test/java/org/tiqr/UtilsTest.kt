package org.tiqr

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.tiqr.service.Token

internal class UtilsUnitTest {

    private val testableConnection = Mockito.mock(TestableConnectionWrapper::class.java)

    @Test
    internal fun `when the response code is not in the 200 range then the token is invalid`() {
        whenever(testableConnection.responseCode).thenReturn(HTTP_FAIL)

        val newToken = Utils.testableReadResponse(testableConnection)

        assertThat(newToken).isEqualTo(Token.Invalid)
    }

    @Test
    internal fun `when the response code is in the 200 range and the response is other than NOT FOUND, then the token is valid`() {
        whenever(testableConnection.responseCode).thenReturn(HTTP_OK)
        whenever(testableConnection.readResponse()).thenReturn(VALID_TOKEN)

        val newToken = Utils.testableReadResponse(testableConnection)

        assertThat(newToken).isEqualTo(Token.Valid(VALID_TOKEN))
    }

    @Test
    internal fun `when the response code is in the 200 range and the response is NOT FOUND, then the token is invalid`() {
        whenever(testableConnection.responseCode).thenReturn(HTTP_OK)
        whenever(testableConnection.readResponse()).thenReturn(INVALID_TOKEN)

        val newToken = Utils.testableReadResponse(testableConnection)

        assertThat(newToken).isEqualTo(Token.Invalid)
    }

    @Test
    internal fun `when the response code is in the 200 range but reading the response throws an error, then the token is invalid`() {
        whenever(testableConnection.responseCode).thenReturn(HTTP_OK)
        whenever(testableConnection.readResponse()).thenThrow(Exception())

        val newToken = Utils.testableReadResponse(testableConnection)

        assertThat(newToken).isEqualTo(Token.Invalid)
    }


    companion object {
        const val VALID_TOKEN = "valid"
        const val INVALID_TOKEN = "NOT FOUND"

        const val HTTP_OK = 200
        const val HTTP_FAIL = 500
    }

}