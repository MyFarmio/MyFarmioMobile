package com.myfarmio.app.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.myfarmio.app.R;

/** Presentation-only primitives. No data mutation or permission assumptions. */
public final class MobileUi {
    private MobileUi() {}
    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
    public static void text(View root, int id, CharSequence value) {
        ((TextView) root.findViewById(id)).setText(value);
    }
    public static void enter(View view) {
        // Native equivalent of reduced motion: honor the system animator setting.
        if (ValueAnimator.areAnimatorsEnabled()) {
            view.setAlpha(0f);
            view.animate().alpha(1f).setDuration(140).start();
        }
    }
    public static void hideKeyboard(View root) {
        if (root == null) return;
        android.view.inputmethod.InputMethodManager keyboard =
            (android.view.inputmethod.InputMethodManager) root.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(root.getWindowToken(), 0);
        View focused = root.findFocus();
        if (focused != null) focused.clearFocus();
    }
    public static void state(View root, String title, String body, String action, Runnable retry) {
        View state = root.findViewById(R.id.mobile_state);
        state.setVisibility(View.VISIBLE);
        text(state, R.id.state_title, title);
        text(state, R.id.state_body, body);
        MaterialButton button = state.findViewById(R.id.state_retry);
        button.setVisibility(retry == null ? View.GONE : View.VISIBLE);
        button.setText(action);
        button.setOnClickListener(v -> { if (retry != null) retry.run(); });
    }
    public static final class Sheet {
        public final BottomSheetDialog dialog;
        public final LinearLayout body;
        public final MaterialButton action;
        private final View root;
        private Runnable close;
        private boolean editing;
        public Sheet(Context context, String title) {
            dialog = new BottomSheetDialog(context);
            root = LayoutInflater.from(context).inflate(R.layout.sheet_mobile, null, false);
            body = root.findViewById(R.id.sheet_body);
            action = root.findViewById(R.id.sheet_action);
            text(root, R.id.sheet_title, title);
            root.findViewById(R.id.sheet_close).setOnClickListener(v -> { if (close != null) close.run(); else dialog.dismiss(); });
            dialog.setContentView(root);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                v.setPadding(bars.left, dp(context, 8), bars.right, ime.bottom > 0 ? 0 : bars.bottom);
                if (root.getParent() instanceof View) {
                    View container = (View) root.getParent();
                    int fullHeight = context.getResources().getDisplayMetrics().heightPixels;
                    int available = fullHeight - bars.top - Math.max(bars.bottom, ime.bottom);
                    int target = Math.min((int) (fullHeight * 0.92f), available);
                    if (container.getLayoutParams().height != target) {
                        container.getLayoutParams().height = target;
                        container.requestLayout();
                    }
                }
                return insets;
            });
            dialog.setOnShowListener(d -> {
                View parent = (View) root.getParent();
                int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.92f);
                BottomSheetBehavior<View> sizing = BottomSheetBehavior.from(parent);
                sizing.setMaxWidth(dp(context, 640));
                parent.getLayoutParams().height = height;
                parent.requestLayout();
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(!editing);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                ViewCompat.requestApplyInsets(root);
            });
        }
        public void heading(String title) {
            TextView view = new TextView(body.getContext());
            view.setTextAppearance(R.style.MobileSection);
            view.setText(title);
            view.setPadding(0, dp(body.getContext(), 20), 0, dp(body.getContext(), 4));
            ViewCompat.setAccessibilityHeading(view, true);
            body.addView(view);
        }
        public void row(String label, String value) {
            View row = LayoutInflater.from(body.getContext()).inflate(R.layout.item_mobile_detail, body, false);
            text(row, R.id.detail_label, label);
            text(row, R.id.detail_value, value == null || value.trim().isEmpty() ? "Sin dato registrado" : value);
            body.addView(row);
        }
        public MaterialButton button(String label, Runnable onClick) {
            MaterialButton button = new MaterialButton(body.getContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setText(label);
            button.setMinHeight(dp(body.getContext(), 48));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(body.getContext(), 8);
            body.addView(button, params);
            button.setOnClickListener(v -> onClick.run());
            return button;
        }
        public void danger(String label, Runnable onClick) {
            MaterialButton button = button(label, onClick);
            button.setTextColor(body.getContext().getColor(R.color.error));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(body.getContext().getColor(R.color.error)));
        }
        public void primary(String label, Runnable onClick) {
            action.setText(label);
            action.setVisibility(View.VISIBLE);
            action.setOnClickListener(v -> onClick.run());
        }
        public void guardClose(Runnable requestClose) {
            close = requestClose; editing = true;
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnKeyListener((d, key, event) -> {
                if (key == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                    requestClose.run(); return true;
                }
                return key == android.view.KeyEvent.KEYCODE_BACK;
            });
        }
        public void show() { dialog.show(); }
        public void dismiss() { dialog.dismiss(); }
    }
}
