package com.myfarmio.app.ui.welcome;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.myfarmio.app.R;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View bottomArea = view.findViewById(R.id.bottom_area);
        View card;
        if (bottomArea instanceof ViewGroup && ((ViewGroup) bottomArea).getChildCount() > 0) {
            card = ((ViewGroup) bottomArea).getChildAt(0);
        } else {
            card = bottomArea;
        }

        if (card != null) {
            float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                60f,
                getResources().getDisplayMetrics()
            );
            card.setTranslationY(px);
            card.setAlpha(0f);
            card.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(500)
                .start();
        }

        View btnLogin = view.findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_welcome_to_login);
        });
    }
}
