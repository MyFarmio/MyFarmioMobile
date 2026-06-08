package com.myfarmio.app.ui.tasks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myfarmio.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment {

    private final List<TaskItem> allTasks = new ArrayList<>();
    private LinearLayout tasksContainer;
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tasksContainer = view.findViewById(R.id.ll_tasks_container);
        MaterialButton filterButton = view.findViewById(R.id.btn_tasks_filter);

        seedTasks();
        renderTasks();

        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }
    }

    private void seedTasks() {
        if (!allTasks.isEmpty()) return;
        allTasks.add(new TaskItem("Reponer stock de urea", "Stock actual por debajo del objetivo operativo.", "Juan Pérez", "09 jun 2026", "Pendiente"));
        allTasks.add(new TaskItem("Confirmar ventana de fertilización", "Validar humedad, viento y disponibilidad.", "María Gómez", "04 jun 2026", "En progreso"));
        allTasks.add(new TaskItem("Revisar lote 12", "Se detectó diferencia de humedad y cobertura.", "Nicolás Ruiz", "11 jun 2026", "En revisión"));
    }

    private void renderTasks() {
        if (tasksContainer == null) return;
        tasksContainer.removeAllViews();

        String[] states = currentFilter.equals("ALL")
            ? new String[]{"Pendiente", "En progreso", "En revisión", "Planificada", "Completada"}
            : new String[]{currentFilter};

        LayoutInflater inflater = getLayoutInflater();
        boolean any = false;

        for (String state : states) {
            List<TaskItem> filtered = new ArrayList<>();
            for (TaskItem task : allTasks) {
                if (state.equalsIgnoreCase(task.status)) {
                    filtered.add(task);
                }
            }
            if (filtered.isEmpty()) continue;
            any = true;

            TextView header = new TextView(requireContext());
            header.setText(state.toUpperCase(Locale.ROOT));
            header.setTextColor(0xFF79564B);
            header.setTextSize(11);
            header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
            header.setPadding(0, dp(18), 0, dp(6));
            header.setAllCaps(true);
            tasksContainer.addView(header);

            for (TaskItem task : filtered) {
                View card = inflater.inflate(R.layout.item_task_summary, tasksContainer, false);
                TextView tvStatus = card.findViewById(R.id.tv_task_status);
                TextView tvDue = card.findViewById(R.id.tv_task_due);
                TextView tvTitle = card.findViewById(R.id.tv_task_title);
                TextView tvDescription = card.findViewById(R.id.tv_task_description);
                TextView tvResponsible = card.findViewById(R.id.tv_task_responsible);
                MaterialButton details = card.findViewById(R.id.btn_task_details);

                tvStatus.setText(task.status.toUpperCase(Locale.ROOT));
                tvDue.setText(task.dueDate);
                tvTitle.setText(task.title);
                tvDescription.setText(task.description);
                tvResponsible.setText("Responsable: " + task.responsible);
                tvStatus.setTextColor(statusColor(task.status));

                View.OnClickListener openDetail = v -> showTaskDetail(task);
                card.setOnClickListener(openDetail);
                details.setOnClickListener(openDetail);

                tasksContainer.addView(card);
            }
        }

        if (!any) {
            TextView empty = new TextView(requireContext());
            empty.setText(getString(R.string.tasks_empty));
            empty.setTextColor(0xFF9CA3AF);
            empty.setPadding(dp(8), dp(24), dp(8), dp(8));
            tasksContainer.addView(empty);
        }
    }

    private void showFilterDialog() {
        String[] options = {
            getString(R.string.tasks_filter_all),
            getString(R.string.tasks_filter_pending),
            getString(R.string.tasks_filter_progress),
            getString(R.string.tasks_filter_review),
            "Planificada",
            getString(R.string.tasks_filter_done)
        };
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tasks_filters)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 1: currentFilter = "Pendiente"; break;
                    case 2: currentFilter = "En progreso"; break;
                    case 3: currentFilter = "En revisión"; break;
                    case 4: currentFilter = "Planificada"; break;
                    case 5: currentFilter = "Completada"; break;
                    default: currentFilter = "ALL"; break;
                }
                renderTasks();
            })
            .show();
    }

    private void showTaskDetail(TaskItem task) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_detail, null, false);
        TextView tvStatus = dialogView.findViewById(R.id.tv_detail_status);
        TextView tvTitle = dialogView.findViewById(R.id.tv_detail_title);
        TextView tvDescription = dialogView.findViewById(R.id.tv_detail_description);
        TextView tvResponsible = dialogView.findViewById(R.id.tv_detail_responsible);
        TextView tvDue = dialogView.findViewById(R.id.tv_detail_due);
        Spinner statusSpinner = dialogView.findViewById(R.id.spinner_detail_status);

        tvStatus.setText(task.status.toUpperCase(Locale.ROOT));
        tvStatus.setTextColor(statusColor(task.status));
        tvTitle.setText(task.title);
        tvDescription.setText(task.description);
        tvResponsible.setText(task.responsible);
        tvDue.setText(task.dueDate);

        String[] statuses = {"Pendiente", "Planificada", "En progreso", "En revisión", "Completada"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, statuses);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);
        int selectedIndex = Arrays.asList(statuses).indexOf(task.status);
        statusSpinner.setSelection(selectedIndex >= 0 ? selectedIndex : 0);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tasks_detail_title)
            .setView(dialogView)
            .setPositiveButton(R.string.tasks_detail_save, (dialog, which) -> {
                task.status = statuses[statusSpinner.getSelectedItemPosition()];
                renderTasks();
            })
            .setNegativeButton(R.string.tasks_detail_close, null)
            .show();
    }

    private int statusColor(String status) {
        if ("Completada".equalsIgnoreCase(status)) return 0xFF154212;
        if ("En progreso".equalsIgnoreCase(status)) return 0xFF1B6EBE;
        if ("En revisión".equalsIgnoreCase(status)) return 0xFF79564B;
        if ("Planificada".equalsIgnoreCase(status)) return 0xFF72796E;
        return 0xFFBA1A1A;
    }

    private int dp(int value) {
        return (int) (value * requireContext().getResources().getDisplayMetrics().density);
    }

    private static class TaskItem {
        String title;
        String description;
        String responsible;
        String dueDate;
        String status;

        TaskItem(String title, String description, String responsible, String dueDate, String status) {
            this.title = title;
            this.description = description;
            this.responsible = responsible;
            this.dueDate = dueDate;
            this.status = status;
        }
    }
}
