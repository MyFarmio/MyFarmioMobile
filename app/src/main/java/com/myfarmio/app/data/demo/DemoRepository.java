package com.myfarmio.app.data.demo;

import java.util.*;

/** In-memory demo only. Copies prevent forms from mutating records before Save. */
public final class DemoRepository implements RecordRepository {
    private final Map<String, List<Map<String, Object>>> records = DemoFixtures.create();
    private final Runnable changed;
    public DemoRepository(Runnable changed) { this.changed = changed; }
    @SuppressWarnings("unchecked")
    public static <T> T copy(T value) {
        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            ((Map<String, Object>) value).forEach((key, item) -> result.put(key, copy(item)));
            return (T) result;
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) result.add(copy(item));
            return (T) result;
        }
        return value;
    }
    private String text(Map<String, Object> row, String key) { return String.valueOf(row.getOrDefault(key, "")); }
    private Map<String, Object> find(String module, String id) {
        for (Map<String, Object> row : records.get(module)) if (id.equals(text(row,"id"))) return row;
        return null;
    }
    @Override public List<Map<String, Object>> list(String module) {
        List<Map<String, Object>> result = copy(records.getOrDefault(module, Collections.emptyList()));
        for (Map<String, Object> row : result) {
            if ("tasks".equals(module)) {
                Map<String, Object> plot = find("fields", text(row,"related_entity_id"));
                row.put("related_entity_name", plot == null ? "" : plot.get("name"));
            }
            if ("animals".equals(module)) {
                Map<String, Object> herd = find("livestock", text(row,"herd_id"));
                row.put("_herd_name", herd == null ? "" : herd.get("name"));
            }
            if ("livestock".equals(module)) {
                int count = 0, weighed = 0; double weight = 0;
                for (Map<String, Object> animal : records.get("animals")) if (text(row,"id").equals(text(animal,"herd_id"))) {
                    count++;
                    Object kg = animal.get("weight_kg");
                    if (kg instanceof Number) { weighed++; weight += ((Number) kg).doubleValue(); }
                }
                row.put("quantity", count);
                if (weighed > 0) row.put("average_weight_kg", weight / weighed);
                else row.remove("average_weight_kg");
            }
        }
        return result;
    }
    @Override public Map<String, Object> save(String module, Map<String, Object> input) {
        if (!records.containsKey(module)) throw new IllegalArgumentException("Módulo demo no disponible.");
        Map<String, Object> row = copy(input);
        String key = "tasks".equals(module) ? "title" : "animals".equals(module) ? "tag" : "name";
        if (text(row,key).trim().isEmpty()) throw new IllegalArgumentException("Completá el nombre o identificación.");
        if ("tasks".equals(module) && !text(row,"related_entity_id").isEmpty() && find("fields",text(row,"related_entity_id")) == null)
            throw new IllegalArgumentException("El lote seleccionado ya no está disponible.");
        if ("animals".equals(module)) {
            if (find("livestock",text(row,"herd_id")) == null) throw new IllegalArgumentException("Seleccioná un rodeo disponible.");
            for (Map<String, Object> animal : records.get("animals"))
                if (!text(animal,"id").equals(text(row,"id")) && text(animal,"tag").equalsIgnoreCase(text(row,"tag")))
                    throw new IllegalArgumentException("Esa caravana ya existe en la demostración.");
        }
        String id = text(row,"id");
        if (id.isEmpty()) { id = "demo-" + UUID.randomUUID(); row.put("id",id); }
        Map<String, Object> previous = find(module,id);
        if (previous != null) records.get(module).remove(previous);
        records.get(module).add(0,row);
        changed.run();
        return copy(row);
    }
    @Override public void delete(String module, String id) {
        if ("livestock".equals(module)) for (Map<String, Object> animal : records.get("animals"))
            if (id.equals(text(animal,"herd_id"))) throw new IllegalArgumentException("Este rodeo contiene animales. Movelos a otro rodeo o eliminá sus fichas demo antes de eliminarlo.");
        if ("fields".equals(module)) for (Map<String, Object> task : records.get("tasks"))
            if (id.equals(text(task,"related_entity_id"))) throw new IllegalArgumentException("Hay tareas vinculadas a este lote. Editá sus referencias antes de eliminarlo.");
        Map<String, Object> row = find(module,id);
        if (row == null) throw new IllegalArgumentException("El registro ya no está disponible.");
        records.get(module).remove(row);
        changed.run();
    }
}
