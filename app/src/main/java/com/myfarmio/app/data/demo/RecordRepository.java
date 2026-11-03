package com.myfarmio.app.data.demo;

import java.util.List;
import java.util.Map;

/** Frontend record boundary. The current implementation is explicitly session-only demo. */
public interface RecordRepository {
    interface Result { void complete(Map<String, Object> record, String error); }
    interface DeleteResult { void complete(String error); }
    List<Map<String, Object>> list(String module);
    Map<String, Object> save(String module, Map<String, Object> record);
    void delete(String module, String id);

    default void saveAsync(String module, Map<String, Object> record, Result result) {
        try { result.complete(save(module, record), null); }
        catch (RuntimeException error) { result.complete(null, error.getMessage()); }
    }
    default void deleteAsync(String module, String id, DeleteResult result) {
        try { delete(module, id); result.complete(null); }
        catch (RuntimeException error) { result.complete(error.getMessage()); }
    }
}
