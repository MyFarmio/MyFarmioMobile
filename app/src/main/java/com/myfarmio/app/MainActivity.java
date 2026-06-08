package com.myfarmio.app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.network.SupabaseConfig;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }
}