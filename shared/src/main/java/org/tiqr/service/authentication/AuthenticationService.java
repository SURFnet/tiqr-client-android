package org.tiqr.service.authentication;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.tiqr.Constants;
import org.tiqr.R;
import org.tiqr.Utils;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.datamodel.IdentityProvider;
import org.tiqr.authenticator.exceptions.InvalidChallengeException;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;
import org.tiqr.authenticator.security.Encryption;
import org.tiqr.authenticator.security.OCRAProtocol;
import org.tiqr.authenticator.security.OCRAWrapper;
import org.tiqr.authenticator.security.OCRAWrapper_v1;
import org.tiqr.authenticator.security.Secret;
import org.tiqr.service.authentication.AuthenticationError.Type;
import org.tiqr.service.notification.NotificationService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.inject.Inject;

/**
 * Authentication data service.
 */
public class AuthenticationService {
    public interface OnParseAuthenticationChallengeListener {
        public void onParseAuthenticationChallengeSuccess(AuthenticationChallenge challenge);
        public void onParseAuthenticationChallengeError(ParseAuthenticationChallengeError error);
    }

    public interface OnAuthenticationListener {
        public void onAuthenticationSuccess();
        public void onAuthenticationError(AuthenticationError error);
    }

    protected @Inject
    NotificationService _notificationService;
    protected @Inject Context _context;

    /**
     * Contains an authentication challenge?
     *
     * @param rawChallenge Raw challenge.
     *
     * @return Is authentication challenge?
     */
    public boolean isAuthenticationChallenge(String rawChallenge) {
        return rawChallenge.startsWith("tiqrauth://");
    }

