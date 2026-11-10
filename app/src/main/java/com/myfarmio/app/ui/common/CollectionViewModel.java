package com.myfarmio.app.ui.common;

import androidx.lifecycle.MutableLiveData;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.myfarmio.app.data.offline.OfflineStore;
import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Retains read-only UI state through rotation, using only existing API contracts. */
public class CollectionViewModel extends AndroidViewModel {
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
    private String userId;
    private String currentModule;
    public CollectionViewModel(@NonNull Application application) { super(application); }
    public void load(String module, String currentUserId, String orgId, boolean force) {
        State current = state.getValue();
        if (current == null) current = new State(new ArrayList<>(), false, false, null);
        boolean changed = !java.util.Objects.equals(orgId, organization) || !java.util.Objects.equals(currentUserId, userId)
            || !java.util.Objects.equals(module, currentModule);
        if (changed && pending != null) pending.cancel();
        if (!changed && current.loading) return;
        if (orgId == null || orgId.trim().isEmpty()) {
            state.setValue(new State(new ArrayList<>(), false, false,
                "No hay una organización activa en esta sesión. Volvé a iniciar sesión para consultar tus datos."));
            return;
        }
        if (!SupabaseConfig.isConfigured()) {
            state.setValue(new State(new ArrayList<>(), false, false,
                "Falta configurar la URL y la clave anon del proyecto Supabase."));
            return;
        }
        if (changed) {
            OfflineStore store = OfflineStore.get(getApplication());
            List<Map<String, Object>> cached = store.list(currentUserId, orgId, module);
            current = new State(cached, false, store.hasSnapshot(currentUserId, orgId, module), null);
            state.setValue(current);
            userId = currentUserId;
            organization = orgId;
            currentModule = module;
        }
        if (current.loaded && !force && !changed) return;
        final State previous = current;
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        switch (module) {
            case "tasks": pending = api.getTasks("eq." + orgId, "is.null", "*"); break;
            case "fields": pending = api.getPlots("eq." + orgId, "is.null", "*"); break;
            case "animals": pending = api.getAnimals("eq." + orgId, "is.null", "*"); break;
            case "finance": pending = api.getFinance("eq." + orgId, "is.null", "*", "date.desc,created_at.desc,id.desc"); break;
            case "inventory": pending = api.getInventory("eq." + orgId, "is.null", "*"); break;
            default: pending = api.getHerds("eq." + orgId, "is.null", "*"); break;
        }
        state.setValue(new State(previous.rows, true, previous.loaded, null));
        pending.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null) {
                    OfflineStore store = OfflineStore.get(getApplication());
                    store.replaceSnapshot(currentUserId, orgId, module, response.body());
                    state.setValue(new State(store.list(currentUserId, orgId, module), false, true, null));
                } else {
                    boolean denied = response.code() == 401 || response.code() == 403;
                    List<Map<String, Object>> cached = denied ? new ArrayList<>() : OfflineStore.get(getApplication()).list(currentUserId, orgId, module);
                    boolean hasCache = !denied && OfflineStore.get(getApplication()).hasSnapshot(currentUserId, orgId, module);
                    state.setValue(new State(cached, false,
                        hasCache, denied
                        ? "No se pudo autorizar la consulta. Revisá tu sesión y los permisos de la organización."
                        : "No pudimos actualizar este módulo (HTTP " + response.code() + "). Se conserva la última información guardada."));
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable error) {
                if (!call.isCanceled()) {
                    List<Map<String, Object>> cached = OfflineStore.get(getApplication()).list(currentUserId, orgId, module);
                    boolean hasCache = OfflineStore.get(getApplication()).hasSnapshot(currentUserId, orgId, module);
                    state.setValue(new State(cached, false, hasCache,
                        hasCache ? "Sin conexión. Se muestra la última información guardada; los cambios pendientes se sincronizarán al volver internet."
                            : "Sin conexión y todavía no hay datos guardados en este dispositivo."));
                }
            }
        });
    }
    @Override protected void onCleared() { if (pending != null) pending.cancel(); }
}
