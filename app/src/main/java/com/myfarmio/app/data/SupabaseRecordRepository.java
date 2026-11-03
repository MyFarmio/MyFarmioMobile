package com.myfarmio.app.data;

import android.os.Handler;
import android.os.Looper;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.data.demo.RecordRepository;
import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;
import org.json.JSONObject;
import java.time.Instant;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** CRUD against the same Supabase tables used by the web app. */
public final class SupabaseRecordRepository implements RecordRepository {
    private final SessionManager session;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, List<Map<String, Object>>> lookups = new HashMap<>();

    public SupabaseRecordRepository(SessionManager session) { this.session = session; }

    public void seed(String module, List<Map<String, Object>> rows) { lookups.put(module, rows == null ? Collections.emptyList() : rows); }
    @Override public List<Map<String, Object>> list(String module) { return lookups.getOrDefault(module, Collections.emptyList()); }
    @Override public Map<String, Object> save(String module, Map<String, Object> record) {
        throw new UnsupportedOperationException("Las escrituras remotas son asíncronas.");
    }
    @Override public void delete(String module, String id) {
        throw new UnsupportedOperationException("Las escrituras remotas son asíncronas.");
    }

    private String table(String module) {
        switch (module) {
            case "fields": return "plots";
            case "livestock": return "livestock_herds";
            case "animals": return "livestock_animals";
            case "tasks": return "tasks";
            case "finance": return "finance_records";
            case "inventory": return "inventory_items";
            default: throw new IllegalArgumentException("Este módulo no admite edición desde mobile.");
        }
    }

