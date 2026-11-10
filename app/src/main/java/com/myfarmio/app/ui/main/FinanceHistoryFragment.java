package com.myfarmio.app.ui.main;

import static com.myfarmio.app.ui.common.RecordText.amount;
import static com.myfarmio.app.ui.common.RecordText.dateLabel;
import static com.myfarmio.app.ui.common.RecordText.label;
import static com.myfarmio.app.ui.common.RecordText.number;
import static com.myfarmio.app.ui.common.RecordText.or;
import static com.myfarmio.app.ui.common.RecordText.value;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.data.SupabaseRecordRepository;
import com.myfarmio.app.ui.common.MobileUi;
import com.myfarmio.app.ui.common.StitchUi;
import com.myfarmio.app.ui.common.WorkspaceViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Full-page, paged finance history; all real rows come from the organization-scoped Supabase endpoint. */
public final class FinanceHistoryFragment extends Fragment {
    private View root;
    private WorkspaceViewModel workspace;
    private FinanceHistoryViewModel model;
    private String organization;
    private String filter = "";

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_finance_history, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        root = view;
        workspace = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
        SessionManager session = SessionManager.getInstance(requireContext());
        workspace.prepare(session);
        organization = session.getOrganizationId();
        model = new ViewModelProvider(this).get(FinanceHistoryViewModel.class);
        MaterialButtonToggleGroup filters = view.findViewById(R.id.history_filters);
        filters.check(R.id.history_filter_all);
        filters.addOnButtonCheckedListener((group,id,checked)->{
            if (!checked) return;
            filter = id == R.id.history_filter_income ? "income" : id == R.id.history_filter_expense ? "expense" : "";
            render();
        });
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = view.findViewById(R.id.history_swipe);
        swipe.setColorSchemeResources(R.color.stitch_primary);
        swipe.setOnRefreshListener(()-> { if (!workspace.isDemo()) model.load(organization, true); else render(); });
        view.findViewById(R.id.history_load_more).setOnClickListener(v->model.loadMore());
        model.state.observe(getViewLifecycleOwner(),state->render());
        workspace.revision.observe(getViewLifecycleOwner(),revision->render());
        if (!workspace.isDemo()) model.load(organization, false);
        else render();
    }

    private List<Map<String,Object>> rows() {
        List<Map<String,Object>> rows = new ArrayList<>();
        if (workspace.isDemo()) rows.addAll(workspace.records().list("finance"));
        else {
            FinanceHistoryViewModel.State state = model.state.getValue();
            if (state != null) for (Map<String,Object> row : state.rows) rows.add(SupabaseRecordRepository.normalizeFinanceRow(row));
        }
        rows.sort(Comparator.comparing((Map<String,Object> row)->value(row,"date"),Collections.reverseOrder())
            .thenComparing(row->value(row,"created_at"),Collections.reverseOrder())
            .thenComparing(row->value(row,"id"),Collections.reverseOrder()));
        return rows;
    }

    private void render() {
        if (root == null || model == null || workspace == null) return;
        FinanceHistoryViewModel.State state = model.state.getValue();
        boolean demo = workspace.isDemo();
        List<Map<String,Object>> all = rows();
        List<Map<String,Object>> visible = new ArrayList<>();
        for (Map<String,Object> row : all) if (filter.isEmpty() || filter.equals(value(row,"movement_type"))) visible.add(row);

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = root.findViewById(R.id.history_swipe);
        swipe.setRefreshing(!demo && state != null && state.loading && !all.isEmpty());
        root.findViewById(R.id.history_loading).setVisibility(!demo && state != null && state.loading && all.isEmpty() ? View.VISIBLE : View.GONE);
        LinearLayout list = root.findViewById(R.id.history_records);
        list.removeAllViews();
        for (Map<String,Object> row : visible) {
            View item = getLayoutInflater().inflate(R.layout.item_stitch_transaction,list,false);
            boolean income = "income".equals(value(row,"movement_type"));
            MobileUi.text(item,R.id.transaction_name,or(row,"description","Movimiento"));
            MobileUi.text(item,R.id.transaction_amount,(income?"+ ":"− ")+(value(row,"currency").isEmpty()?"":value(row,"currency")+" ")+amount(number(row,"amount")));
            ((TextView)item.findViewById(R.id.transaction_amount)).setTextColor(requireContext().getColor(income?R.color.stitch_primary:R.color.secondary));
            MobileUi.text(item,R.id.transaction_detail,dateLabel(value(row,"date")));
            MobileUi.text(item,R.id.transaction_plot,value(row,"category").isEmpty()?value(row,"counterparty"):value(row,"category"));
            StitchUi.badge(item,R.id.transaction_type,income?"INGRESO":"GASTO",!income,false);
            String status=value(row,"status");
            boolean settled="posted".equals(status)||"paid".equals(status)||"collected".equals(status);
            StitchUi.badge(item,R.id.transaction_status,settled?income?"COBRADO":"PAGADO":"overdue".equals(status)?"VENCIDO":"PENDIENTE",!settled,"overdue".equals(status));
            item.setOnClickListener(v->detail(row));
            list.addView(item);
        }

        View emptyState = root.findViewById(R.id.mobile_state);
        emptyState.setVisibility(View.GONE);
        if (!demo && state != null && state.error != null && all.isEmpty()) {
            MobileUi.state(root,"No se pudo cargar el historial",state.error,"Reintentar",()->model.load(organization,true));
        } else if (visible.isEmpty() && (demo || state != null && state.loaded)) {
            MobileUi.state(root,all.isEmpty()?"Sin movimientos":"Sin resultados",all.isEmpty()?"Todavía no hay movimientos registrados.":"No hay movimientos para este filtro.","Actualizar",()->{
                if (!demo) model.load(organization,true); else render();
            });
        } else if (!demo && state != null && state.error != null) {
            MobileUi.state(root,"No se cargó la página siguiente",state.error,"Reintentar",model::loadMore);
        }
        View more = root.findViewById(R.id.history_load_more);
        more.setVisibility(!demo && state != null && state.hasMore ? View.VISIBLE : View.GONE);
        if (more instanceof com.google.android.material.button.MaterialButton)
            ((com.google.android.material.button.MaterialButton)more).setEnabled(state == null || !state.loading);
    }

    private void detail(Map<String,Object> row) {
        MobileUi.Sheet sheet = new MobileUi.Sheet(requireContext(),or(row,"description","Movimiento"));
        String status=value(row,"status");
        sheet.detailHeader(MobileUi.detailIcon("finance"),or(row,"description","Movimiento"),label(status),MobileUi.detailStatusColor(status));
        sheet.metricRow(R.drawable.st_payments,R.string.detail_amount,(value(row,"currency").isEmpty()?"":value(row,"currency")+" ")+amount(number(row,"amount")),R.drawable.st_calendar_month,R.string.detail_date,dateLabel(value(row,"date")));
        sheet.row("Tipo","income".equals(value(row,"movement_type"))?"Ingreso":"Gasto");
        sheet.row("Importe",(value(row,"currency").isEmpty()?"":value(row,"currency")+" ")+amount(number(row,"amount")));
        sheet.row("Fecha",dateLabel(value(row,"date")));
        if (!value(row,"category").isEmpty()) sheet.row("Categoría",value(row,"category"));
        if (!value(row,"counterparty").isEmpty()) sheet.row("Contraparte",value(row,"counterparty"));
        if (!value(row,"notes").isEmpty()) sheet.row("Notas",value(row,"notes"));
        sheet.row("Estado",value(row,"status"));
        sheet.primary("Cerrar",sheet::dismiss);
        sheet.show();
    }

    @Override public void onDestroyView() { root = null; super.onDestroyView(); }
}
