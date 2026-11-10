package com.myfarmio.app.network;

import com.myfarmio.app.auth.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/** Refreshes Supabase sessions after a 401, off the UI thread, and persists rotated tokens securely. */
final class SupabaseSessionAuthenticator implements Authenticator {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final SessionManager session;
    private final OkHttpClient refreshClient = new OkHttpClient.Builder()
        .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    SupabaseSessionAuthenticator(SessionManager session) { this.session = session; }

    @Override public Request authenticate(Route route, Response response) throws IOException {
        if (session == null || responseCount(response) > 1
            || response.request().url().encodedPath().contains("/auth/v1/token")) return null;
        synchronized (SupabaseSessionAuthenticator.class) {
            String latestToken = session.getAccessToken();
            String usedAuthorization = response.request().header("Authorization");
            if (latestToken != null && !("Bearer " + latestToken).equals(usedAuthorization)) {
                return response.request().newBuilder().header("Authorization", "Bearer " + latestToken).build();
            }
            String refreshToken = session.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) return null;
            JSONObject payload = new JSONObject();
            try { payload.put("refresh_token", refreshToken); }
            catch (org.json.JSONException error) { return null; }
            Request refresh = new Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL.replaceAll("/$", "") + "/auth/v1/token?grant_type=refresh_token")
                .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(JSON, payload.toString()))
                .build();
            try (Response refreshed = refreshClient.newCall(refresh).execute()) {
                if (!refreshed.isSuccessful() || refreshed.body() == null) {
                    if (refreshed.code() == 400 || refreshed.code() == 401 || refreshed.code() == 403) session.clearSession();
                    return null;
                }
                JSONObject tokens = new JSONObject(refreshed.body().string());
                String access = tokens.optString("access_token", "");
                String rotatedRefresh = tokens.optString("refresh_token", "");
                if (access.isEmpty() || rotatedRefresh.isEmpty()) return null;
                session.updateAuthTokens(access, rotatedRefresh);
                return response.request().newBuilder().header("Authorization", "Bearer " + access).build();
            } catch (org.json.JSONException malformed) {
                return null;
            }
        }
    }

    private static int responseCount(Response response) {
        int count = 1;
        for (Response prior = response.priorResponse(); prior != null; prior = prior.priorResponse()) count++;
        return count;
    }
}
