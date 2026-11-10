package com.myfarmio.app.ui.auth;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {
    public static final String AUTH_REDIRECT_URI = "com.myfarmio.app://auth/callback";

    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    public final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);

    public LoginViewModel(@NonNull Application application) { super(application); }

    public void login(String email, String password, Context context) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        loginSuccess.setValue(false);
        if (!SupabaseConfig.isConfigured()) {
            fail("Falta configurar la URL de Supabase y la clave anon para esta app.");
            return;
        }
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        api.login(new SupabaseApiService.LoginRequest(email, password)).enqueue(new Callback<SupabaseApiService.LoginResponse>() {
            @Override public void onResponse(Call<SupabaseApiService.LoginResponse> call, Response<SupabaseApiService.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    acceptSession(response.body(), email, context);
                } else {
                    isLoading.postValue(false);
                    errorMessage.postValue(authError(response.errorBody(), "El correo o la contraseña no son correctos."));
                }
            }
            @Override public void onFailure(Call<SupabaseApiService.LoginResponse> call, Throwable error) {
                fail("Sin conexión. Verificá tu red.");
            }
        });
    }

    public Uri startGoogleOAuth(Context context) throws Exception {
        if (!SupabaseConfig.isConfigured()) throw new IllegalStateException("Falta configurar Supabase.");
        byte[] random = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(random);
        String verifier = base64Url(random);
        String challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        SessionManager.getInstance(context).savePendingOAuth(verifier);
        isLoading.setValue(true);
        errorMessage.setValue(null);
        loginSuccess.setValue(false);
        return Uri.parse(SupabaseConfig.SUPABASE_URL).buildUpon()
            .appendPath("auth").appendPath("v1").appendPath("authorize")
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", AUTH_REDIRECT_URI)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .build();
    }

    public void handleGoogleCallback(Uri callback, Context context) {
        SessionManager session = SessionManager.getInstance(context);
        String verifier = session.getPendingOAuthVerifier();
        session.clearPendingOAuth();
        String error = callback == null ? null : callback.getQueryParameter("error");
        if ("access_denied".equals(error)) {
            isLoading.postValue(false);
            errorMessage.postValue("Cancelaste el inicio con Google.");
            return;
        }
        if (callback == null || verifier == null || verifier.isEmpty()) {
            fail("No se pudo validar el retorno de Google. Volvé a intentarlo.");
            return;
        }
        String code = callback.getQueryParameter("code");
        if (code == null || code.isEmpty()) {
            fail("No se completó el inicio con Google. Volvé a intentarlo.");
            return;
        }
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        api.exchangeOAuthCode(new SupabaseApiService.OAuthCodeExchange(code, verifier))
            .enqueue(new Callback<SupabaseApiService.LoginResponse>() {
                @Override public void onResponse(Call<SupabaseApiService.LoginResponse> call, Response<SupabaseApiService.LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) acceptSession(response.body(), null, context);
                    else {
                        isLoading.postValue(false);
                        errorMessage.postValue(authError(response.errorBody(), "No se pudo completar el inicio con Google."));
                    }
                }
                @Override public void onFailure(Call<SupabaseApiService.LoginResponse> call, Throwable error) {
                    fail("No se pudo conectar con Supabase. Volvé a intentarlo.");
                }
            });
    }

    public void cancelGoogleIfPending(Context context) {
        SessionManager session = SessionManager.getInstance(context);
        if (session.getPendingOAuthVerifier() != null) {
            session.clearPendingOAuth();
            isLoading.postValue(false);
        }
    }

    private void acceptSession(SupabaseApiService.LoginResponse body, String fallbackEmail, Context context) {
        String accessToken = body.accessToken;
        String refreshToken = body.refreshToken;
        String userId = body.user == null ? null : body.user.id;
        String email = body.user == null || body.user.email == null ? fallbackEmail : body.user.email;
        String name = "Usuario";
        if (body.user != null && body.user.userMetadata != null) {
            String candidate = body.user.userMetadata.fullName != null ? body.user.userMetadata.fullName : body.user.userMetadata.name;
            if (candidate != null && !candidate.trim().isEmpty()) name = candidate;
        }
        if (accessToken == null || refreshToken == null || userId == null || userId.isEmpty()) {
            fail("No pudimos validar la sesión recibida.");
            return;
        }
        final String resolvedName = name;
        SessionManager session = SessionManager.getInstance(context);
        session.saveSession(accessToken, refreshToken, userId, email, resolvedName, null, null);
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        api.getMemberships("eq." + userId, "eq.active", "organization_id", "created_at.asc")
            .enqueue(new Callback<List<Map<String, Object>>>() {
                @Override public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    List<Map<String, Object>> memberships = response.body();
                    if (!response.isSuccessful() || memberships == null || memberships.isEmpty()) {
                        session.clearSession();
                        fail("Tu usuario no tiene una organización activa asignada.");
                        return;
                    }
                    Object organization = memberships.get(0).get("organization_id");
                    if (organization == null || String.valueOf(organization).isEmpty()) {
                        session.clearSession();
                        fail("No pudimos resolver la organización de esta cuenta.");
                        return;
                    }
                    session.saveSession(accessToken, refreshToken, userId, email, resolvedName, String.valueOf(organization), null);
                    isLoading.postValue(false);
                    loginSuccess.postValue(true);
                }
                @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable error) {
                    session.clearSession();
                    fail("No pudimos cargar tu organización. Revisá la conexión e intentá de nuevo.");
                }
            });
    }

    private String authError(ResponseBody body, String fallback) {
        String message = fallback;
        try (ResponseBody responseBody = body) {
            String text = responseBody == null ? null : responseBody.string();
            if (text != null && !text.isEmpty()) {
                JSONObject json = new JSONObject(text);
                String candidate = json.optString("error_description", json.optString("msg", json.optString("message", "")));
                if (!candidate.isEmpty()) message = candidate;
            }
        } catch (Exception ignored) { }
        String normalized = message.toLowerCase();
        if (normalized.contains("invalid login credentials") || normalized.contains("invalid_credentials")) return "El correo o la contraseña no son correctos.";
        if (normalized.contains("email not confirmed")) return "Confirmá tu correo antes de ingresar.";
        return message;
    }

    private void fail(String message) { isLoading.postValue(false); errorMessage.postValue(message); }
    private static String base64Url(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
