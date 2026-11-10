package com.myfarmio.app.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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

import com.airbnb.lottie.LottieAnimationView;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;

public class SplashFragment extends Fragment {

    private static final long SPLASH_FALLBACK_MS = 1_750L;
    private static final long REDUCED_MOTION_DELAY_MS = 350L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigationHandled;
    private LottieAnimationView splashAnimation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean loggedIn = SessionManager.getInstance(requireContext()).isLoggedIn();
        splashAnimation = view.findViewById(R.id.splash_animation);
        if (ValueAnimator.areAnimatorsEnabled()) {
            splashAnimation.enableMergePathsForKitKatAndAbove(true);
            splashAnimation.setRepeatCount(0);
            splashAnimation.setSpeed(1.25f);
            splashAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    openNextScreen(view, loggedIn);
                }
            });
            splashAnimation.playAnimation();
            handler.postDelayed(() -> openNextScreen(view, loggedIn), SPLASH_FALLBACK_MS);
        } else {
            handler.postDelayed(() -> openNextScreen(view, loggedIn), REDUCED_MOTION_DELAY_MS);
        }
    }

    private void openNextScreen(@NonNull View view, boolean loggedIn) {
        if (navigationHandled || getView() != view || !isAdded()) return;
        navigationHandled = true;
        NavController navController = Navigation.findNavController(view);
        navController.navigate(loggedIn ? R.id.action_splash_to_main : R.id.action_splash_to_login);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        if (splashAnimation != null) {
            splashAnimation.cancelAnimation();
            splashAnimation.removeAllAnimatorListeners();
            splashAnimation = null;
        }
        super.onDestroyView();
    }
}
