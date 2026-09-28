package com.myfarmio.app.ui.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private SwipeRefreshLayout swipeRefresh; // will point to dynamically created SwipeRefreshLayout or the placeholder frame
    private FrameLayout progressOverlay;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressOverlay = view.findViewById(R.id.progress_overlay);

        swipeRefresh.setOnRefreshListener(() -> {
            String orgId = SessionManager.getInstance(requireContext()).getOrganizationId();
            if (orgId != null && !orgId.isEmpty()) {
                viewModel.loadDashboard(orgId);
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });

        TextView tvDashboardTitle = view.findViewById(R.id.tv_dashboard_title);
        TextView tvAlertPlots = view.findViewById(R.id.tv_alert_plots);
        TextView tvAlertPlotsDetail = view.findViewById(R.id.tv_alert_plots_detail);
        TextView tvCriticalTasks = view.findViewById(R.id.tv_critical_tasks);
        TextView tvCriticalTasksDetail = view.findViewById(R.id.tv_critical_tasks_detail);
        TextView tvLivestock = view.findViewById(R.id.tv_livestock);
        TextView tvLivestockDetail = view.findViewById(R.id.tv_livestock_detail);
        TextView tvFinance = view.findViewById(R.id.tv_finance);
        TextView tvFinanceDetail = view.findViewById(R.id.tv_finance_detail);

        LinearLayout llAlerts = view.findViewById(R.id.ll_alerts_container);
        LinearLayout llCropPulse = view.findViewById(R.id.ll_crop_pulse_container);
        LinearLayout llAgenda = view.findViewById(R.id.ll_agenda_container);

        TextView tvVerTareas = view.findViewById(R.id.tv_ver_tareas);

        // Session info
        SessionManager session = SessionManager.getInstance(requireContext());
        String orgId = session.getOrganizationId();
        String userName = session.getUserName();
        if (TextUtils.isEmpty(userName)) userName = "Usuario";
        tvDashboardTitle.setText(String.format(Locale.getDefault(), "Estado general de %s", userName));

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Observers
        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = loading != null && loading;
            setSwipeRefreshing(isLoading);
            progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.state.observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;

            // Metrics
            tvAlertPlots.setText(String.valueOf(s.alertPlots));
            tvAlertPlotsDetail.setText(String.format(Locale.getDefault(), "%d lotes / %d ha", s.totalPlots, (long) s.totalHectares));

            tvCriticalTasks.setText(String.valueOf(s.criticalTasks));
            tvCriticalTasksDetail.setText(String.format(Locale.getDefault(), "%d vencen hoy", s.dueTodayTasks));

            tvLivestock.setText(String.valueOf(s.livestockHerds));
            tvLivestockDetail.setText(String.format(Locale.getDefault(), "%d animales / %d rodeos", s.livestockAnimals, s.livestockHerds));

            tvFinance.setText(formatCurrency(s.financeBalance));
            tvFinanceDetail.setText(String.format(Locale.getDefault(), "%d registros pendientes", s.financePending));

            // Alerts
            llAlerts.removeAllViews();
            if (s.alerts == null || s.alerts.isEmpty()) {
                addEmptyMessage(llAlerts);
            } else {
                LayoutInflater inflater = getLayoutInflater();
                int count = 0;
                for (DashboardViewModel.AlertItem a : s.alerts) {
                    if (count >= 10) break; // safety cap
                    View item = inflater.inflate(R.layout.item_alert, llAlerts, false);
                    TextView tvTitle = item.findViewById(R.id.tv_alert_title);
                    TextView tvDetail = item.findViewById(R.id.tv_alert_detail);
                    TextView tvPriority = item.findViewById(R.id.tv_alert_priority);
                    tvTitle.setText(a.title);
                    tvDetail.setText(a.detail);
                    tvPriority.setText(a.priority != null ? a.priority.toUpperCase(Locale.ROOT) : "");
                    if ("Alta".equalsIgnoreCase(a.priority)) {
                        tvPriority.setBackgroundResource(R.drawable.bg_priority_high);
                        tvPriority.setTextColor(0xFF93000A);
                    } else if ("Media".equalsIgnoreCase(a.priority)) {
                        tvPriority.setBackgroundResource(R.drawable.bg_priority_medium);
                        tvPriority.setTextColor(0xFF795200);
                    } else {
                        tvPriority.setBackgroundResource(R.drawable.bg_priority_medium);
                        tvPriority.setTextColor(0xFF795200);
                    }
                    llAlerts.addView(item);
                    count++;
                }
            }

            // Crop pulse
            llCropPulse.removeAllViews();
            if (s.cropPulse == null || s.cropPulse.isEmpty()) {
                addEmptyMessage(llCropPulse);
            } else {
                LayoutInflater inflater = getLayoutInflater();
                for (DashboardViewModel.CropPulseItem c : s.cropPulse) {
                    View item = inflater.inflate(R.layout.item_crop_pulse, llCropPulse, false);
                    TextView tvTitle = item.findViewById(R.id.tv_crop_title);
                    TextView tvDetail = item.findViewById(R.id.tv_crop_detail);
                    TextView tvPercent = item.findViewById(R.id.tv_crop_percent);
                    ProgressBar pb = item.findViewById(R.id.pb_crop);
                    tvTitle.setText(c.title);
                    tvDetail.setText(c.detail);
                    tvPercent.setText(String.format(Locale.getDefault(), "%d%%", c.valuePercent));
                    try { pb.setProgress(c.valuePercent); } catch (Exception ignored) {}
                    llCropPulse.addView(item);
                }
            }

            // Agenda
            llAgenda.removeAllViews();
            if (s.agenda == null || s.agenda.isEmpty()) {
                addEmptyMessage(llAgenda);
            } else {
                LayoutInflater inflater = getLayoutInflater();
                for (DashboardViewModel.AgendaItem a : s.agenda) {
                    View item = inflater.inflate(R.layout.item_agenda, llAgenda, false);
                    TextView tvTime = item.findViewById(R.id.tv_agenda_time);
                    TextView tvTitle = item.findViewById(R.id.tv_agenda_title);
                    TextView tvRef = item.findViewById(R.id.tv_agenda_reference);
                    tvTime.setText(a.time != null ? a.time : "");
                    tvTitle.setText(a.title != null ? a.title : "");
                    tvRef.setText(a.reference != null ? a.reference : "");
                    llAgenda.addView(item);
                }
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg)) {
                // show as toast or snackbar; for now set overlay to gone
                setSwipeRefreshing(false);
                progressOverlay.setVisibility(View.GONE);
            }
        });

        // Pull to refresh: set listener via reflexión
        setSwipeRefreshListener(() -> viewModel.loadDashboard(orgId));

        // Ver tareas click -> navigate to MainFragment with bundle to select tasks tab
        tvVerTareas.setOnClickListener(v -> {
            NavController nav = Navigation.findNavController(view);
            nav.navigate(R.id.nav_tareas);
        });

        if (orgId != null && !orgId.isEmpty()) {
            viewModel.loadDashboard(orgId);
        }
    }

    private void addEmptyMessage(LinearLayout container) {
        TextView tv = new TextView(requireContext());
        tv.setText("Sin datos disponibles.");
        tv.setTextColor(0xFF9CA3AF);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        container.addView(tv);
    }

    private String formatCurrency(double v) {
        double abs = Math.abs(v);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        String formatted = df.format(Math.round(abs));
        if (v < 0) return "-US$ " + formatted;
        return "US$ " + formatted;
    }

    // Reflection helpers to interact with SwipeRefreshLayout if available at runtime
    private void setSwipeRefreshing(boolean refreshing) {
        if (swipeRefresh == null) return;
        try {
            Method m = swipeRefresh.getClass().getMethod("setRefreshing", boolean.class);
            m.invoke(swipeRefresh, refreshing);
        } catch (Exception ignored) {
        }
    }

    private void setSwipeRefreshListener(Runnable onRefresh) {
        if (swipeRefresh == null) return;
        try {
            ClassLoader cl = swipeRefresh.getClass().getClassLoader();
            Class<?> listenerInterface = cl.loadClass("androidx.swiperefreshlayout.widget.SwipeRefreshLayout$OnRefreshListener");

            Object proxy = Proxy.newProxyInstance(cl, new Class<?>[]{listenerInterface}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // onRefresh()
                    onRefresh.run();
                    return null;
                }
            });

            Method setListener = swipeRefresh.getClass().getMethod("setOnRefreshListener", listenerInterface);
            setListener.invoke(swipeRefresh, proxy);
        } catch (Exception ignored) {
        }
    }}


