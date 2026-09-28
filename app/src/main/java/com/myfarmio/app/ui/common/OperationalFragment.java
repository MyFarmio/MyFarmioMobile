package com.myfarmio.app.ui.common;

import static com.myfarmio.app.ui.common.RecordText.*;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Shared mobile collection UI. Keeps existing services, models and authentication untouched. */
public abstract class OperationalFragment extends Fragment {
    protected abstract String module();
    protected abstract String title();
    protected abstract String searchHint();
    protected abstract int screenLayout();
    private CollectionViewModel model;
    private View root;
    private RecordAdapter adapter;
    private Parcelable pendingListState;
    private String query = "", status = "", priority = "", due = "", related = "", responsible = "";
    private boolean demo;
    private WorkspaceViewModel workspace;
    private boolean animals;
    private List<Map<String, Object>> source = new ArrayList<>(), visible = new ArrayList<>();
    private MobileUi.Sheet sheet;
    private RecordEditor editor;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(screenLayout(), parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        root = view;
        if (saved != null) {
            query = saved.getString("query", ""); status = saved.getString("status", "");
            priority = saved.getString("priority", ""); due = saved.getString("due", "");
            related = saved.getString("related", ""); responsible = saved.getString("responsible", "");
            animals = saved.getBoolean("animals", false);
            pendingListState = saved.getParcelable("listState");
        }
        workspace = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
        workspace.prepare(SessionManager.getInstance(requireContext()));
        demo = workspace.isDemo();
        MobileUi.text(view, R.id.collection_title, title());
        TextInputEditText search = view.findViewById(R.id.collection_search);
        search.setHint(searchHint());
        search.setText(query);
        search.setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                MobileUi.hideKeyboard(root);
                return true;
            }
            return false;
        });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); render(); }
            public void afterTextChanged(Editable s) {}
        });
        RecyclerView list = view.findViewById(R.id.collection_list);
        androidx.recyclerview.widget.GridLayoutManager grid = new androidx.recyclerview.widget.GridLayoutManager(requireContext(),
            getResources().getConfiguration().screenWidthDp >= 700 ? 2 : 1);
        grid.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) { return position == 0 ? grid.getSpanCount() : 1; }
        });
        list.setLayoutManager(grid);
        // RecyclerView 1.1.0 has no Adapter.StateRestorationPolicy. Restore explicitly
        // after data arrives so an initially empty adapter cannot consume the position.
        list.setSaveEnabled(false);
        list.setItemAnimator(null); // Avoid motion/reordering while typing or filtering.
        adapter = new RecordAdapter();
        list.setAdapter(adapter);
        view.findViewById(R.id.collection_filter).setOnClickListener(v -> filters());
        view.findViewById(R.id.collection_options).setOnClickListener(v -> options());
        view.findViewById(R.id.collection_refresh).setOnClickListener(v -> refresh());
        ((SwipeRefreshLayout) view.findViewById(R.id.collection_swipe)).setOnRefreshListener(this::refresh);
        model = new ViewModelProvider(this).get(CollectionViewModel.class);
        model.state.observe(getViewLifecycleOwner(), state -> render());
        workspace.revision.observe(getViewLifecycleOwner(), revision -> {
            boolean changedMode = demo != workspace.isDemo();
            demo = workspace.isDemo();
            if (changedMode) { animals = false; clear(); }
            root.findViewById(R.id.collection_create).setVisibility(demo ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.collection_kind).setVisibility(demo && module().equals("livestock") ? View.VISIBLE : View.GONE);
            render();
            if (!demo) load(false);
        });
        view.findViewById(R.id.collection_create).setOnClickListener(v -> edit(new java.util.LinkedHashMap<>()));
        com.google.android.material.button.MaterialButtonToggleGroup kinds = view.findViewById(R.id.collection_kind);
        kinds.check(animals ? R.id.kind_animals : R.id.kind_herds);
        kinds.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked) { animals = id == R.id.kind_animals; clear(); }
        });
        if (!demo) load(false);
        if (saved != null && saved.containsKey("editorDraft") && demo) {
            @SuppressWarnings("unchecked") Map<String,Object> draft = (Map<String,Object>) saved.getSerializable("editorDraft");
            if (draft != null) view.post(() -> {
                if (getView() == view) { edit(draft); editor.markRestored(); }
            });
        }
    }
    private void load(boolean force) {
        model.load(module(), SessionManager.getInstance(requireContext()).getOrganizationId(), force);
    }
    private void refresh() {
        if (demo) {
            ((SwipeRefreshLayout) root.findViewById(R.id.collection_swipe)).setRefreshing(false);
            render();
        } else load(true);
    }
    private void render() {
        if (root == null || model == null || adapter == null) return;
        CollectionViewModel.State state = model.state.getValue();
        if (state == null) return;
        source = demo ? workspace.records().list(activeModule()) : state.rows;
        MobileUi.text(root, R.id.collection_create, module().equals("tasks") ? "Nueva tarea" : module().equals("fields") ? "Nuevo lote" : animals ? "Nuevo animal" : "Nuevo rodeo");
        visible = new ArrayList<>();
        for (Map<String, Object> row : source) if (accepts(row)) visible.add(row);
        boolean initial = !demo && state.loading && source.isEmpty();
        boolean noRows = !initial && visible.isEmpty();
        root.findViewById(R.id.collection_feedback).setVisibility(initial || noRows ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.mobile_skeleton).setVisibility(initial ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.mobile_state).setVisibility(noRows ? View.VISIBLE : View.GONE);
        SwipeRefreshLayout swipe = root.findViewById(R.id.collection_swipe);
        swipe.setVisibility(initial || noRows ? View.GONE : View.VISIBLE);
        swipe.setRefreshing(!demo && state.loading && !source.isEmpty());
        root.findViewById(R.id.collection_refresh).setEnabled(demo || !state.loading);
        TextView notice = root.findViewById(R.id.collection_notice);
        String message = demo ? "DEMO · Cambios solo durante esta sesión"
            : state.error != null && !source.isEmpty() ? state.error + " Se conserva la última consulta." : "";
        notice.setText(message);
        notice.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
        int filters = (status.isEmpty() ? 0 : 1) + (priority.isEmpty() ? 0 : 1)
            + (due.isEmpty() ? 0 : 1) + (related.isEmpty() ? 0 : 1) + (responsible.isEmpty() ? 0 : 1);
        MobileUi.text(root, R.id.collection_filter, filters == 0 ? "Todos · Filtrar" : "Filtros activos (" + filters + ")");
        if (noRows) {
            if (!demo && state.error != null && source.isEmpty()) {
                MobileUi.state(root, "No pudimos cargar " + title().toLowerCase(), state.error, "Reintentar", () -> load(true));
            } else if (!source.isEmpty() || !query.isEmpty() || filters > 0) {
                MobileUi.state(root, "Sin coincidencias", "Probá otra búsqueda o quitá los filtros para ver todos los registros.", "Limpiar filtros", this::clear);
            } else {
                MobileUi.state(root, "Todavía no hay registros", demo ? "Creá el primer registro para comenzar a explorar." : "No hay registros visibles para esta organización. Las altas se realizan en la web.", demo ? "Crear en demo" : "Actualizar", demo ? () -> edit(new java.util.LinkedHashMap<>()) : this::refresh);
            }
        }
        adapter.notifyDataSetChanged();
        if (pendingListState != null && !visible.isEmpty()) {
            RecyclerView list = root.findViewById(R.id.collection_list);
            RecyclerView.LayoutManager layout = list.getLayoutManager();
            if (layout != null) {
                layout.onRestoreInstanceState(pendingListState);
                pendingListState = null;
            }
        }
    }
    private boolean accepts(Map<String, Object> row) {
        if (!status.isEmpty() && !status.equals(value(row, animals ? "health_status" : "status"))) return false;
        if (!priority.isEmpty() && !priority.equals(value(row, "priority"))) return false;
        if (!related.isEmpty() && !related.equals(value(row, relationKey()))) return false;
        if (!responsible.isEmpty() && !responsible.equals(value(row, demo ? "_responsible" : "responsible_user_id"))) return false;
        if ("Hoy".equals(due) && !LocalDate.now().equals(date(value(row, "due_date")))) return false;
        if ("Vencidas".equals(due) && !overdue(row, LocalDate.now())) return false;
        return matches(row, query, "title", "name", "description", "category", "current_crop",
            "crop_stage", "zone", "next_action", "related_entity_name", "_responsible", "_location", "tag", "_herd_name")
            || (!query.isEmpty() && normalize(label(value(row, "status"))).contains(normalize(query)));
    }
    private String relationKey() { return module().equals("tasks") ? "related_entity_name" : module().equals("fields") ? "current_crop" : "category"; }
    private void clear() {
        pendingListState = null;
        query = ""; status = ""; priority = ""; due = ""; related = ""; responsible = "";
        ((TextInputEditText) root.findViewById(R.id.collection_search)).setText("");
        render();
    }
    private MobileUi.Sheet openSheet(String heading) {
        MobileUi.hideKeyboard(root);
        if (sheet != null) sheet.dismiss();
        editor = null;
        sheet = new MobileUi.Sheet(requireContext(), heading);
        return sheet;
    }
    private List<String> values(String key) {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.add("");
        for (Map<String, Object> row : source) if (!value(row, key).isEmpty()) options.add(value(row, key));
        return new ArrayList<>(options);
    }
    private Spinner selector(MobileUi.Sheet sheet, String title, List<String> values, String selected, boolean translate) {
        TextView labelView = new TextView(requireContext());
        labelView.setTextAppearance(R.style.MobileLabel); labelView.setText(title);
        labelView.setPadding(0, MobileUi.dp(requireContext(), 16), 0, MobileUi.dp(requireContext(), 4));
        sheet.body.addView(labelView);
        Spinner spinner = new Spinner(requireContext());
        spinner.setMinimumHeight(MobileUi.dp(requireContext(), 48));
        spinner.setContentDescription(title);
        List<String> labels = new ArrayList<>();
        for (String option : values) labels.add(option.isEmpty() ? "Todos" : translate ? label(option) : option);
        ArrayAdapter<String> array = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        array.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(array);
        spinner.setSelection(Math.max(0, values.indexOf(selected)));
        sheet.body.addView(spinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return spinner;
    }
    private void filters() {
        MobileUi.Sheet current = openSheet("Filtrar " + title().toLowerCase());
        List<String> statuses = values(animals ? "health_status" : "status"), relatedValues = values(relationKey());
        Spinner stateSelector = selector(current, "Estado", statuses, status, true);
        Spinner relatedSelector = selector(current, module().equals("tasks") ? "Referencia / campo" : module().equals("fields") ? "Cultivo" : "Categoría", relatedValues, related, false);
        List<String> priorities = values("priority");
        List<String> dates = Arrays.asList("", "Hoy", "Vencidas");
        Spinner prioritySelector = module().equals("tasks") ? selector(current, "Prioridad", priorities, priority, true) : null;
        Spinner dateSelector = module().equals("tasks") ? selector(current, "Vencimiento", dates, due, false) : null;
        String myId = SessionManager.getInstance(requireContext()).getUserId();
        List<String> people = new ArrayList<>(Arrays.asList(""));
        if (demo) people = values("_responsible");
        else if (myId != null && source.stream().anyMatch(row -> myId.equals(value(row, "responsible_user_id")))) people.add("Asignadas a mí");
        final List<String> personOptions = people;
        Spinner personSelector = module().equals("tasks") && people.size() > 1
            ? selector(current, "Responsable", people, demo ? responsible : responsible.isEmpty() ? "" : "Asignadas a mí", false) : null;
        current.button("Limpiar búsqueda y filtros", () -> { clear(); current.dismiss(); });
        current.primary("Mostrar resultados", () -> {
            status = statuses.get(stateSelector.getSelectedItemPosition());
            related = relatedValues.get(relatedSelector.getSelectedItemPosition());
            priority = prioritySelector == null ? "" : priorities.get(prioritySelector.getSelectedItemPosition());
            due = dateSelector == null ? "" : dates.get(dateSelector.getSelectedItemPosition());
            responsible = personSelector == null ? "" : demo ? personOptions.get(personSelector.getSelectedItemPosition())
                : personSelector.getSelectedItemPosition() == 1 ? myId : "";
            render(); current.dismiss();
        });
        current.show();
    }
    private String activeModule() { return module().equals("livestock") && animals ? "animals" : module(); }
    private void options() {
        MobileUi.Sheet current = openSheet(title());
        current.row(demo ? "Modo demostración" : "Consulta de tu organización", demo
            ? "Podés crear, editar y eliminar registros locales. Se conservan al cambiar de módulo, pero no al cerrar la sesión o el proceso."
            : "La consulta utiliza tus servicios y permisos actuales. Las escrituras siguen disponibles únicamente en la web.");
        if (!workspace.isDemoAccount()) current.button(demo ? "Volver a mi organización" : "Explorar demostración", () -> {
            current.dismiss(); workspace.setDemo(!demo);
        });
        current.primary("Actualizar vista", () -> { current.dismiss(); refresh(); });
        current.show();
    }
    private void edit(Map<String,Object> row) {
        if (!demo) return;
        MobileUi.Sheet current = openSheet((row.isEmpty() ? "Crear" : "Editar") + (activeModule().equals("tasks") ? " tarea" : activeModule().equals("fields") ? " lote" : animals ? " animal" : " rodeo"));
        editor = new RecordEditor(current, activeModule(), row, workspace.records(), () -> {
            clear();
            android.widget.Toast.makeText(requireContext(), "Guardado en la demostración", android.widget.Toast.LENGTH_SHORT).show();
        });
        current.dialog.setOnDismissListener(dialog -> editor = null);
        current.show();
    }
    private String responsible(Map<String, Object> row) {
        if (!value(row, "_responsible").isEmpty()) return value(row, "_responsible");
        String id = value(row, "responsible_user_id");
        if (id.isEmpty()) return "Sin responsable";
        SessionManager session = SessionManager.getInstance(requireContext());
        return id.equals(session.getUserId()) ? (session.getUserName() == null ? "Asignada a mí" : session.getUserName())
            : "Responsable asignado";
    }
    private String herdNote(Map<String, Object> row, String key) {
        if (key.equals("location") && !value(row, "_location").isEmpty()) return value(row, "_location");
        String raw = value(row, "notes");
        try {
            JSONObject json = new JSONObject(raw);
            if ("myfarmio.livestock_herd.v1".equals(json.optString("kind"))) return json.optString(key, "");
        } catch (Exception ignored) { }
        return key.equals("notes") ? raw : "";
    }
    private void detail(Map<String, Object> row) {
        if (demo) {
            MobileUi.Sheet current = openSheet(or(row, "title", or(row, "name", value(row,"tag"))));
            DemoRecordDetails.bind(current, activeModule(), row, workspace.records(), () -> {
                // Re-read to include checklist or comments changed while this detail was open.
                Map<String,Object> latest = workspace.records().list(activeModule()).stream()
                    .filter(item -> value(item,"id").equals(value(row,"id"))).findFirst().orElse(row);
                edit(latest);
            }, this::render);
            current.show(); return;
        }
        MobileUi.Sheet current = openSheet(or(row, module().equals("tasks") ? "title" : "name", "Detalle"));
        current.row(demo ? "Ejemplo local" : "Estado", label(value(row, "status")));
        if (module().equals("tasks")) {
            current.row("Descripción", value(row, "description"));
            current.heading("Organización del trabajo");
            current.row("Prioridad", value(row, "priority").isEmpty() ? "" : label(value(row, "priority")));
            current.row("Responsable", responsible(row));
            current.row("Inicio", dateLabel(value(row, "start_date")));
            current.row("Vencimiento", dateLabel(value(row, "due_date")));
            current.row("Categoría", value(row, "category"));
            current.row("Campo / referencia", value(row, "related_entity_name"));
            current.row("Zona y ubicación", join(value(row, "zone"), value(row, "location")));
            if (row.get("progress") != null) current.row("Avance registrado", amount(number(row, "progress")) + "%");
            current.heading("Seguimiento");
            current.row("Observaciones", value(row, "observations"));
            current.row("Checklist y comentarios", "El detalle de estos registros aún no está conectado en Android.");
            // TODO: Conectar checklist, comentarios y resolución de responsables sin alterar contratos.
            if (demo) {
                List<String> states = Arrays.asList("pending", "planned", "in_progress", "in_review", "completed");
                Spinner selection = selector(current, "Cambiar estado del ejemplo", states, value(row, "status"), true);
                current.primary("Guardar en el ejemplo", () -> { row.put("status", states.get(selection.getSelectedItemPosition())); render(); current.dismiss(); });
            }
        } else if (module().equals("fields")) {
            current.heading("Resumen productivo");
            current.row("Cultivo", value(row, "current_crop"));
            current.row("Superficie", row.get("area_hectares") == null ? "" : amount(number(row, "area_hectares")) + " ha");
            current.row("Etapa", value(row, "crop_stage"));
            current.row("Humedad", row.get("soil_moisture_percent") == null ? "" : amount(number(row, "soil_moisture_percent")) + "%");
            current.row("Rinde estimado", value(row, "estimated_yield"));
            current.row("Zona", value(row, "zone"));
            current.row("Responsable", responsible(row));
            current.row("Rotación", value(row, "rotation"));
            current.heading("Próxima acción");
            current.row("Seguimiento del lote", value(row, "next_action"));
            current.row("Observaciones", value(row, "notes"));
            current.heading("Labores, aplicaciones y cosecha");
            current.row("Sin consulta móvil disponible", "Estos registros se gestionan en la web; no se muestran proyecciones ni labores de ejemplo como datos reales.");
            // TODO: Conectar lecturas y formularios de labores, aplicaciones y cosecha cuando exista su servicio Android.
        } else {
            current.heading(value(row, "_animal").isEmpty() ? "Resumen del rodeo" : "Animal de ejemplo");
            current.row("Categoría", value(row, "category"));
            if (value(row, "_animal").isEmpty()) {
                current.row("Cantidad", row.get("quantity") == null ? "" : amount(number(row, "quantity")) + " animales");
                current.row("Peso promedio", row.get("average_weight_kg") == null ? "" : amount(number(row, "average_weight_kg")) + " kg");
            } else current.row("Peso", value(row, "_weight"));
            current.row("Ubicación", herdNote(row, "location"));
            current.row("Observaciones", herdNote(row, "notes"));
            current.heading("Animales y controles");
            current.row("Seguimiento individual", "La consulta de animales, pastoreo y actividades aún no está conectada en Android. El resumen superior corresponde únicamente a rodeos.");
            // TODO: Conectar animales, sanidad, reproducción, pastoreo y actividades a sus servicios reales.
        }
        if (!demo) current.row("Solo consulta", "Para crear, editar o completar registros, utilizá la web. No se realizan cambios desde este detalle.");
        current.show();
    }
    private String join(String first, String second) {
        return first.isEmpty() ? second : second.isEmpty() ? first : first + " · " + second;
    }
    private String summary() {
        if (module().equals("tasks")) return source.size() + " tareas";
        if (module().equals("fields")) {
            double area = 0; for (Map<String, Object> row : source) area += number(row, "area_hectares");
            return amount(area) + " ha registradas";
        }
        return source.size() + (animals ? " animales" : " rodeos");
    }
    private String summaryContext() {
        if (module().equals("tasks")) {
            int active = 0, late = 0;
            for (Map<String, Object> row : source) {
                String state = value(row, "status");
                if (!Arrays.asList("completed", "done", "cancelled").contains(state)) active++;
                if (overdue(row, LocalDate.now())) late++;
            }
            return active + " activas · " + late + " vencidas";
        }
        int alerts = 0; double animals = 0;
        for (Map<String, Object> row : source) {
            if (attention(value(row, "status"))) alerts++;
            animals += number(row, "quantity");
        }
        return module().equals("fields") ? source.size() + " lotes · " + alerts + " en seguimiento"
            : this.animals ? "Caravanas y seguimiento individual" : amount(animals) + " animales declarados · " + alerts + " rodeos en seguimiento";
    }
    private final class RecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemCount() { return visible.isEmpty() ? 0 : visible.size() + 1; }
        @Override public int getItemViewType(int position) { return position == 0 ? 0 : 1; }
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view = LayoutInflater.from(parent.getContext()).inflate(type == 0 ? R.layout.item_mobile_summary : R.layout.item_mobile_record, parent, false);
            return new RecyclerView.ViewHolder(view) {};
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View card = holder.itemView;
            if (position == 0) {
                MobileUi.text(card, R.id.summary_value, summary());
                MobileUi.text(card, R.id.summary_context, summaryContext());
                MobileUi.text(card, R.id.summary_count, visible.size() + " de " + source.size() + " registros · " + (demo ? "Demo editable" : "Solo consulta"));
                return;
            }
            Map<String, Object> row = visible.get(position - 1);
            String state = value(row, animals ? "health_status" : "status");
            boolean risk = attention(state) || (module().equals("tasks") && overdue(row, LocalDate.now()));
            TextView badge = card.findViewById(R.id.record_status);
            badge.setText(label(state));
            badge.setTextColor(requireContext().getColor(risk ? R.color.on_error_container : R.color.primary));
            badge.setBackgroundResource(risk ? R.drawable.bg_priority_high : R.drawable.bg_mobile_badge);
            MobileUi.text(card, R.id.record_title, or(row, module().equals("tasks") ? "title" : animals ? "tag" : "name", "Sin nombre"));
            String signal = "", subtitle = "", meta = "";
            ProgressBar progress = card.findViewById(R.id.record_progress);
            progress.setVisibility(View.GONE);
            if (module().equals("tasks")) {
                signal = value(row, "priority").isEmpty() ? "" : "Prioridad " + label(value(row, "priority")).toLowerCase();
                subtitle = or(row, "related_entity_name", value(row, "description"));
                meta = (overdue(row, LocalDate.now()) ? "Vencida · " : "Vence · ") + dateLabel(value(row, "due_date")) + "\n" + responsible(row);
                if (row.get("progress") != null) {
                    int amount = (int) Math.max(0, Math.min(100, number(row, "progress")));
                    progress.setVisibility(View.VISIBLE); progress.setProgress(amount);
                    progress.setContentDescription("Avance registrado: " + amount + "%");
                    meta += "\nAvance " + amount + "%";
                }
            } else if (module().equals("fields")) {
                signal = row.get("area_hectares") == null ? "" : amount(number(row, "area_hectares")) + " ha";
                subtitle = join(or(row, "current_crop", "Sin cultivo"), value(row, "crop_stage"));
                meta = or(row, "next_action", "Sin próxima acción registrada");
            } else {
                signal = animals ? (row.containsKey("weight_kg") ? amount(number(row,"weight_kg")) + " kg" : "") : row.get("quantity") == null ? "" : amount(number(row, "quantity")) + " animales";
                subtitle = join(value(row, "category"), herdNote(row, "location"));
                meta = animals ? value(row,"_herd_name") + " · " + label(value(row,"reproductive_status")) : row.get("average_weight_kg") == null
                    ? "Peso promedio sin registrar" : "Peso promedio · " + amount(number(row, "average_weight_kg")) + " kg";
            }
            MobileUi.text(card, R.id.record_signal, signal);
            MobileUi.text(card, R.id.record_subtitle, subtitle);
            card.findViewById(R.id.record_subtitle).setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);
            MobileUi.text(card, R.id.record_meta, meta);
            card.setOnClickListener(v -> detail(row));
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        captureListState();
        out.putParcelable("listState", pendingListState);
        out.putString("query", query); out.putString("status", status); out.putString("priority", priority);
        out.putString("due", due); out.putString("related", related); out.putString("responsible", responsible);
        out.putBoolean("animals", animals);
        if (editor != null && sheet != null && sheet.dialog.isShowing())
            out.putSerializable("editorDraft", new java.util.LinkedHashMap<>(editor.snapshot()));
    }
    private void captureListState() {
        if (root == null || adapter == null || adapter.getItemCount() == 0) return;
        RecyclerView list = root.findViewById(R.id.collection_list);
        RecyclerView.LayoutManager layout = list.getLayoutManager();
        if (layout != null) pendingListState = layout.onSaveInstanceState();
    }
    @Override public void onDestroyView() {
        captureListState();
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        if (root != null) ((RecyclerView) root.findViewById(R.id.collection_list)).setAdapter(null);
        root = null; adapter = null;
        super.onDestroyView();
    }
}
