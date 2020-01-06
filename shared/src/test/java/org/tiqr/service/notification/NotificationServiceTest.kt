package org.tiqr.service.notification

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.tiqr.service.Token

internal class NotificationServiceTest {

    private val context = mock(Context::class.java)

    @Test
    fun `when an existing token is corrupt, it should be removed`() {
        val service = NotificationServiceSUT("existing token is corrupted", true, Token.Invalid, context)

        service.validateExistingToken()

        assertThat(service.performedSave).isEqualTo(NewToken.IsCleared)
    }

    @Test
    internal fun `when we have a valid token, it should be part of the post data`() {
        val service = NotificationServiceSUT(VALID_TOKEN, true, Token.Valid(VALID_TOKEN), context)

        val postData = service.createPostData(DEVICE_TOKEN)

        assertThat(postData.keys.size).isEqualTo(2)
    }

    @Test
    internal fun `when we have a valid token, it's value should be in the post data`() {
        val service = NotificationServiceSUT(VALID_TOKEN, true, Token.Valid(VALID_TOKEN), context)

        val postData = service.createPostData(DEVICE_TOKEN)

        assertThat(postData["notificationToken"]).isEqualTo(VALID_TOKEN)
    }

    @Test
    internal fun `for a given device token, it's value should be in the post data`() {
        val service = NotificationServiceSUT(VALID_TOKEN, true, Token.Valid(VALID_TOKEN), context)

        val postData = service.createPostData(DEVICE_TOKEN)

        assertThat(postData["deviceToken"]).isEqualTo(DEVICE_TOKEN)
    }

    @Test
    internal fun `when a new valid token is received it should be saved`() {
        val service = NotificationServiceSUT(null, true, Token.Valid(VALID_TOKEN), context)

        service.testableRequestNewToken(DEVICE_TOKEN)

        assertThat(service.performedSave).isEqualTo(NewToken.HasValue(VALID_TOKEN))
    }

    companion object {
        const val VALID_TOKEN = " This. Be. VALID. "
        const val DEVICE_TOKEN = "DeviceToken"
    }

    class NotificationServiceSUT(existingToken: String?, shouldValidate: Boolean, private val newToken: Token, context: Context) : NotificationService(context) {
        var performedSave: NewToken? = null

        override val notificationToken = existingToken
        override var shouldValidateExistingToken = shouldValidate

        override fun requestToken(nameValuePairs: HashMap<String, String>): Token = newToken

        override fun saveNewToken(notificationToken: String?) {
            //We don't want this to go to shared preferences, it adds no value
            performedSave = if (notificationToken == null) {
                NewToken.IsCleared
            } else {
                NewToken.HasValue(notificationToken)
            }
        }

    }

    sealed class NewToken {
        data class HasValue(val value: String) : NewToken()
        object IsCleared : NewToken()
    }
}