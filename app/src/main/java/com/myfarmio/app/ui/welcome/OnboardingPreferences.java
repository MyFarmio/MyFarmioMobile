package com.myfarmio.app.ui.welcome;
import android.content.Context;
/** Isolated frontend preference, unrelated to the authenticated session. */
public final class OnboardingPreferences {
    private OnboardingPreferences() {}
    public static boolean completed(Context context) { return context.getSharedPreferences("myfarmio_ui",Context.MODE_PRIVATE).getBoolean("onboarding_completed",false); }
    public static void complete(Context context) { context.getSharedPreferences("myfarmio_ui",Context.MODE_PRIVATE).edit().putBoolean("onboarding_completed",true).apply(); }
}
