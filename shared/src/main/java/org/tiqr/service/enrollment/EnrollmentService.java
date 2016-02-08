package org.tiqr.service.enrollment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.tiqr.Constants;
import org.tiqr.R;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;
import javax.inject.Inject;

/**
 * Enrollment data service.
 */
public class EnrollmentService {
    public interface OnParseEnrollmentChallengeListener {
        public void onParseEnrollmentChallengeSuccess(EnrollmentChallenge challenge);

        public void onParseEnrollmentChallengeError(ParseEnrollmentChallengeError error);
    }

    public interface OnEnrollmentListener {
        public void onEnrollmentSuccess();

        public void onEnrollmentError(EnrollmentError error);
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
                    HttpGet httpGet = new HttpGet(url.toString());
                    httpGet.setHeader("ACCEPT", "application/json");
                    httpGet.setHeader("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION);
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    String json = EntityUtils.toString(httpResponse.getEntity());
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

                    HttpPost httpPost = new HttpPost(challenge.getEnrollmentURL());

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                    nameValuePairs.add(new BasicNameValuePair("secret", _keyToHex(secret)));
                    nameValuePairs.add(new BasicNameValuePair("language", Locale.getDefault().getLanguage()));
                    String notificationAddress = _notificationService.getNotificationToken();
                    if (notificationAddress != null) {
                        nameValuePairs.add(new BasicNameValuePair("notificationType", "GCM"));
                        nameValuePairs.add(new BasicNameValuePair("notificationAddress", notificationAddress));
                    }

                    nameValuePairs.add(new BasicNameValuePair("operation", "register"));

                    httpPost.setHeader("ACCEPT", "application/json");
                    httpPost.setHeader("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION);

                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpResponse httpResponse = httpClient.execute(httpPost);

                    EnrollmentError error = null;

                    Header versionHeader = httpResponse.getFirstHeader("X-TIQR-Protocol-Version");
                    if (versionHeader == null || versionHeader.getValue().equals("1")) {
                        // v1 protocol (ascii)
                        error = _parseV1Response(EntityUtils.toString(httpResponse.getEntity()));
                    } else {
                        // v2 protocol (json)
                        error = _parseV2Response(EntityUtils.toString(httpResponse.getEntity()));
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
                    case 100:
                        type = Type.VERIFICATION_REQUIRED;
                        message = _context.getString(R.string.error_enroll_verification_needed);
                        break;
                    case 102:
                        type = Type.USERNAME_TAKEN;
                        message = _context.getString(R.string.error_enroll_username_taken);
                        break;
                    default:
                        message = _context.getString(R.string.error_enroll_unknown);
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
     * Store identity (and identity provider if needed).
     */
    private void _storeIdentityAndIdentityProvider(EnrollmentChallenge challenge, SecretKey secret, SecretKey sessionKey) throws UserException {
        if (challenge.getIdentityProvider().isNew() && !_dbAdapter.insertIdentityProvider(challenge.getIdentityProvider())) {
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
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ByteArrayBuffer buffer = new ByteArrayBuffer(50);

        int current = 0;
        while ((current = bufferedInputStream.read()) != -1) {
            buffer.append((byte)current);
        }

        return buffer.toByteArray();
    }

    /**
     * Returns a identity provider object based on the given metadata. If the identity provider already exists, the existing identity provider object is
     * returned, else a new one is created.
     *
     * @param metadata JSON identity provider metadata
     * @return IdentityProvider object
     * @throws Exception
     */
    private IdentityProvider _getIdentityProviderForMetadata(JSONObject metadata) throws JSONException, UserException {
        IdentityProvider ip = _dbAdapter.getIdentityProviderByIdentifierAsObject(metadata.getString("identifier"));
        if (ip == null) {
            ip = new IdentityProvider();
            ip.setIdentifier(metadata.getString("identifier"));
            ip.setDisplayName(metadata.getString("displayName"));
            ip.setAuthenticationURL(metadata.getString("authenticationUrl"));
            ip.setInfoURL(metadata.getString("infoUrl"));
            if (metadata.has("ocraSuite")) {
                ip.setOCRASuite(metadata.getString("ocraSuite"));
            }
            try {
                URL logoURL = new URL(metadata.getString("logoUrl"));
                byte[] logoData = _downloadSynchronously(logoURL);
                ip.setLogoData(logoData);
            } catch (Exception ex) {
                throw new UserException(_context.getString(R.string.error_enroll_logo_error), ex);
            }

            if (ip.getLogoBitmap() == null) {
                throw new UserException(_context.getString(R.string.error_enroll_logo_error));
            }
        }

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
        Identity identity = _dbAdapter.getIdentityByIdentifierAndIdentityProviderIdAsObject(metadata.getString("identifier"), ip.getId());
        if (identity != null) {
            Object[] args = new Object[]{metadata.getString("displayName"), ip.getDisplayName()};
            throw new UserException(_context.getString(R.string.error_enroll_already_enrolled, args));
        }

        identity = new Identity();
        identity.setIdentifier(metadata.getString("identifier"));
        identity.setDisplayName(metadata.getString("displayName"));
        return identity;
    }


    private String _keyToHex(SecretKey secret) {
        byte[] buf = secret.getEncoded();
        StringBuffer stringBuffer = new StringBuffer(buf.length * 2);
        int i;

        for (i = 0; i < buf.length; i++) {
            if (((int)buf[i] & 0xff) < 0x10)
                stringBuffer.append("0");

            stringBuffer.append(Long.toString((int)buf[i] & 0xff, 16));
        }

        return stringBuffer.toString();
    }
}