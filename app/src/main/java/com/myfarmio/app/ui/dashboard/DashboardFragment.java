package com.myfarmio.app.ui.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.ui.common.MobileUi;
import com.myfarmio.app.ui.common.RecordText;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private DashboardViewModel viewModel;
    private SwipeRefreshLayout swipeRefresh;
    // XML has a container here, never cast this ID to ProgressBar.
    private FrameLayout progressOverlay;
    private MobileUi.Sheet sheet;
    private View root;
    private com.myfarmio.app.ui.common.WorkspaceViewModel workspace;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        root = view;
        workspace = new ViewModelProvider(requireActivity()).get(com.myfarmio.app.ui.common.WorkspaceViewModel.class);
        workspace.prepare(SessionManager.getInstance(requireContext()));
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressOverlay = view.findViewById(R.id.progress_overlay);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        view.findViewById(R.id.dashboard_content).setVisibility(viewModel.state.getValue() == null ? View.GONE : View.VISIBLE);
        MobileUi.text(view, R.id.tv_dashboard_date, LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "AR"))));
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::reload);
        view.findViewById(R.id.tv_ver_tareas).setOnClickListener(v -> navigate(R.id.nav_tareas));
        view.findViewById(R.id.card_tasks).setOnClickListener(v -> navigate(R.id.nav_tareas));
        view.findViewById(R.id.card_fields).setOnClickListener(v -> navigate(R.id.nav_campos));
        view.findViewById(R.id.dashboard_fields).setOnClickListener(v -> navigate(R.id.nav_campos));
        view.findViewById(R.id.card_livestock).setOnClickListener(v -> navigate(R.id.nav_ganado));
        view.findViewById(R.id.card_finance).setOnClickListener(v -> navigate(R.id.nav_finanzas));
        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            if (workspace.isDemo()) return;
            boolean busy = Boolean.TRUE.equals(loading);
            boolean initial = busy && viewModel.state.getValue() == null;
            progressOverlay.setVisibility(initial ? View.VISIBLE : View.GONE);
            swipeRefresh.setImportantForAccessibility(initial ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            swipeRefresh.setRefreshing(busy && !initial);
        });
        viewModel.state.observe(getViewLifecycleOwner(), state -> {
            if (state != null && !workspace.isDemo()) render(state);
        });
        viewModel.error.observe(getViewLifecycleOwner(), message -> {
            if (!workspace.isDemo() && !TextUtils.isEmpty(message)) {
                progressOverlay.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                MobileUi.state(view, "Resumen no disponible", "No pudimos cargar el resumen. Revisá la organización de tu sesión.", "Reintentar", this::reload);
            }
        });
        workspace.revision.observe(getViewLifecycleOwner(), revision -> {
            MobileUi.text(root, R.id.dashboard_source, workspace.isDemo() ? "DEMO · Resumen de tus cambios locales" : "DATOS DE TU ORGANIZACIÓN");
            MobileUi.text(root, R.id.dashboard_agenda_title, workspace.isDemo() ? "Próximos vencimientos" : "Agenda registrada");
            if (workspace.isDemo()) reload();
            else if (viewModel.state.getValue() != null) render(viewModel.state.getValue());
            else reload();
        });
    }
    private void reload() {
        if (workspace.isDemo()) {
            progressOverlay.setVisibility(View.GONE); swipeRefresh.setRefreshing(false);
            swipeRefresh.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            render(DemoDashboard.summarize(workspace.records())); return;
        }
        String org = SessionManager.getInstance(requireContext()).getOrganizationId();
        if (TextUtils.isEmpty(org)) {
            swipeRefresh.setRefreshing(false);
            root.findViewById(R.id.dashboard_content).setVisibility(View.GONE);
            MobileUi.state(root, "Falta una organización activa", "Revisá tu perfil o volvé a iniciar sesión para cargar el resumen.", null, null);
            return;
        }
        if (!Boolean.TRUE.equals(viewModel.isLoading.getValue())) viewModel.loadDashboard(org);
    }
    private void navigate(int route) {
        NavController navigation = Navigation.findNavController(requireView());
        NavOptions options = new NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
            .setPopUpTo(navigation.getGraph().getStartDestinationId(), false, true).build();
        navigation.navigate(route, null, options);
    }
    private void render(DashboardViewModel.DashboardState state) {
        root.findViewById(R.id.dashboard_content).setVisibility(View.VISIBLE);
        boolean plots = !state.unavailableSections.contains("Campos");
        boolean tasks = !state.unavailableSections.contains("Tareas");
        boolean herds = !state.unavailableSections.contains("Ganado");
        boolean finance = !state.unavailableSections.contains("Finanzas");
        root.findViewById(R.id.mobile_state).setVisibility(View.GONE);
        if (!state.unavailableSections.isEmpty()) {
            MobileUi.state(root, "Resumen parcial", "No se pudo consultar: " + TextUtils.join(", ", state.unavailableSections)
                + ". Un guion indica un dato no disponible.", "Reintentar", this::reload);
        }
        MobileUi.text(root, R.id.tv_alert_plots, plots ? String.valueOf(state.alertPlots) : "—");
        MobileUi.text(root, R.id.tv_alert_plots_detail, plots ? state.totalPlots + " lotes · " + RecordText.amount(state.totalHectares) + " ha" : "Consulta no disponible");
        MobileUi.text(root, R.id.tv_critical_tasks, tasks ? String.valueOf(state.criticalTasks) : "—");
        MobileUi.text(root, R.id.tv_critical_tasks_detail, tasks ? state.dueTodayTasks + " vencen hoy" : "Consulta no disponible");
        MobileUi.text(root, R.id.tv_livestock, herds ? String.valueOf(state.livestockHerds) : "—");
        MobileUi.text(root, R.id.tv_livestock_detail, herds ? workspace.isDemo() ? state.livestockAnimals + " animales · " + state.livestockCritical + " rodeos en seguimiento" : "Revisar rodeos y seguimiento" : "Consulta no disponible");
        // Existing summary has no currency field. Do not invent a USD denomination.
        MobileUi.text(root, R.id.tv_finance, finance && !workspace.isDemo() ? RecordText.amount(state.financeBalance) : "—");
        MobileUi.text(root, R.id.tv_finance_detail, workspace.isDemo() ? "Sin movimientos demo" : finance ? state.financePending + " pendientes · moneda no informada" : "Consulta no disponible");
        LinearLayout alerts = root.findViewById(R.id.ll_alerts_container);
        alerts.removeAllViews();
        if (state.alerts.isEmpty()) empty(alerts, tasks && plots ? "No hay alertas en las consultas de tareas y lotes." : "Las alertas están incompletas. Reintentá la consulta.");
        int count = 0;
        for (DashboardViewModel.AlertItem alert : state.alerts) {
            if (count++ >= 10) break;
            View card = getLayoutInflater().inflate(R.layout.item_alert, alerts, false);
            boolean field = alert.title != null && alert.title.startsWith("Lote ");
            MobileUi.text(card, R.id.tv_alert_title, field && !workspace.isDemo() ? "Lote que requiere atención" : alert.title);
            // The current summary exposes IDs, not record names; avoid showing raw UUIDs.
            MobileUi.text(card, R.id.tv_alert_detail, field || workspace.isDemo() ? alert.detail : "Abrir tareas para revisar el registro");
            MobileUi.text(card, R.id.tv_alert_priority, "Prioridad " + (alert.priority == null ? "sin informar" : alert.priority.toLowerCase()));
            card.setOnClickListener(v -> navigate(field ? R.id.nav_campos : R.id.nav_tareas));
            alerts.addView(card);
        }
        LinearLayout pulse = root.findViewById(R.id.ll_crop_pulse_container);
        pulse.removeAllViews();
        if (state.cropPulse.isEmpty()) empty(pulse, plots ? "No hay cultivos visibles en tus lotes." : "No pudimos consultar tus campos.");
        for (DashboardViewModel.CropPulseItem crop : state.cropPulse) {
            View card = getLayoutInflater().inflate(R.layout.item_crop_pulse, pulse, false);
            MobileUi.text(card, R.id.tv_crop_title, crop.title);
            MobileUi.text(card, R.id.tv_crop_detail, TextUtils.isEmpty(crop.detail) ? "Etapa sin registrar" : crop.detail);
            // valuePercent is a placeholder in the existing ViewModel, not measured progress.
            pulse.addView(card);
        }
        LinearLayout agenda = root.findViewById(R.id.ll_agenda_container);
        agenda.removeAllViews();
        if (state.agenda.isEmpty()) empty(agenda, state.unavailableSections.contains("Agenda")
            ? "No pudimos consultar la agenda." : "No hay eventos en la consulta actual.");
        for (DashboardViewModel.AgendaItem event : state.agenda) {
            View card = getLayoutInflater().inflate(R.layout.item_agenda, agenda, false);
            MobileUi.text(card, R.id.tv_agenda_title, event.title);
            MobileUi.text(card, R.id.tv_agenda_time, formatEventTime(event.time));
            MobileUi.text(card, R.id.tv_agenda_reference, event.reference);
            agenda.addView(card);
        }
        // TODO: Conectar clima y detalle de eventos a sus rutas y servicios móviles reales.
    }
    private String formatEventTime(String raw) {
        if (raw == null || raw.isEmpty()) return "Sin fecha";
        try {
            return java.time.OffsetDateTime.parse(raw).atZoneSameInstant(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM · HH:mm", new Locale("es", "AR")));
        } catch (Exception ignored) { return RecordText.dateLabel(raw); }
    }
    private void empty(LinearLayout parent, String message) {
        TextView view = new TextView(requireContext());
        view.setTextAppearance(R.style.MobileBody);
        view.setText(message);
        view.setPadding(0, MobileUi.dp(requireContext(), 8), 0, MobileUi.dp(requireContext(), 8));
        parent.addView(view);
    }
    @Override public void onDestroyView() {
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        root = null; swipeRefresh = null; progressOverlay = null;
        super.onDestroyView();
    }
}


