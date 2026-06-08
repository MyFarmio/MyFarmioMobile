package com.myfarmio.app.auth;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.myfarmio.app.network.SupabaseConfig;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {

    private static final String PREFS_NAME = "myfarmio_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_ORG_ID = "organization_id";
    private static final String KEY_ORG_NAME = "organization_name";

    private SharedPreferences prefs;
    private static SessionManager instance;

    private SessionManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback a SharedPreferences normales si falla el cifrado
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveSession(String accessToken, String refreshToken,
                            String userId, String email, String name,
                            String orgId, String orgName) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_ORG_ID, orgId);
        editor.putString(KEY_ORG_NAME, orgName);
        editor.apply();

        // Actualizar Retrofit con el nuevo token
        SupabaseConfig.setAuthToken(accessToken);
    }

    public boolean isLoggedIn() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        return token != null && !token.isEmpty();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Usuario");
    }

    public String getOrganizationId() {
        return prefs.getString(KEY_ORG_ID, null);
    }

    public String getOrganizationName() {
        return prefs.getString(KEY_ORG_NAME, null);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
        SupabaseConfig.clearAuthToken();
    }
}
