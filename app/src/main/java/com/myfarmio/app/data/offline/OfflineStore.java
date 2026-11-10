package com.myfarmio.app.data.offline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** App-private cache and durable outbox for organization records. */
public final class OfflineStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "myfarmio_offline.db";
    private static final int DB_VERSION = 1;
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static volatile OfflineStore instance;
    private final Gson gson = new Gson();
    private final MutableLiveData<Long> revision = new MutableLiveData<>(0L);

    public static OfflineStore get(Context context) {
        if (instance == null) synchronized (OfflineStore.class) {
            if (instance == null) instance = new OfflineStore(context.getApplicationContext());
        }
        return instance;
    }

    private OfflineStore(Context context) { super(context, DB_NAME, null, DB_VERSION); }
    public MutableLiveData<Long> revision() { return revision; }
    private void notifyChanged() { Long value = revision.getValue(); revision.postValue(value == null ? 1L : value + 1L); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cached_records (user_id TEXT NOT NULL, organization_id TEXT NOT NULL, module TEXT NOT NULL, record_id TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(user_id, organization_id, module, record_id))");
        db.execSQL("CREATE TABLE cached_modules (user_id TEXT NOT NULL, organization_id TEXT NOT NULL, module TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(user_id, organization_id, module))");
        db.execSQL("CREATE TABLE offline_outbox (local_id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT NOT NULL, organization_id TEXT NOT NULL, module TEXT NOT NULL, record_id TEXT NOT NULL, operation TEXT NOT NULL, payload TEXT NOT NULL, display_payload TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending', attempts INTEGER NOT NULL DEFAULT 0, last_error TEXT, created_at INTEGER NOT NULL, UNIQUE(user_id, organization_id, module, record_id))");
        db.execSQL("CREATE INDEX offline_outbox_owner ON offline_outbox(user_id, organization_id, status, local_id)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public synchronized void replaceSnapshot(String user, String organization, String module,
                                             List<Map<String, Object>> rows) {
        if (empty(user) || empty(organization) || empty(module)) return;
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            db.delete("cached_records", "user_id=? AND organization_id=? AND module=?",
                new String[]{user, organization, module});
            long now = System.currentTimeMillis();
            if (rows != null) for (Map<String, Object> row : rows) {
                String id = text(row.get("id"));
                if (!id.isEmpty()) putCached(db, user, organization, module, id, row, now);
            }
            ContentValues snapshot = new ContentValues(); snapshot.put("user_id", user); snapshot.put("organization_id", organization);
            snapshot.put("module", module); snapshot.put("updated_at", now);
            db.insertWithOnConflict("cached_modules", null, snapshot, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        notifyChanged();
    }

    public synchronized boolean hasSnapshot(String user, String organization, String module) {
        try (Cursor cursor = getReadableDatabase().query("cached_modules", new String[]{"module"},
            "user_id=? AND organization_id=? AND module=?", new String[]{user, organization, module}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public synchronized void cacheServerRow(String user, String organization, String module, Map<String, Object> row) {
        if (empty(user) || empty(organization) || empty(module) || row == null) return;
        String id = text(row.get("id")); if (id.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        putCached(db, user, organization, module, id, row, System.currentTimeMillis());
        ContentValues snapshot = new ContentValues(); snapshot.put("user_id", user); snapshot.put("organization_id", organization);
        snapshot.put("module", module); snapshot.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict("cached_modules", null, snapshot, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void removeCachedRow(String user, String organization, String module, String id) {
        if (empty(user) || empty(organization) || empty(module) || empty(id)) return;
        getWritableDatabase().delete("cached_records", "user_id=? AND organization_id=? AND module=? AND record_id=?",
            new String[]{user, organization, module, id});
    }

    public synchronized List<Map<String, Object>> list(String user, String organization, String module) {
        if (empty(user) || empty(organization) || empty(module)) return Collections.emptyList();
        LinkedHashMap<String, Map<String, Object>> rows = new LinkedHashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query("cached_records", new String[]{"record_id", "payload"},
            "user_id=? AND organization_id=? AND module=?", new String[]{user, organization, module}, null, null, "updated_at ASC")) {
            while (cursor.moveToNext()) {
                Map<String, Object> row = decode(cursor.getString(1));
                if (row != null) rows.put(cursor.getString(0), row);
            }
        }
        try (Cursor cursor = db.query("offline_outbox", new String[]{"record_id", "operation", "display_payload", "status", "last_error"},
            "user_id=? AND organization_id=? AND module=?", new String[]{user, organization, module}, null, null, "local_id ASC")) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(0), operation = cursor.getString(1);
                if ("delete".equals(operation)) { rows.remove(id); continue; }
                Map<String, Object> row = decode(cursor.getString(2));
                if (row == null) continue;
                row.put("_sync_status", "failed".equals(cursor.getString(3)) ? "error" : "pending");
                String error = cursor.getString(4);
                if (!empty(error)) row.put("_sync_error", error);
                rows.put(id, row);
            }
        }
        return new ArrayList<>(rows.values());
    }

    public synchronized void queueSave(String user, String organization, String module, String recordId,
                                       String operation, Map<String, Object> payload,
                                       Map<String, Object> displayPayload) {
        if (empty(user) || empty(organization) || empty(module) || empty(recordId))
            throw new IllegalArgumentException("Falta identificar la cuenta, organización o registro.");
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            String effectiveOperation = operation;
            Map<String, Object> retainedPayload = new LinkedHashMap<>(payload);
            try (Cursor cursor = db.query("offline_outbox", new String[]{"operation", "payload"},
                "user_id=? AND organization_id=? AND module=? AND record_id=?",
                new String[]{user, organization, module, recordId}, null, null, null, "1")) {
                if (cursor.moveToFirst() && "create".equals(cursor.getString(0))) {
                    effectiveOperation = "create";
                    Map<String,Object> original = decode(cursor.getString(1));
                    if (original != null) for (String key : Arrays.asList("organization_id", "created_by"))
                        if (!retainedPayload.containsKey(key) && original.containsKey(key)) retainedPayload.put(key, original.get(key));
                }
            }
            ContentValues queued = new ContentValues();
            queued.put("user_id", user); queued.put("organization_id", organization); queued.put("module", module);
            queued.put("record_id", recordId); queued.put("operation", effectiveOperation);
            queued.put("payload", gson.toJson(retainedPayload)); queued.put("display_payload", gson.toJson(displayPayload));
            queued.put("status", "pending"); queued.put("attempts", 0); queued.putNull("last_error");
            queued.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict("offline_outbox", null, queued, SQLiteDatabase.CONFLICT_REPLACE);
            Map<String, Object> local = new LinkedHashMap<>(displayPayload);
            local.put("_sync_status", "pending");
            putCached(db, user, organization, module, recordId, local, System.currentTimeMillis());
            ContentValues snapshot = new ContentValues(); snapshot.put("user_id", user); snapshot.put("organization_id", organization);
            snapshot.put("module", module); snapshot.put("updated_at", System.currentTimeMillis());
            db.insertWithOnConflict("cached_modules", null, snapshot, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        notifyChanged();
    }

    public synchronized void queueDelete(String user, String organization, String module, String recordId,
                                         Map<String, Object> payload) {
        if (empty(user) || empty(organization) || empty(module) || empty(recordId))
            throw new IllegalArgumentException("Falta identificar la cuenta, organización o registro.");
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            boolean unsyncedCreate = false;
            try (Cursor cursor = db.query("offline_outbox", new String[]{"operation"},
                "user_id=? AND organization_id=? AND module=? AND record_id=?",
                new String[]{user, organization, module, recordId}, null, null, null, "1")) {
                unsyncedCreate = cursor.moveToFirst() && "create".equals(cursor.getString(0));
            }
            String where = "user_id=? AND organization_id=? AND module=? AND record_id=?";
            String[] args = {user, organization, module, recordId};
            if (unsyncedCreate) db.delete("offline_outbox", where, args);
            else {
                ContentValues queued = new ContentValues();
                queued.put("user_id", user); queued.put("organization_id", organization); queued.put("module", module);
                queued.put("record_id", recordId); queued.put("operation", "delete");
                queued.put("payload", gson.toJson(payload)); queued.put("display_payload", "{}");
                queued.put("status", "pending"); queued.put("attempts", 0); queued.putNull("last_error");
                queued.put("created_at", System.currentTimeMillis());
                db.insertWithOnConflict("offline_outbox", null, queued, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.delete("cached_records", where, args);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        notifyChanged();
    }

    @Nullable public synchronized PendingChange nextPending(String user, String organization) {
        try (Cursor cursor = getReadableDatabase().query("offline_outbox", new String[]{"local_id", "module", "record_id", "operation", "payload"},
            "user_id=? AND organization_id=? AND status='pending'", new String[]{user, organization}, null, null, "created_at ASC, local_id ASC", "1")) {
            if (!cursor.moveToFirst()) return null;
            return new PendingChange(cursor.getLong(0), user, organization, cursor.getString(1), cursor.getString(2),
                cursor.getString(3), decode(cursor.getString(4)));
        }
    }

    public synchronized void markSynced(PendingChange change, Map<String, Object> serverRow) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            db.delete("offline_outbox", "local_id=?", new String[]{String.valueOf(change.localId)});
            if ("delete".equals(change.operation)) {
                db.delete("cached_records", "user_id=? AND organization_id=? AND module=? AND record_id=?",
                    new String[]{change.userId, change.organizationId, change.module, change.recordId});
            } else if (serverRow != null) {
                putCached(db, change.userId, change.organizationId, change.module, change.recordId, serverRow, System.currentTimeMillis());
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        notifyChanged();
    }

    public synchronized void markFailed(PendingChange change, String error) {
        getWritableDatabase().execSQL("UPDATE offline_outbox SET status='failed', last_error=?, attempts=attempts+1 WHERE local_id=?",
            new Object[]{error, change.localId});
        notifyChanged();
    }

    public synchronized int pendingCount(String user, String organization) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM offline_outbox WHERE user_id=? AND organization_id=? AND status IN ('pending','failed')",
            new String[]{user, organization})) { return cursor.moveToFirst() ? cursor.getInt(0) : 0; }
    }

    public synchronized int failedCount(String user, String organization) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM offline_outbox WHERE user_id=? AND organization_id=? AND status='failed'",
            new String[]{user, organization})) { return cursor.moveToFirst() ? cursor.getInt(0) : 0; }
    }

    public synchronized void retryFailed(String user, String organization) {
        ContentValues values = new ContentValues(); values.put("status", "pending"); values.putNull("last_error");
        getWritableDatabase().update("offline_outbox", values, "user_id=? AND organization_id=? AND status='failed'", new String[]{user, organization});
        notifyChanged();
    }

    private void putCached(SQLiteDatabase db, String user, String organization, String module, String id,
                           Map<String, Object> row, long updated) {
        ContentValues values = new ContentValues(); values.put("user_id", user); values.put("organization_id", organization);
        values.put("module", module); values.put("record_id", id); values.put("payload", gson.toJson(row)); values.put("updated_at", updated);
        db.insertWithOnConflict("cached_records", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    private Map<String, Object> decode(String json) {
        try { return gson.fromJson(json, MAP_TYPE); } catch (RuntimeException error) { return null; }
    }
    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value); }

    public static final class PendingChange {
        public final long localId;
        public final String userId, organizationId, module, recordId, operation;
        public final Map<String, Object> payload;
        PendingChange(long localId, String userId, String organizationId, String module, String recordId,
                      String operation, Map<String, Object> payload) {
            this.localId = localId; this.userId = userId; this.organizationId = organizationId;
            this.module = module; this.recordId = recordId; this.operation = operation;
            this.payload = payload == null ? new LinkedHashMap<>() : payload;
        }
    }
}
