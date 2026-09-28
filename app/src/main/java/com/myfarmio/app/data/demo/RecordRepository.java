package com.myfarmio.app.data.demo;

import java.util.List;
import java.util.Map;

/** Frontend record boundary. The current implementation is explicitly session-only demo. */
public interface RecordRepository {
    List<Map<String, Object>> list(String module);
    Map<String, Object> save(String module, Map<String, Object> record);
    void delete(String module, String id);
}