    private Map<String, Object> clean(String module, Map<String, Object> source, boolean create) {
        Set<String> allowed = new HashSet<>(Arrays.asList(
            "name", "farm_id", "plot_type", "area_hectares", "current_crop", "status", "crop_stage",
            "soil_moisture_percent", "estimated_yield", "zone", "rotation", "next_action", "notes",
            "title", "description", "module_key", "category", "priority", "responsible_user_id",
            "start_date", "due_date", "location", "related_entity_type", "related_entity_id",
            "related_entity_name", "progress", "observations", "completed_at", "herd_id", "tag",
            "birth_date", "weight_kg", "health_status", "reproductive_status", "average_weight_kg", "quantity"));
        allowed.addAll(Arrays.asList("movement_type", "amount", "date", "due_date", "reference_code",
            "current_quantity", "target_quantity", "reorder_point", "expiry_date", "lot", "unit",
            "description", "status", "module_key"));
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (allowed.contains(entry.getKey()) && !entry.getKey().startsWith("_")) out.put(entry.getKey(), entry.getValue());
        }
        if ("finance".equals(module)) {
            String movement = text(source.get("movement_type"));
            String category = text(source.get("category"));
            String financeModule = text(source.get("finance_module"));
            String paymentMethod = text(source.get("payment_method"));
            String counterparty = text(source.get("counterparty"));
            String notes = text(source.get("notes"));
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("kind", "myfarmio.finance_record.v1");
            metadata.put("notes", notes);
            metadata.put("type", "expense".equals(movement) ? "expense" : "income");
            metadata.put("category", category.isEmpty() ? "otros" : category);
            metadata.put("module", financeModule.isEmpty() ? "general" : financeModule);
            if (!paymentMethod.isEmpty()) metadata.put("paymentMethod", paymentMethod);
            if (!counterparty.isEmpty()) metadata.put("counterparty", counterparty);
            out.put("notes", new JSONObject(metadata).toString());
            out.put("module_key", financeModuleKey(financeModule));
            out.remove("category"); out.remove("finance_module");
            out.remove("payment_method"); out.remove("counterparty");
        }
        if (create) {
            out.put("organization_id", session.getOrganizationId());
            out.put("created_by", session.getUserId());
        }
        out.put("updated_by", session.getUserId());
        if ("tasks".equals(module)) {
            out.put("module_key", "tasks");
            if (out.containsKey("related_entity_id")) out.put("related_entity_type", "plot");
        }
        if ("finance".equals(module) && !out.containsKey("module_key")) out.put("module_key", "finance");
        // Stock balance changes belong in input_movements so the web and mobile share audit history.
        if ("inventory".equals(module)) {
            out.remove("quantity"); out.remove("capacity"); out.remove("expiry");
            if (!create) out.remove("current_quantity");
        }
        return out;
    }

    /** Converts the web app's JSON metadata envelope to the fields used by the mobile editor. */
    public static Map<String, Object> normalizeFinanceRow(Map<String, Object> raw) {
        Map<String, Object> row = new LinkedHashMap<>(raw);
        String notes = text(raw.get("notes"));
        try {
            JSONObject metadata = new JSONObject(notes);
            if ("myfarmio.finance_record.v1".equals(metadata.optString("kind"))) {
                row.put("notes", metadata.optString("notes", ""));
                row.put("category", metadata.optString("category", "otros"));
                row.put("finance_module", metadata.optString("module", "general"));
                row.put("counterparty", metadata.optString("counterparty", ""));
                row.put("payment_method", metadata.optString("paymentMethod", ""));
            } else row.put("notes", notes);
        } catch (Exception ignored) { row.put("notes", notes); }
        if (!row.containsKey("category")) row.put("category", "otros");
        if (!row.containsKey("finance_module")) row.put("finance_module", financeModuleFromKey(text(raw.get("module_key"))));
        if (!row.containsKey("payment_method")) row.put("payment_method", "");
        if (!row.containsKey("counterparty")) row.put("counterparty", "");
        return row;
    }

    private static String financeModuleKey(String value) {
        switch (value) {
            case "agricultura": return "fields";
            case "ganaderia": return "livestock";
            case "inventario": return "inventory";
            default: return "finance";
        }
    }

    private static String financeModuleFromKey(String value) {
        switch (value) {
            case "fields": return "agricultura";
            case "livestock": return "ganaderia";
            case "inventory": return "inventario";
            default: return "general";
        }
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }

    @Override public void saveAsync(String module, Map<String, Object> record, Result result) {
        if (!SupabaseConfig.isConfigured()) { main.post(() -> result.complete(null, "Falta configurar el proyecto Supabase.")); return; }
        String org = session.getOrganizationId();
        if (org == null || org.trim().isEmpty()) { main.post(() -> result.complete(null, "No hay una organización activa en esta sesión.")); return; }
        String table = table(module);
        Object rawId = record.get("id");
        String id = rawId == null ? "" : String.valueOf(rawId);
        boolean create = id.trim().isEmpty();
        Map<String, Object> payload = clean(module, record, create);
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        Call<List<Map<String, Object>>> call = create
            ? api.createRecord(table, payload)
            : api.updateRecord(table, filters(org, id), payload);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    main.post(() -> result.complete(response.body().get(0), null));
                } else { String message = error(response.code()); main.post(() -> result.complete(null, message)); }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable error) {
                main.post(() -> result.complete(null, "No se pudo guardar. Revisá la conexión e intentá de nuevo."));
            }
        });
    }

    @Override public void deleteAsync(String module, String id, DeleteResult result) {
        if (!SupabaseConfig.isConfigured()) { main.post(() -> result.complete("Falta configurar el proyecto Supabase.")); return; }
        String org = session.getOrganizationId();
        if (org == null || org.trim().isEmpty()) { main.post(() -> result.complete("No hay una organización activa en esta sesión.")); return; }
        Map<String, Object> deleted = new HashMap<>();
        deleted.put("deleted_at", Instant.now().toString());
        deleted.put("updated_by", session.getUserId());
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        Call<List<Map<String, Object>>> call = api.updateRecord(table(module), filters(org, id), deleted);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) main.post(() -> result.complete(null));
                else { String message = response.isSuccessful() ? "El registro ya no existe o no está disponible para tu organización." : error(response.code()); main.post(() -> result.complete(message)); }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable error) {
                main.post(() -> result.complete("No se pudo eliminar. Revisá la conexión e intentá de nuevo."));
            }
        });
    }

    private Map<String, String> filters(String org, String id) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("id", "eq." + id);
        query.put("organization_id", "eq." + org);
        query.put("deleted_at", "is.null");
        return query;
    }
    private String error(int code) {
        if (code == 401 || code == 403) return "No tenés permiso para modificar este registro. Revisá tu sesión.";
        if (code == 404) return "El registro ya no está disponible.";
        return "No se pudo guardar el registro (HTTP " + code + "). Revisá los datos e intentá de nuevo.";
    }
}
