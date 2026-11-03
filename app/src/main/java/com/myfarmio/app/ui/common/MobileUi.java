package com.myfarmio.app.ui.common;

import android.animation.ValueAnimator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
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
            view.setScaleX(.97f); view.setScaleY(.97f);
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        }
    }
    public static int detailIcon(String module) {
        switch(module) {
            case "fields": return R.drawable.st_grass;
            case "livestock": case "animals": return R.drawable.st_pets;
            case "finance": return R.drawable.st_payments;
            case "inventory": return R.drawable.st_inventory_2;
            default: return R.drawable.st_task_alt;
        }
    }
    public static int detailStatusColor(String status) {
        if (status==null) return R.color.primary_container;
        switch(status) {
            case "completed": case "done": case "paid": case "collected": case "healthy": case "optimal": return R.color.status_completed;
            case "cancelled": case "overdue": case "critical": case "sick": case "issue": case "problem": return R.color.error;
            case "in_progress": case "planned": return R.color.status_in_progress;
            case "pending": case "inactive": return R.color.status_pending;
            default: return R.color.primary_container;
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
        public void detailHeader(int icon, String name, String status, int statusColor) {
            root.findViewById(R.id.sheet_title).setVisibility(View.GONE);
            View header=LayoutInflater.from(body.getContext()).inflate(R.layout.item_mobile_detail_header,body,false);
            ImageView image=header.findViewById(R.id.detail_header_icon);
            image.setImageResource(icon);
            image.setImageTintList(ColorStateList.valueOf(body.getContext().getColor(R.color.primary)));
            TextView title=header.findViewById(R.id.detail_header_name);
            title.setText(name);
            ViewCompat.setAccessibilityHeading(title,true);
            Chip chip=header.findViewById(R.id.detail_header_status);
            if(status==null||status.trim().isEmpty()) chip.setVisibility(View.GONE);
            else {
                chip.setVisibility(View.VISIBLE); chip.setText(status);
                chip.setChipBackgroundColor(ColorStateList.valueOf(body.getContext().getColor(statusColor)));
                chip.setTextColor(body.getContext().getColor(R.color.on_primary));
            }
            body.addView(header,0);
        }
        public void metricRow(int iconOne,String labelOne,String valueOne,int iconTwo,String labelTwo,String valueTwo) {
            LinearLayout row=new LinearLayout(body.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin=dp(body.getContext(),8);
            body.addView(row,rowParams);
            addMetric(row,iconOne,labelOne,valueOne);
            if(labelTwo!=null) addMetric(row,iconTwo,labelTwo,valueTwo);
        }
        public void metricRow(int iconOne,int labelOne,String valueOne,int iconTwo,int labelTwo,String valueTwo) {
            metricRow(iconOne,body.getContext().getString(labelOne),valueOne,iconTwo,body.getContext().getString(labelTwo),valueTwo);
        }
        private void addMetric(LinearLayout row,int icon,String label,String value) {
            View metric=LayoutInflater.from(body.getContext()).inflate(R.layout.item_mobile_detail_metric,row,false);
            ImageView image=metric.findViewById(R.id.detail_metric_icon);
            image.setImageResource(icon); image.setImageTintList(ColorStateList.valueOf(body.getContext().getColor(R.color.secondary)));
            text(metric,R.id.detail_metric_label,label);
            text(metric,R.id.detail_metric_value,value==null||value.trim().isEmpty()?body.getContext().getString(R.string.detail_unrecorded):value);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
            params.setMarginEnd(dp(body.getContext(),6));
            row.addView(metric,params);
        }
        public void row(String label, String value) {
            View row = LayoutInflater.from(body.getContext()).inflate(R.layout.item_mobile_detail, body, false);
            text(row, R.id.detail_label, label);
            text(row, R.id.detail_value, value == null || value.trim().isEmpty() ? body.getContext().getString(R.string.detail_unrecorded) : value);
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
        public void show() {
            dialog.show();
            if (ValueAnimator.areAnimatorsEnabled()) {
                root.setAlpha(0f); root.setScaleX(.985f); root.setScaleY(.985f);
                AnimatorSet enter=new AnimatorSet();
                enter.playTogether(ObjectAnimator.ofFloat(root,View.ALPHA,1f),ObjectAnimator.ofFloat(root,View.SCALE_X,1f),ObjectAnimator.ofFloat(root,View.SCALE_Y,1f));
                enter.setDuration(180); enter.start();
            }
        }
        public void dismiss() { dialog.dismiss(); }
    }
}
