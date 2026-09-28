package com.myfarmio.app.ui.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Original local examples retained separately. Never mixed with organization data. */
final class LocalExamples {
    private LocalExamples() {}
    static List<Map<String, Object>> forModule(String module) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if ("tasks".equals(module)) {
            rows.add(row("title", "Reponer stock de urea", "description", "Stock actual por debajo del objetivo operativo.",
                "_responsible", "Juan Pérez", "due_date", "2026-06-09", "status", "pending"));
            rows.add(row("title", "Confirmar ventana de fertilización", "description", "Validar humedad, viento y disponibilidad.",
                "_responsible", "María Gómez", "due_date", "2026-06-04", "status", "in_progress"));
            rows.add(row("title", "Revisar lote 12", "description", "Se detectó diferencia de humedad y cobertura.",
                "_responsible", "Nicolás Ruiz", "due_date", "2026-06-11", "status", "in_review"));
        } else if ("livestock".equals(module)) {
            rows.add(row("name", "Caravana 184", "category", "Vaca · Rodeo Norte", "status", "En seguimiento",
                "_weight", "482 kg", "_location", "Potrero 3", "_animal", "true"));
            rows.add(row("name", "Caravana 241", "category", "Vaquillona · Rodeo Cría", "status", "Control hoy",
                "_weight", "416 kg", "_location", "Corral sanitario", "_animal", "true"));
            rows.add(row("name", "Caravana 097", "category", "Toro · Reproductores", "status", "Óptimo",
                "_weight", "728 kg", "_location", "Potrero 1", "_animal", "true"));
        }
        return rows;
    }
    private static Map<String, Object> row(String... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) row.put(pairs[i], pairs[i + 1]);
        return row;
    }
}

