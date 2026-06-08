package com.myfarmio.app.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;

public class SplashFragment extends Fragment {

    private static final long DELAY_MS = 800;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean loggedIn = SessionManager.getInstance(requireContext()).isLoggedIn();

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            NavController navController = Navigation.findNavController(view);
            if (loggedIn) {
                navController.navigate(R.id.action_splash_to_main);
            } else {
                navController.navigate(R.id.action_splash_to_welcome);
            }
        }, DELAY_MS);
    }
}
