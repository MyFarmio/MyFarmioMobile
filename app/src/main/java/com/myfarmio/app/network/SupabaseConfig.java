package com.myfarmio.app.network;

import java.io.IOException;
import com.myfarmio.app.auth.SessionManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseConfig {

    public static final String SUPABASE_URL = com.myfarmio.app.BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_ANON_KEY = com.myfarmio.app.BuildConfig.SUPABASE_ANON_KEY;

    private static Retrofit retrofit = null;
    private static String authToken = null; // Se setea después del login
    private static SessionManager sessionManager;

    public static synchronized void setSessionManager(SessionManager manager) {
        sessionManager = manager;
    }

    public static boolean isConfigured() {
        return SUPABASE_URL != null && !SUPABASE_URL.trim().isEmpty()
            && SUPABASE_ANON_KEY != null && !SUPABASE_ANON_KEY.trim().isEmpty();
    }

    public static synchronized void setAuthToken(String token) {
        authToken = token;
        retrofit = null; // Forzar recreación con nuevo token
    }

    public static synchronized void clearAuthToken() {
        authToken = null;
        retrofit = null;
    }

    public static synchronized Retrofit getRetrofit() {
        if (retrofit == null) {
            if (!isConfigured()) throw new IllegalStateException("Configurá MYFARMIO_SUPABASE_URL y MYFARMIO_SUPABASE_ANON_KEY.");
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Never log auth headers or record payloads in release builds.
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);

            // Interceptor que agrega los headers de Supabase a CADA request
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                        .header("apikey", SUPABASE_ANON_KEY)
                        .header("Prefer", "return=representation");

                    // Si hay token de sesión, lo agregamos
                    if (authToken != null && !authToken.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + authToken);
                    }

                    return chain.proceed(requestBuilder.build());
                }
            });
            httpClient.authenticator(new SupabaseSessionAuthenticator(sessionManager));

            retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL.endsWith("/") ? SUPABASE_URL : SUPABASE_URL + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        }
        return retrofit;
    }
}
