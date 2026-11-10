package com.myfarmio.app.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Paged, organization-scoped finance history ordered by operation date and creation time. */
public final class FinanceHistoryViewModel extends ViewModel {
    private static final int PAGE_SIZE = 50;
    public static final class State {
        public final List<Map<String,Object>> rows;
        public final boolean loading, loaded, hasMore;
        public final String error;
        State(List<Map<String,Object>> rows, boolean loading, boolean loaded, boolean hasMore, String error) {
            this.rows = rows; this.loading = loading; this.loaded = loaded; this.hasMore = hasMore; this.error = error;
        }
    }
    public final MutableLiveData<State> state = new MutableLiveData<>(new State(new ArrayList<>(), false, false, false, null));
    private Call<List<Map<String,Object>>> pending;
    private String organizationId;
    private int offset;

    public void load(String organization, boolean reset) {
        State previous = state.getValue();
        if (previous == null) previous = new State(new ArrayList<>(), false, false, false, null);
        boolean changed = !Objects.equals(organizationId, organization);
        if (changed && pending != null) pending.cancel();
        if (changed) { organizationId = organization; offset = 0; reset = true; }
        if (!reset && (previous.loading || (previous.loaded && !previous.hasMore))) return;
        if (reset && pending != null) pending.cancel();
        if (organization == null || organization.trim().isEmpty()) {
            state.setValue(new State(new ArrayList<>(), false, false, false, "No hay una organización activa en esta sesión."));
            return;
        }
        if (!SupabaseConfig.isConfigured()) {
            state.setValue(new State(new ArrayList<>(), false, false, false, "Falta configurar la URL y la clave anon de Supabase."));
            return;
        }
        List<Map<String,Object>> retained = reset ? new ArrayList<>() : new ArrayList<>(previous.rows);
        final boolean previousHasMore = previous.hasMore;
        if (reset) offset = 0;
        state.setValue(new State(retained, true, !retained.isEmpty(), previous.hasMore, null));
        Map<String,String> query = new LinkedHashMap<>();
        query.put("organization_id", "eq." + organization);
        query.put("deleted_at", "is.null");
        query.put("select", "*");
        query.put("order", "date.desc,created_at.desc,id.desc");
        query.put("limit", String.valueOf(PAGE_SIZE));
        query.put("offset", String.valueOf(offset));
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        pending = api.listRecords("finance_records", query);
        final String requestOrganization = organization;
        pending.enqueue(new Callback<List<Map<String,Object>>>() {
            @Override public void onResponse(Call<List<Map<String,Object>>> call, Response<List<Map<String,Object>>> response) {
                if (call.isCanceled() || !Objects.equals(requestOrganization, organizationId)) return;
                if (!response.isSuccessful() || response.body() == null) {
                    String message = response.code() == 401 || response.code() == 403
                        ? "No se pudo autorizar el historial. Revisá tu sesión y los permisos de la organización."
                        : "No se pudo cargar el historial (HTTP " + response.code() + ").";
                    state.setValue(new State(retained, false, !retained.isEmpty(), previousHasMore, message));
                    return;
                }
                List<Map<String,Object>> merged = new ArrayList<>(retained);
                Set<String> ids = new HashSet<>();
                for (Map<String,Object> row : retained) if (row.get("id") != null) ids.add(String.valueOf(row.get("id")));
                for (Map<String,Object> row : response.body()) {
                    Object id = row.get("id");
                    if (id == null || ids.add(String.valueOf(id))) merged.add(row);
                }
                offset += response.body().size();
                state.setValue(new State(merged, false, true, response.body().size() == PAGE_SIZE, null));
            }
            @Override public void onFailure(Call<List<Map<String,Object>>> call, Throwable error) {
                if (!call.isCanceled() && Objects.equals(requestOrganization, organizationId))
                    state.setValue(new State(retained, false, !retained.isEmpty(), previousHasMore, "No se pudo actualizar el historial. Revisá la conexión y reintentá."));
            }
        });
    }

    public void loadMore() {
        if (organizationId != null) load(organizationId, false);
    }

    @Override protected void onCleared() { if (pending != null) pending.cancel(); }
}
