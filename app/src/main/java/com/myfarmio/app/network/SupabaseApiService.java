package com.myfarmio.app.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApiService {

    // Endpoint para login con password (Supabase auth)
    @Headers({"Content-Type: application/json"})
    @POST("auth/v1/token?grant_type=password")
    Call<LoginResponse> login(@Body LoginRequest request);

    // REST endpoints (Supabase REST)
    @GET("rest/v1/plots")
    Call<List<Map<String, Object>>> getPlots(@Query(value = "organization_id", encoded = true) String orgEq,
                                              @Query(value = "deleted_at", encoded = true) String deletedAt,
                                              @Query(value = "select", encoded = true) String select);

    @GET("rest/v1/tasks")
    Call<List<Map<String, Object>>> getTasks(@Query(value = "organization_id", encoded = true) String orgEq,
                                              @Query(value = "deleted_at", encoded = true) String deletedAt,
                                              @Query(value = "select", encoded = true) String select);

    @GET("rest/v1/livestock_herds")
    Call<List<Map<String, Object>>> getHerds(@Query(value = "organization_id", encoded = true) String orgEq,
                                              @Query(value = "deleted_at", encoded = true) String deletedAt,
                                              @Query(value = "select", encoded = true) String select);

    @GET("rest/v1/finance_records")
    Call<List<Map<String, Object>>> getFinance(@Query(value = "organization_id", encoded = true) String orgEq,
                                               @Query(value = "deleted_at", encoded = true) String deletedAt,
                                               @Query(value = "select", encoded = true) String select);

    @GET("rest/v1/calendar_events")
    Call<List<Map<String, Object>>> getCalendar(@Query(value = "organization_id", encoded = true) String orgEq,
                                                @Query(value = "select", encoded = true) String select,
                                                @Query(value = "order", encoded = true) String order,
                                                @Query(value = "limit", encoded = true) Integer limit);

    class LoginRequest {
        @SerializedName("email")
        public String email;
        @SerializedName("password")
        public String password;
        @SerializedName("grant_type")
        public String grantType = "password";

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    class LoginResponse {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("refresh_token")
        public String refreshToken;
        @SerializedName("expires_in")
        public int expiresIn;
        @SerializedName("token_type")
        public String tokenType;
        @SerializedName("user")
        public User user;
    }

    class User {
        @SerializedName("id")
        public String id;
        @SerializedName("email")
        public String email;
        @SerializedName("user_metadata")
        public UserMetadata userMetadata;
    }

    class UserMetadata {
        @SerializedName("full_name")
        public String fullName;
        @SerializedName("name")
        public String name;
    }
}
