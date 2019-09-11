package org.tiqr.authenticator.security

import android.content.Context
import android.util.Base64
import android.util.Log

import org.tiqr.authenticator.exceptions.SecurityFeaturesException

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.Arrays

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.ShortBufferException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/**
 * Class with a number of encryption related (static) utility functions.
 *
 * @author ivo
 */
object Encryption {

    private val PREFERENCES_KEY = "securitySettings"
    private val SALT_KEY = "salt"
    private val DEVICE_KEY = "deviceKey"

    private val RANDOM_ALGORITHM = "SHA1PRNG" // a randomizer supported by android

    private val PASSWORD_KEY_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC" // A typical AES supported by android (this is the bouncy castle implementation)

    private val SALT_BYTES = 20

    private val MASTER_KEY_ALGORITHM = "HMACSHA256"
    private val CIPHER_KEY_ITERATIONS = 1500
    private val CIPHER_KEY_SIZE = 16
    private val IV_LENGTH = 16
    private val CIPHER_TRANSFORMATION = "AES/CBC/NoPadding"

    /**
     * Get a generic key. It's randomized the first time it's retrieved and stored in the SharedPreferences.
     * This key is used for the keystore for an extra level of encryption.
     *
     * @param ctx
     * @return
     * @throws SecurityFeaturesException
     */
    @Throws(SecurityFeaturesException::class)
    fun getDeviceKey(ctx: Context): SecretKey {
        val settings = ctx.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)

        val bytes: ByteArray

