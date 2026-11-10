package com.myfarmio.app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.lifecycle.ViewModelProvider;

import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.network.SupabaseConfig;
import com.myfarmio.app.ui.auth.LoginViewModel;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen splash=androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        splash.setOnExitAnimationListener(provider -> {
            android.view.View splashView=provider.getView();
            splashView.animate().alpha(0f).scaleX(.96f).scaleY(.96f).setDuration(180L)
                .withEndAction(provider::remove).start();
        });
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializar la sesión y restaurar token si existe
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            SupabaseConfig.setAuthToken(sessionManager.getAccessToken());
        }

        // Configurar Navigation (buscar id dinámicamente para evitar referencias estáticas a R)
        int navHostId = getResources().getIdentifier("nav_host_fragment", "id", getPackageName());
        if (navHostId != 0) {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(navHostId);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, keyboard.bottom));
            android.view.View navigation = findViewById(R.id.bottom_nav);
            if (navigation != null) navigation.setVisibility(insets.isVisible(WindowInsetsCompat.Type.ime()) || Boolean.TRUE.equals(navigation.getTag(R.id.nav_perfil))
                ? android.view.View.GONE : android.view.View.VISIBLE);
            // The activity owns the safe area; do not pad the bottom navigation a second time.
            return WindowInsetsCompat.CONSUMED;
        });
        findViewById(R.id.main).post(this::handleAuthCallback);
    }

    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAuthCallback();
    }

    private void handleAuthCallback() {
        android.net.Uri callback = getIntent() == null ? null : getIntent().getData();
        if (callback == null || !"com.myfarmio.app".equals(callback.getScheme())
            || !"auth".equals(callback.getHost()) || !"/callback".equals(callback.getPath())) return;
        setIntent(new android.content.Intent(this, MainActivity.class));
        new ViewModelProvider(this).get(LoginViewModel.class).handleGoogleCallback(callback, this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }
}
