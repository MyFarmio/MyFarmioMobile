package com.myfarmio.app.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseConfig {

    // Proyecto Supabase de MyFarmio
    public static final String SUPABASE_URL = "https://ledbqdwwxqfzaxfccvvo.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxlZGJxZHd3eHFmemF4ZmNjdnZvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk3NTE5NDcsImV4cCI6MjA5NTMyNzk0N30.1sA37gNTpV3SZAuTomRZX8S4zv_Dd20BA7s67raRHfw";

    private static Retrofit retrofit = null;
    private static String authToken = null; // Se setea después del login

    public static void setAuthToken(String token) {
        authToken = token;
        retrofit = null; // Forzar recreación con nuevo token
    }

    public static void clearAuthToken() {
        authToken = null;
        retrofit = null;
    }

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

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

            retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        }
        return retrofit;
    }
}
