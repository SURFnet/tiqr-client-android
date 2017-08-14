package org.tiqr.service.enrollment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.tiqr.Constants;
import org.tiqr.R;
import org.tiqr.Utils;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.datamodel.IdentityProvider;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;
import org.tiqr.authenticator.exceptions.UserException;
import org.tiqr.authenticator.security.Encryption;
import org.tiqr.authenticator.security.Secret;
import org.tiqr.service.enrollment.EnrollmentError.Type;
import org.tiqr.service.notification.NotificationService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.inject.Inject;

/**
 * Enrollment data service.
 */
public class EnrollmentService {

    public interface OnParseEnrollmentChallengeListener {
        void onParseEnrollmentChallengeSuccess(EnrollmentChallenge challenge);

        void onParseEnrollmentChallengeError(ParseEnrollmentChallengeError error);
    }

    public interface OnEnrollmentListener {
        void onEnrollmentSuccess();

        void onEnrollmentError(EnrollmentError error);
    }

    protected
    @Inject
    NotificationService _notificationService;

    protected
    @Inject
    Context _context;

    protected
    @Inject
    DbAdapter _dbAdapter;

    /**
     * Contains an enrollment challenge?
     *
     * @param rawChallenge Raw challenge.
     * @return Is enrollment challenge?
     */
    public boolean isEnrollmentChallenge(String rawChallenge) {
        return rawChallenge.startsWith("tiqrenroll://");
    }

