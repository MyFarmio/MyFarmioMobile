package com.myfarmio.app.ui.auth;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {

    public static final String DEMO_EMAIL = "demo@myfarmio.com";
    public static final String DEMO_PASSWORD = "MyFarmio2026!";

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    public MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);

    public LoginViewModel(@NonNull Application application) {
        super(application);
    }

    public void login(String email, String password, Context context) {
        isLoading.postValue(true);
        errorMessage.postValue(null);
        loginSuccess.postValue(false);

        if (DEMO_EMAIL.equalsIgnoreCase(email) && DEMO_PASSWORD.equals(password)) {
            SessionManager.getInstance(context).saveSession(
                "demo-access-token",
                "demo-refresh-token",
                "demo-user-id",
                DEMO_EMAIL,
                "Demo User",
                null,
                null
            );
            isLoading.postValue(false);
            loginSuccess.postValue(true);
            return;
        }

        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        Call<SupabaseApiService.LoginResponse> call = api.login(new SupabaseApiService.LoginRequest(email, password));
        call.enqueue(new Callback<SupabaseApiService.LoginResponse>() {
            @Override
            public void onResponse(Call<SupabaseApiService.LoginResponse> call, Response<SupabaseApiService.LoginResponse> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    SupabaseApiService.LoginResponse body = response.body();
                    String accessToken = body.accessToken;
                    String refreshToken = body.refreshToken;
                    String userId = body.user != null ? body.user.id : null;
                    String emailResp = body.user != null ? body.user.email : email;
                    String fullName = null;
                    if (body.user != null && body.user.userMetadata != null) {
                        fullName = body.user.userMetadata.fullName != null ? body.user.userMetadata.fullName : body.user.userMetadata.name;
                    }

                    SessionManager.getInstance(context).saveSession(
                        accessToken,
                        refreshToken,
                        userId,
                        emailResp,
                        fullName != null ? fullName : "Usuario",
                        null,
                        null
                    );
                    loginSuccess.postValue(true);
                } else {
                    String errMsg = "Error desconocido.";
                    try (ResponseBody rb = response.errorBody()) {
                        String errorBody = rb != null ? rb.string() : null;
                        if (errorBody != null && !errorBody.isEmpty()) {
                            JSONObject json = new JSONObject(errorBody);
                            if (json.has("error_description")) {
                                errMsg = json.optString("error_description");
                            } else if (json.has("msg")) {
                                errMsg = json.optString("msg");
                            } else if (json.has("error")) {
                                errMsg = json.optString("error");
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    String normalized = errMsg != null ? errMsg.toLowerCase() : "";
                    if ("invalid login credentials".equalsIgnoreCase(normalized) || normalized.contains("invalid")) {
                        errMsg = "El correo o la contraseña no son correctos.";
                    } else if (normalized.contains("email not confirmed") || "email not confirmed".equalsIgnoreCase(normalized)) {
                        errMsg = "Confirmá tu correo antes de ingresar.";
                    }

                    errorMessage.postValue(errMsg);
                }
            }

            @Override
            public void onFailure(Call<SupabaseApiService.LoginResponse> call, Throwable t) {
                isLoading.postValue(false);
                errorMessage.postValue("Sin conexión. Verificá tu red.");
            }
        });
    }
}
