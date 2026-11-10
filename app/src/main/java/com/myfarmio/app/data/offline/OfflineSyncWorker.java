package com.myfarmio.app.data.offline;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Response;

/** Replays the per-account outbox only after Android reports a usable network. */
public final class OfflineSyncWorker extends Worker {
    public OfflineSyncWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull @Override public Result doWork() {
        SessionManager session = SessionManager.getInstance(getApplicationContext());
        String user = session.getUserId(), organization = session.getOrganizationId();
        if (user == null || user.isEmpty() || organization == null || organization.isEmpty()) return Result.success();
        if (!SupabaseConfig.isConfigured()) return Result.retry();
        OfflineStore store = OfflineStore.get(getApplicationContext());
        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);
        OfflineStore.PendingChange change;
        while ((change = store.nextPending(user, organization)) != null) {
            try {
                Response<List<Map<String, Object>>> response;
                String table = table(change.module);
                if ("create".equals(change.operation)) {
                    response = api.upsertRecord(table, "id", change.payload).execute();
                } else if ("update".equals(change.operation)) {
                    response = api.updateRecord(table, filters(organization, change.recordId), change.payload).execute();
                } else if ("delete".equals(change.operation)) {
                    Map<String, Object> deleted = new LinkedHashMap<>();
                    deleted.put("deleted_at", Instant.now().toString());
                    deleted.put("updated_by", user);
                    response = api.updateRecord(table, filters(organization, change.recordId), deleted).execute();
                } else {
                    store.markFailed(change, "Operación local desconocida.");
                    continue;
                }
                if (response.isSuccessful()) {
                    Map<String, Object> serverRow = response.body() == null || response.body().isEmpty()
                        ? change.payload : response.body().get(0);
                    store.markSynced(change, serverRow);
                } else if (response.code() == 408 || response.code() == 429 || response.code() >= 500) {
                    return Result.retry();
                } else {
                    store.markFailed(change, "Supabase respondió HTTP " + response.code() + ". Revisá permisos y datos.");
                }
            } catch (IOException networkError) {
                return Result.retry();
            } catch (RuntimeException configurationError) {
                return Result.retry();
            }
        }
        return Result.success();
    }

    private String table(String module) {
        switch (module) {
            case "fields": return "plots";
            case "livestock": return "livestock_herds";
            case "animals": return "livestock_animals";
            case "tasks": return "tasks";
            case "finance": return "finance_records";
            case "inventory": return "inventory_items";
            default: throw new IllegalArgumentException("Módulo offline desconocido.");
        }
    }
    private Map<String, String> filters(String organization, String id) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("id", "eq." + id); query.put("organization_id", "eq." + organization); query.put("deleted_at", "is.null");
        return query;
    }
}