        val value = settings.getString(DEVICE_KEY, null)
        if (value != null) {
            bytes = Base64.decode(value, Base64.DEFAULT)
        } else {

            bytes = getRandomBytes(SALT_BYTES)

            val editor = settings.edit()
            editor.putString(DEVICE_KEY, String(Base64.encode(bytes, Base64.DEFAULT)))
            editor.commit()
        }
        return SecretKeySpec(bytes, "RAW")

    }

    /**
     * Encrypt a plaintext using the method defined in CIPHER_TRANSFORMATION.
     * Depending on the transformation, you may need to pass text in correct
     * blocksize (current implementation should be multiples of 16 bytes)
     *
     *
     * Returns a tuple with the ciphertext and randomly generated iv.
     *
     * @param text
     * @param key
     * @return
     * @throws SecurityFeaturesException
     */
    @Throws(SecurityFeaturesException::class)
    fun encrypt(text: ByteArray, key: Key): CipherPayload {
        try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

            val generatedIV = generateIv()
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(generatedIV))
            val iv = cipher.iv

            /* Some versions of Android don't actually set the IV in that case
             * so we'll test for that and log it, but return the iv that the system
             * says is the one that's been used */
            if (!Arrays.equals(generatedIV, iv)) {
                Log.i("encryption", "Not able to set random IV on this system.")
            }

            val cipherText = ByteArray(cipher.getOutputSize(text.size))
            var ctLength = cipher.update(text, 0, text.size, cipherText, 0)
            ctLength += cipher.doFinal(cipherText, ctLength)
            return CipherPayload(cipherText, iv)
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: NoSuchPaddingException) {
        } catch (e: InvalidKeyException) {
        } catch (e: ShortBufferException) {
        } catch (e: IllegalBlockSizeException) {
        } catch (e: BadPaddingException) {
        } catch (e: NoSuchProviderException) {
        } catch (e: InvalidAlgorithmParameterException) {
        }

        // If any of these fail, we're dealing with a device that can't handle our level of encryption
        throw SecurityFeaturesException()

    }

    /**
     * Decrypts the ciphertext according to CIPHER_TRANSFORMATION.
     * Note that if the cipher transformation defines a padding scheme, then the decrypted
     * string will have padding bytes (length will be a multiple of 16). Since we know
     * the length of what we encoded, we should use substrings to retrieve the result from
     * what decrypt returns to us.
     *
     * @param payload is both the ciphertext and iv, or if no iv, payload.iv is null
     * @param key
     * @return
     * @throws InvalidKeyException
     * @throws SecurityFeaturesException
     */
    @Throws(InvalidKeyException::class, SecurityFeaturesException::class)
    fun decrypt(payload: CipherPayload, key: Key): ByteArray {
        val original = payload.cipherText
        val iv = payload.iv
        try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            //     byte[] original = Base64Coder.decode(text);
            val plainText = ByteArray(cipher.getOutputSize(original.size))
            var ptLength = cipher.update(original, 0, original.size, plainText, 0)
            ptLength += cipher.doFinal(plainText, ptLength)
            ///  return new String(plainText);
            return plainText
        } catch (e: NoSuchAlgorithmException) {
            // Can't work with this device
            throw SecurityFeaturesException()
        } catch (e: NoSuchPaddingException) {
            // Can't work with this device
            throw SecurityFeaturesException()
        } catch (e: InvalidKeyException) {
            // Probably a wrong PIN
            throw InvalidKeyException()
        } catch (e: ShortBufferException) {
            // Probably a wrong PIN
            throw InvalidKeyException()
        } catch (e: IllegalBlockSizeException) {
            // Probably a wrong PIN
            throw InvalidKeyException()
        } catch (e: BadPaddingException) {
            // Probably a wrong PIN
            throw InvalidKeyException()
        } catch (e: InvalidAlgorithmParameterException) {
            // IV was messed up
            throw InvalidKeyException()
        }

    }

    /**
     * Like keyFromPassword this method converts a password to a SecretKey. The difference is that
     * this method allows passing a fixed Salt. This should *NEVER* be done in production code, it's
     * here only to cater for unit tests that need to get predictable results from this function.
     *
     * @param ctx
     * @param password
     * @param salt
     * @return
     * @throws SecurityFeaturesException
     */
    @Throws(SecurityFeaturesException::class)
    @JvmOverloads
    fun keyFromPassword(ctx: Context, password: String, salt: ByteArray = getSalt(ctx)): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, CIPHER_KEY_ITERATIONS, CIPHER_KEY_SIZE)

        val f: SecretKeyFactory
        try {
            // First convert Key to something more secure using the pbekeyspec
            f = SecretKeyFactory.getInstance(PASSWORD_KEY_ALGORITHM)
            return f.generateSecret(spec)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }

        throw SecurityFeaturesException()
    }

    /**
     * Get a Salt. The salt is initially randomized but then stored in sharedpreferences
     * so a consistent salt is used.
     *
     * @param ctx
     * @return
     * @throws SecurityFeaturesException
     */
    @Throws(SecurityFeaturesException::class)
    fun getSalt(ctx: Context): ByteArray {
        val settings = ctx.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)

        val value = settings.getString(SALT_KEY, null)
        if (value != null) {
            return Base64.decode(value, Base64.DEFAULT)
        }

        val bytes = getRandomBytes(SALT_BYTES)

        val editor = settings.edit()
        editor.putString(SALT_KEY, String(Base64.encode(bytes, Base64.DEFAULT)))
        editor.apply()
        return bytes

    }

    /**
     * Get a number of random bytes using a secure random algorithm.
     *
     * @param numberOfBytes
     * @return
     * @throws SecurityFeaturesException
     */
    @Throws(SecurityFeaturesException::class)
    fun getRandomBytes(numberOfBytes: Int): ByteArray {
        val bytes = ByteArray(numberOfBytes)
        try {

            val r = SecureRandom.getInstance(RANDOM_ALGORITHM)
            r.nextBytes(bytes)
            return bytes

        } catch (e: NoSuchAlgorithmException) {
            throw SecurityFeaturesException()
        }

    }

    /**
     * Generate a random key. Like generateRandomKey, but the return value is in SecretKey format.
     *
     * @return
     * @throws SecurityFeaturesException
     */
    @Throws(SecurityFeaturesException::class)
    fun generateRandomSecretKey(): SecretKey {
        return SecretKeySpec(generateRandomKey().encoded, MASTER_KEY_ALGORITHM)
    }

    /**
     * Generate a random key.
     *
     * @return
     * @throws SecurityFeaturesException
     */
    @Throws(SecurityFeaturesException::class)
    fun generateRandomKey(): Key {
        val generator: KeyGenerator
        try {
            generator = KeyGenerator.getInstance(MASTER_KEY_ALGORITHM)
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityFeaturesException()
        }

        generator.init(SecureRandom())
        return generator.generateKey()
    }

    fun bytesToHexString(bArray: ByteArray): String {
        val sb = StringBuffer(bArray.size)
        var sTemp: String
        for (i in bArray.indices) {
            sTemp = Integer.toHexString(0xFF and bArray[i].toInt())
            if (sTemp.length < 2)
                sb.append(0)
            sb.append(sTemp.toUpperCase())
        }
        return sb.toString()
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class)
    private fun generateIv(): ByteArray {
        val random = SecureRandom.getInstance(RANDOM_ALGORITHM)
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)
        return iv
    }

}
/**
 * Convert a password/pincode to an encryption compatible SecretKey by salting the password,
 * hashing it a number of times and making it the correct size for a key.
 *
 * @param ctx
 * @param password
 * @return
 * @throws SecurityFeaturesException
 */
