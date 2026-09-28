package com.myfarmio.app.data.demo;

import java.time.LocalDate;
import java.util.*;

/** Fictional, linked fixtures. Never sent to Supabase. */
final class DemoFixtures {
    private DemoFixtures() {}
    static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
        return row;
    }
    static Map<String, List<Map<String, Object>>> create() {
        LocalDate today = LocalDate.now();
        Map<String, List<Map<String, Object>>> all = new LinkedHashMap<>();
        all.put("fields", new ArrayList<>(Arrays.asList(
            row("id","demo-plot-north","name","Lote Norte","current_crop","Maíz temprano","area_hectares",42,
                "status","active","crop_stage","Vegetativo","soil_moisture_percent",28,"zone","Sector Norte",
                "next_action","Revisar cobertura y fertilización","rotation","Soja → trigo → maíz","_responsible","Laura Gómez"),
            row("id","demo-plot-south","name","Lote Sur","current_crop","Trigo","area_hectares",31,
                "status","monitoring","crop_stage","Maduración","soil_moisture_percent",16,"zone","Sector Sur",
                "next_action","Medir humedad antes de cosechar","_responsible","Martín Pérez"),
            row("id","demo-plot-east","name","Lote Este","current_crop","Soja","area_hectares",24,
                "status","active","crop_stage","Floración","soil_moisture_percent",32,"zone","Sector Este",
                "next_action","Monitorear malezas","_responsible","Laura Gómez")
        )));
        all.put("tasks", new ArrayList<>(Arrays.asList(
            row("id","demo-task-moisture","title","Medir humedad del trigo","description","Tomar muestras en tres puntos del lote.",
                "status","in_progress","priority","high","due_date",today.toString(),"start_date",today.minusDays(1).toString(),
                "related_entity_id","demo-plot-south","related_entity_name","Lote Sur","_responsible","Martín Pérez","progress",50,
                "_checklist",new ArrayList<>(Arrays.asList(row("text","Preparar el medidor","done",true),row("text","Registrar las tres muestras","done",false)))),
            row("id","demo-task-fertilizer","title","Revisar fertilización","description","Validar cobertura antes de coordinar la próxima labor.",
                "status","pending","priority","urgent","due_date",today.minusDays(1).toString(),
                "related_entity_id","demo-plot-north","related_entity_name","Lote Norte","_responsible","Laura Gómez","progress",0),
            row("id","demo-task-weeds","title","Recorrer el lote Este","description","Revisar cabeceras y bordes.",
                "status","planned","priority","medium","due_date",today.plusDays(2).toString(),
                "related_entity_id","demo-plot-east","related_entity_name","Lote Este","_responsible","Laura Gómez","progress",0),
            row("id","demo-task-fence","title","Controlar alambrado","description","Recorrido del perímetro finalizado.",
                "status","completed","priority","low","due_date",today.minusDays(2).toString(),"_responsible","Martín Pérez","progress",100)
        )));
        all.put("livestock", new ArrayList<>(Arrays.asList(
            row("id","demo-herd-breed","name","Rodeo de cría","category","Vacas","status","stable","_location","Potrero 4","notes","Seguimiento semanal."),
            row("id","demo-herd-young","name","Recría","category","Vaquillonas","status","monitoring","_location","Potrero 7","notes","Revisar condición corporal.")
        )));
        all.put("animals", new ArrayList<>(Arrays.asList(
            row("id","demo-animal-1048","tag","AR-1048","herd_id","demo-herd-breed","category","Vaca","weight_kg",420,"health_status","healthy","reproductive_status","pregnant","_location","Potrero 4"),
            row("id","demo-animal-1052","tag","AR-1052","herd_id","demo-herd-breed","category","Vaca","weight_kg",405,"health_status","healthy","reproductive_status","open","_location","Potrero 4"),
            row("id","demo-animal-2104","tag","AR-2104","herd_id","demo-herd-young","category","Vaquillona","weight_kg",285,"health_status","monitoring","reproductive_status","open","_location","Potrero 7"),
            row("id","demo-animal-2108","tag","AR-2108","herd_id","demo-herd-young","category","Vaquillona","weight_kg",298,"health_status","healthy","reproductive_status","served","_location","Potrero 7")
        )));
        return all;
    }
}
