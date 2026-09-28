package com.myfarmio.app.ui.common;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Retains read-only UI state through rotation, using only existing API contracts. */
public class CollectionViewModel extends ViewModel {
    public static final class State {
        public final List<Map<String, Object>> rows;
        public final boolean loading, loaded;
        public final String error;
        public State(List<Map<String, Object>> rows, boolean loading, boolean loaded, String error) {
            this.rows = rows; this.loading = loading; this.loaded = loaded; this.error = error;
        }
    }
    public final MutableLiveData<State> state = new MutableLiveData<>(new State(new ArrayList<>(), false, false, null));
    private Call<List<Map<String, Object>>> pending;
    private String organization;
    public void load(String module, String orgId, boolean force) {
        State current = state.getValue();
        if (current == null || current.loading) return;
        if (orgId == null || orgId.trim().isEmpty()) {
            state.setValue(new State(new ArrayList<>(), false, false,
                "No hay una organización activa en esta sesión. Volvé a iniciar sesión para consultar tus datos."));
            return;
        }
        if (!orgId.equals(organization)) {
            current = new State(new ArrayList<>(), false, false, null);
            organization = orgId;
        }
        if (current.loaded && !force) return;
        final State previous = current;
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        switch (module) {
            case "tasks": pending = api.getTasks("eq." + orgId, "is.null", "*"); break;
            case "fields": pending = api.getPlots("eq." + orgId, "is.null", "*"); break;
            default: pending = api.getHerds("eq." + orgId, "is.null", "*"); break;
        }
        state.setValue(new State(previous.rows, true, previous.loaded, null));
        pending.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    state.setValue(new State(new ArrayList<>(response.body()), false, true, null));
                } else {
                    boolean denied = response.code() == 401 || response.code() == 403;
                    state.setValue(new State(denied ? new ArrayList<>() : previous.rows, false,
                        !denied && previous.loaded, denied
                        ? "No se pudo autorizar la consulta. Revisá tu sesión y los permisos de la organización."
                        : "No pudimos cargar este módulo (HTTP " + response.code() + "). Probá nuevamente."));
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable error) {
                if (!call.isCanceled()) state.setValue(new State(previous.rows, false, previous.loaded,
                    "No pudimos actualizar los datos. Revisá la conexión y reintentá."));
            }
        });
    }
    @Override protected void onCleared() { if (pending != null) pending.cancel(); }
}