    /**
     * Parses the raw authentication challenge.
     *
     * @param rawChallenge Raw challenge.
     * @param listener     Completion listener.
     */
    public AsyncTask<?, ?, ?> parseAuthenticationChallenge(final String rawChallenge, final OnParseAuthenticationChallengeListener listener) {
        AsyncTask<Void, Void, Object> task = new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... voids) {
                AuthenticationChallenge challenge = new AuthenticationChallenge();

                if (!rawChallenge.startsWith("tiqrauth://")) {
                    return new ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_qr_code));
                }

                URL url;

                try {
                    url = new URL(rawChallenge.replaceFirst("tiqrauth://", "http://"));
                }
                catch (MalformedURLException ex) {
                    return new ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_qr_code));
                }

                String[] pathComponents = url.getPath().split("/");
                if (pathComponents.length < 3) {
                    return new ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_qr_code));
                }

                DbAdapter dbAdapter = new DbAdapter(_context);

                IdentityProvider ip = dbAdapter.getIdentityProviderByIdentifierAsObject(url.getHost());
                if (ip == null) {
                    return new ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_IDENTITY_PROVIDER, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_unknown_identity_provider));
                }

                Identity identity = null;

                if (url.getUserInfo() != null) {
                    String userInfo  = url.getUserInfo();
                    try {
                        userInfo = URLDecoder.decode(userInfo, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        // never happens...
                    }
                    identity = dbAdapter.getIdentityByIdentifierAndIdentityProviderIdentifierAsObject(userInfo, ip.getIdentifier());
                    if (identity == null) {
                        return new ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_IDENTITY, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_unknown_identity));
                    }

                } else {
                    Identity[] identities = dbAdapter.findIdentitiesByIdentityProviderIdentifierAsObjects(ip.getIdentifier());

                    if (identities == null || identities.length == 0) {
                        return new ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.NO_IDENTITIES, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_no_identities_for_identity_provider));
                    }

                    identity = identities.length == 1 ? identities[0] : null;
                }

                challenge.setIdentity(identity);
                challenge.setIdentityProvider(identity == null ? ip : dbAdapter.getIdentityProviderForIdentityId(identity.getId()));

                challenge.setSessionKey(pathComponents[1]);
                challenge.setChallenge(pathComponents[2]);

                if (pathComponents.length > 3) {
                    challenge.setServiceProviderDisplayName(pathComponents[3]);
                } else {
                    challenge.setServiceProviderDisplayName(_context.getString(R.string.unknown));
                }

                challenge.setServiceProviderIdentifier("");

                if (pathComponents.length > 4) {
                    challenge.setProtocolVersion(pathComponents[4]);
                } else {
                    // v1 qr without protocol version identifier
                    challenge.setProtocolVersion("1");
                }

                String returnURL = url.getQuery() == null || url.getQuery().length() == 0 ? null : url.getQuery();
                if (returnURL != null && returnURL.matches("^http(s)?://.*")) {
                    try {
                        challenge.setReturnURL(URLDecoder.decode(returnURL, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        // never happens...
                    }
                }

                return challenge;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (result instanceof AuthenticationChallenge) {
                    AuthenticationChallenge challenge = (AuthenticationChallenge)result;
                    listener.onParseAuthenticationChallengeSuccess(challenge);
                } else {
                    ParseAuthenticationChallengeError error = (ParseAuthenticationChallengeError)result;
                    listener.onParseAuthenticationChallengeError(error);
                }
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;

    }

    /**
     * Authenticate.
     *
     * @param challenge Challenge.
     * @param password  Password / pin.
     * @param listener  Completion listener.
     *
     * @return async task
     */
    public AsyncTask<?, ?, ?> authenticate(final AuthenticationChallenge challenge, final String password, final OnAuthenticationListener listener) {
        AsyncTask<Void, Void, AuthenticationError> task = new AsyncTask<Void, Void, AuthenticationError>() {
            @Override
            protected AuthenticationError doInBackground(Void... voids) {
                String otp = null;

                try {
                    SecretKey sessionKey = Encryption.keyFromPassword(_context, password);
                    Secret secret = Secret.secretForIdentity(challenge.getIdentity(), _context);
                    SecretKey secretKey = secret.getSecret(sessionKey);

                    OCRAProtocol ocra;
                    if (challenge.getProtocolVersion().equals("1")) {
                        ocra = new OCRAWrapper_v1();
                    } else {
                        ocra = new OCRAWrapper();
                    }

                    otp = ocra.generateOCRA(challenge.getIdentityProvider().getOCRASuite(), secretKey.getEncoded(), challenge.getChallenge(), challenge.getSessionKey());

                    // Add your dNameValuePair
                    Map<String, String> nameValuePairs = new HashMap<>();
                    nameValuePairs.put("sessionKey", challenge.getSessionKey());
                    nameValuePairs.put("userId", challenge.getIdentity().getIdentifier());
                    nameValuePairs.put("response", otp);
                    nameValuePairs.put("language", Locale.getDefault().getLanguage());

                    String notificationAddress = _notificationService.getNotificationToken();
                    if (notificationAddress != null) {
                        // communicate latest notification type and address
                        nameValuePairs.put("notificationType", "GCM");
                        nameValuePairs.put("notificationAddress", notificationAddress);
                    }

                    nameValuePairs.put("operation", "login");

                    byte[] postData = Utils.keyValueMapToByteArray(nameValuePairs);

                    URL authenticationURL = new URL(challenge.getIdentityProvider().getAuthenticationURL());
                    HttpURLConnection httpURLConnection = (HttpURLConnection)authenticationURL.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty("ACCEPT", "application/json");
                    httpURLConnection.setRequestProperty("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION);
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(postData.length));
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.getOutputStream().write(postData);

                    String response = Utils.urlConnectionResponseAsString(httpURLConnection);
                    String versionHeader = httpURLConnection.getHeaderField("X-TIQR-Protocol-Version");
                    if (versionHeader == null || versionHeader.equals("1")) {
                        // v1 protocol (ascii)
                        return _parseV1Response(response);
                    } else {
                        // v2 protocol (json)
                        return _parseV2Response(response);
                    }
                } catch (InvalidChallengeException e) {
                    return new AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge));
                } catch (InvalidKeyException e) {
                    return new AuthenticationError(Type.UNKNOWN,_context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_key));
                } catch (SecurityFeaturesException e) {
                    return new AuthenticationError(Type.UNKNOWN, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_device_incompatible_with_security_standards));
                } catch (IOException e) {
                    return new AuthenticationError(Type.CONNECTION, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_connect_error));
                }
            }

            @Override
            protected void onPostExecute(AuthenticationError error) {
                if (error == null) {
                    listener.onAuthenticationSuccess();
                } else {
                    DbAdapter db = new DbAdapter(_context); // TODO: inject
                    if (error.getType() == Type.ACCOUNT_BLOCKED) {
                        challenge.getIdentity().setBlocked(true);
                        db.updateIdentity(challenge.getIdentity());
                    } else if (error.getType() == Type.INVALID_RESPONSE) {
                        if (error.getExtras().containsKey("attemptsLeft") && error.getExtras().getInt("attemptsLeft") == 0) {
                            db.blockAllIdentities();
                        }
                    }

                    listener.onAuthenticationError(error);
                }
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
    }

    /**
     * Parse authentication response from server (v1, plain string).
     *
     * @param response authentication response
     *
     * @return Error or null on success.
     */
    private AuthenticationError _parseV1Response(String response) {
        try {
            if (response != null && response.equals("OK")) {
                return null;
            } else if (response.equals("ACCOUNT_BLOCKED")) {
                return new AuthenticationError(Type.ACCOUNT_BLOCKED, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message));
            } else if (response.equals("INVALID_CHALLENGE")) {
                return new AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message));
            } else if (response.equals("INVALID_REQUEST")) {
                return new AuthenticationError(Type.INVALID_REQUEST, _context.getString(R.string.error_auth_invalid_request_title), _context.getString(R.string.error_auth_invalid_request_message));
            } else if (response.substring(0, 17).equals("INVALID_RESPONSE:")) {
                int attemptsLeft = Integer.parseInt(response.substring(17, 18));
                Bundle extras = new Bundle();
                extras.putInt("attemptsLeft", attemptsLeft);

                String message;
                if (attemptsLeft > 1) {
                    return new AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), String.format(_context.getString(R.string.error_auth_x_attempts_left), attemptsLeft), extras);
                } else if (attemptsLeft == 1) {
                    return new AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), _context.getString(R.string.error_auth_one_attempt_left), extras);
                } else {
                    return new AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message), extras);
                }
            } else if (response.equals("INVALID_USERID")) {
                return new AuthenticationError(Type.INVALID_USER, _context.getString(R.string.error_auth_invalid_account), _context.getString(R.string.error_auth_invalid_account_message));
            } else {
                return new AuthenticationError(Type.UNKNOWN, _context.getString(R.string.unknown_error), _context.getString(R.string.error_auth_unknown_error));
            }
        } catch (NumberFormatException e) {
            return new AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message));
        } catch (Exception e) {
            return new AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message));
        }
    }

    /**
     * Parse authentication response from server (v2, json).
     *
     * @param response authentication response
     *
     * @return Error or null on success.
     */
    private AuthenticationError _parseV2Response(String response) {
        try {
            JSONObject object = new JSONObject(response);

            int responseCode = object.getInt("responseCode");

            if (responseCode == 1) {
                return null;
            } else if (responseCode == 204) {
                if (object.has("duration")) {
                    int duration = object.getInt("duration");
                    Bundle extras = new Bundle();
                    extras.putInt("duration", duration);
                    return new AuthenticationError(Type.ACCOUNT_TEMPORARY_BLOCKED, _context.getString(R.string.error_auth_account_blocked_temporary_title), _context.getString(R.string.error_auth_account_blocked_temporary_message), extras);
                } else {
                    return new AuthenticationError(Type.ACCOUNT_BLOCKED, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message));
                }
            } else if (responseCode == 203) {
                return new AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message));
            } else if (responseCode == 202) {
                return new AuthenticationError(Type.INVALID_REQUEST, _context.getString(R.string.error_auth_invalid_request_title), _context.getString(R.string.error_auth_invalid_request_message));
            } else if (responseCode == 201) {
                int attemptsLeft = object.getInt("attemptsLeft");
                Bundle extras = new Bundle();
                extras.putInt("attemptsLeft", attemptsLeft);

                String message;
                if (attemptsLeft > 1) {
                    return new AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), String.format(_context.getString(R.string.error_auth_x_attempts_left), attemptsLeft), extras);
                } else if (attemptsLeft == 1) {
                    return new AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), _context.getString(R.string.error_auth_one_attempt_left), extras);
                } else {
                    return new AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message), extras);
                }
            } else if (responseCode == 205) {
                return new AuthenticationError(Type.INVALID_USER, _context.getString(R.string.error_auth_invalid_account), _context.getString(R.string.error_auth_invalid_account_message));
            } else {
                return new AuthenticationError(Type.UNKNOWN, _context.getString(R.string.unknown_error), _context.getString(R.string.error_auth_unknown_error));
            }
        } catch (JSONException e) {
            return new AuthenticationError(Type.UNKNOWN, _context.getString(R.string.unknown_error), _context.getString(R.string.error_auth_unknown_error));
        } catch (NumberFormatException e) {
            return new AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message));
        } catch (Exception e) {
            return new AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message));
        }
    }
}