    /**
     * Parses the raw enrollment challenge.
     *
     * @param rawChallenge Raw challenge.
     * @param listener     Completion listener.
     */
    public AsyncTask<?, ?, ?> parseEnrollmentChallenge(final String rawChallenge, final OnParseEnrollmentChallengeListener listener) {
        AsyncTask<Void, Void, Object> task = new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... voids) {
                EnrollmentChallenge challenge = new EnrollmentChallenge();

                if (!rawChallenge.startsWith("tiqrenroll://")) {
                    return new ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.enrollment_failure_title), _context.getString(R.string.error_enroll_invalid_qr_code));
                }

                URL url;
                try {
                    url = new URL(rawChallenge.substring(13));
                } catch (MalformedURLException ex) {
                    return new ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.enrollment_failure_title), _context.getString(R.string.error_enroll_invalid_qr_code));
                }

                if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                    return new ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.enrollment_failure_title), _context.getString(R.string.error_enroll_invalid_qr_code));
                }

                JSONObject metadata;

                try {
                    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                    urlConnection.addRequestProperty("ACCEPT", "application/json");
                    urlConnection.addRequestProperty("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION);
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();
                    String json = Utils.urlConnectionResponseAsString(urlConnection);
                    Log.d(getClass().getSimpleName(), "Enrollment server response: " + json);
                    JSONTokener tokener = new JSONTokener(json);
                    Object value = tokener.nextValue();
                    if (!(value instanceof JSONObject)) {
                        throw new UserException(_context.getString(R.string.error_enroll_invalid_response));
                    }

                    metadata = (JSONObject)value;
                    challenge.setEnrollmentURL(metadata.getJSONObject("service").getString("enrollmentUrl"));
                    challenge.setReturnURL(null); // TODO: FIXME
                    challenge.setIdentityProvider(_getIdentityProviderForMetadata(metadata.getJSONObject("service")));
                    challenge.setIdentity(_getIdentityForMetadata(metadata.getJSONObject("identity"), challenge.getIdentityProvider()));
                } catch (IOException ex) {
                    return new ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.CONNECTION, _context.getString(R.string.enrollment_failure_title), _context.getString(R.string.error_enroll_connect_error));
                } catch (JSONException ex) {
                    return new ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_RESPONSE, _context.getString(R.string.enrollment_failure_title), _context.getString(R.string.error_enroll_invalid_response));
                } catch (UserException ex) {
                    return new ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.enrollment_failure_title), ex.getMessage());
                }

                return challenge;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (result instanceof EnrollmentChallenge) {
                    EnrollmentChallenge challenge = (EnrollmentChallenge)result;
                    listener.onParseEnrollmentChallengeSuccess(challenge);
                } else {
                    ParseEnrollmentChallengeError error = (ParseEnrollmentChallengeError)result;
                    listener.onParseEnrollmentChallengeError(error);
                }
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
    }

    /**
     * Send enrollment request to server.
     *
     * @param challenge Enrollment challenge.
     * @param password  Password / PIN.
     * @param listener  Completion listener.
     */
    public AsyncTask<?, ?, ?> enroll(final EnrollmentChallenge challenge, final String password, final OnEnrollmentListener listener) {
        AsyncTask<Void, Void, EnrollmentError> task = new AsyncTask<Void, Void, EnrollmentError>() {
            @Override
            protected EnrollmentError doInBackground(Void... voids) {
                try {
                    SecretKey sessionKey = Encryption.keyFromPassword(_context, password);

                    SecretKey secret = _generateSecret();


                    Map<String, String> nameValuePairs = new HashMap<>();
                    nameValuePairs.put("secret", _keyToHex(secret));
                    nameValuePairs.put("language", Locale.getDefault().getLanguage());
                    String notificationAddress = _notificationService.getNotificationToken();
                    if (notificationAddress != null) {
                        nameValuePairs.put("notificationType", "GCM");
                        nameValuePairs.put("notificationAddress", notificationAddress);
                    }

                    nameValuePairs.put("operation", "register");

                    URL enrollmentURL = new URL(challenge.getEnrollmentURL());
                    HttpURLConnection httpURLConnection = (HttpURLConnection)enrollmentURL.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty("ACCEPT", "application/json");
                    httpURLConnection.setRequestProperty("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION);
                    httpURLConnection.setDoOutput(true);
                    byte[] postData = Utils.keyValueMapToByteArray(nameValuePairs);
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(postData.length));
                    httpURLConnection.getOutputStream().write(postData);
                    String response = Utils.urlConnectionResponseAsString(httpURLConnection);

                    String versionHeader = httpURLConnection.getHeaderField("X-TIQR-Protocol-Version");

                    EnrollmentError error;
                    if (versionHeader == null || versionHeader.equals("1")) {
                        // v1 protocol (ascii)
                        error = _parseV1Response(response);
                    } else {
                        // v2 protocol (json)
                        error = _parseV2Response(response);
                    }

                    if (error == null) {
                        _storeIdentityAndIdentityProvider(challenge, secret, sessionKey);
                        return null;
                    } else {
                        return error;
                    }
                } catch (Exception ex) {
                    return new EnrollmentError(Type.CONNECTION, _context.getString(R.string.error_enroll_connect_error), _context.getString(R.string.error_enroll_connect_error));
                }
            }

            @Override
            protected void onPostExecute(EnrollmentError error) {
                if (error == null) {
                    listener.onEnrollmentSuccess();
                } else {
                    listener.onEnrollmentError(error);
                }
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
    }


    /**
     * Parse v1 response format (ascii), return error object when unsuccessful.
     *
     * @param response
     * @return Error object on failure.
     */
    private EnrollmentError _parseV1Response(String response) {
        if (response != null && response.equals("OK")) {
            return null;
        } else {
            return new EnrollmentError(Type.UNKNOWN, _context.getString(R.string.enrollment_failure_title), response);
        }
    }

    /**
     * Parse v2 response format (json), return error object when unsuccessful.
     *
     * @param response
     * @return Error object on failure.
     */
    private EnrollmentError _parseV2Response(String response) {
        try {
            JSONObject object = new JSONObject(response);

            int responseCode = object.getInt("responseCode");
            if (responseCode == 1) {
                return null; // success, no error
            }

            Type type = Type.UNKNOWN;
            String message = object.optString("message", "");

            if (message.length() == 0) {
                switch (responseCode) {
                    case 101:
                        type = Type.INVALID_RESPONSE;
                        message = _context.getString(R.string.error_enroll_general);
                        break;
                    default:
                        type = Type.UNKNOWN;
                        message = _context.getString(R.string.error_enroll_general);
                        break;
                }
            }

            return new EnrollmentError(type, _context.getString(R.string.enrollment_failure_title), message);
        } catch (JSONException e) {
            return new EnrollmentError(Type.INVALID_RESPONSE, _context.getString(R.string.enrollment_failure_title), _context.getString(R.string.error_enroll_invalid_response));
        }

    }

    /**
     * Generate identity secret.
     *
     * @return secret key
     * @throws UserException
     */
    private SecretKey _generateSecret() throws UserException {
        try {
            return Encryption.generateRandomSecretKey();
        } catch (Exception ex) {
            throw new UserException(_context.getString(R.string.error_enroll_failed_to_generate_secret));
        }
    }

    /**
     * Store identity and identity provider.
     */
    private void _storeIdentityAndIdentityProvider(EnrollmentChallenge challenge, SecretKey secret, SecretKey sessionKey) throws UserException {
        if (!_dbAdapter.insertIdentityProvider(challenge.getIdentityProvider())) {
            throw new UserException(_context.getString(R.string.error_enroll_failed_to_store_identity_provider));
        }

        if (!_dbAdapter.insertIdentityForIdentityProvider(challenge.getIdentity(), challenge.getIdentityProvider())) {
            throw new UserException(_context.getString(R.string.error_enroll_failed_to_store_identity));
        }

        Secret secretStore = Secret.secretForIdentity(challenge.getIdentity(), _context);
        secretStore.setSecret(secret);
        try {
            secretStore.storeInKeyStore(sessionKey);
        } catch (SecurityFeaturesException e) {
            throw new UserException(_context.getString(R.string.error_device_incompatible_with_security_standards));
        }
    }


    /**
     * Download data from the given URL (synchronously).
     *
     * @param url url
     * @return data
     */
    private byte[] _downloadSynchronously(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Returns a identity provider object based on the given metadata.
     *
     * @param metadata JSON identity provider metadata
     * @return IdentityProvider object
     * @throws Exception
     */
    private IdentityProvider _getIdentityProviderForMetadata(JSONObject metadata) throws JSONException, UserException {
        IdentityProvider ip = new IdentityProvider();

        ip.setIdentifier(metadata.getString("identifier"));
        ip.setDisplayName(metadata.getString("displayName"));
        ip.setAuthenticationURL(metadata.getString("authenticationUrl"));
        ip.setInfoURL(metadata.getString("infoUrl"));
        if (metadata.has("ocraSuite")) {
            ip.setOCRASuite(metadata.getString("ocraSuite"));
        }
        ip.setLogoURL(metadata.getString("logoUrl"));
        return ip;
    }

    /**
     * Returns an identity object based on the given metadata. If the identity already exists an exception is thrown.
     *
     * @param metadata JSON identity metadata
     * @return identity object
     * @throws Exception
     */
    private Identity _getIdentityForMetadata(JSONObject metadata, IdentityProvider ip) throws JSONException, UserException {
        Identity identity = _dbAdapter.getIdentityByIdentifierAndIdentityProviderIdentifierAsObject(metadata.getString("identifier"), ip.getIdentifier());
        if (identity != null) {
            Object[] args = new Object[] { metadata.getString("displayName"), ip.getDisplayName() };
            throw new UserException(_context.getString(R.string.error_enroll_already_enrolled, args));
        }

        identity = new Identity();
        identity.setIdentifier(metadata.getString("identifier"));
        identity.setDisplayName(metadata.getString("displayName"));
        return identity;
    }

    private String _keyToHex(SecretKey secret) {
        byte[] buf = secret.getEncoded();
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;

        for (i = 0; i < buf.length; i++) {
            if (((int)buf[i] & 0xff) < 0x10)
                strbuf.append("0");

            strbuf.append(Long.toString((int)buf[i] & 0xff, 16));
        }

        return strbuf.toString();
    }
}