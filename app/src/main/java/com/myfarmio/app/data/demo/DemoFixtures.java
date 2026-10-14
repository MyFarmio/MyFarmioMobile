package com.myfarmio.app.data.demo;

import java.time.LocalDate;
import java.util.*;

/** Fictional Stitch fixtures, isolated in memory. No credential or remote write. */
final class DemoFixtures {
    private DemoFixtures() {}
    static Map<String, Object> row(Object... pairs) {
        Map<String,Object> row = new LinkedHashMap<>();
        for (int i=0;i<pairs.length;i+=2) row.put((String)pairs[i],pairs[i+1]);
        return row;
    }
    static Map<String,List<Map<String,Object>>> create() {
        Map<String,List<Map<String,Object>>> all = new LinkedHashMap<>();
        all.put("fields", new ArrayList<>(Arrays.asList(row("id","demo-plot-north","name","Lote Norte",
            "current_crop","Girasol","area_hectares",4000,"status","monitoring","crop_stage","Floración",
            "soil_moisture_percent",30,"zone","Sector Sur","progress",95,"estimated_yield","4000",
            "next_action","Confirmar rotación de cultivo","rotation","Soja → trigo → girasol","_responsible","Laura Gómez"))));
        all.put("tasks", new ArrayList<>(Arrays.asList(
            row("id","demo-task-stock","title","Inventario de semillas — reposición","description","Revisar existencias antes de la próxima siembra.",
                "status","in_progress","priority","high","_responsible","Laura Gómez","progress",0),
            row("id","demo-task-done","title","Preparar lote para siembra","status","completed","priority","low",
                "related_entity_id","demo-plot-north","progress",100,"due_date",LocalDate.now().minusDays(2).toString()))));
        all.put("livestock",new ArrayList<>(Arrays.asList(
            row("id","demo-herd-four","name","Potrero 4","category","Vacas","status","stable","_location","Potrero este"),
            row("id","demo-herd-young","name","Rodeo de recría","category","Novillos","status","stable","_location","Corral este"))));
        List<Map<String,Object>> animals=new ArrayList<>();
        // Herd totals are derived by DemoRepository, never independent hardcoded counters.
        for(int i=0;i<60;i++) animals.add(row("id","demo-animal-"+i,"tag",i==0?"AR-1048":"AR-"+(1100+i),
            "herd_id",i<10?"demo-herd-four":"demo-herd-young","category",i<10?"Vaca":"Novillo",
            "weight_kg",i==0?130:i<10?208:145,"health_status",i==0?"monitoring":"healthy",
            "reproductive_status","open","_location",i<10?"Potrero este":"Corral este"));
        all.put("animals",animals);
        all.put("inventory",new ArrayList<>(Arrays.asList(
            row("id","demo-supply-90","name","Insecticida","category","Protección vegetal","quantity",48,
                "capacity",144,"unit","Frascos","lot","AR-90","expiry","10 oct","status","low","depot","Depósito Central: Sector 2"),
            row("id","demo-supply-5046","name","Fertilizantes","category","Fertilizantes","quantity",15,
                "capacity",150,"unit","Litros","lot","UR_5046","expiry","14 nov","status","critical","depot","Depósito Central: Sector 2"))));
        all.put("inventory_movements",new ArrayList<>(Arrays.asList(
            row("id","demo-stock-90","record_id","demo-supply-90","name","Insecticida","quantity",48,"unit","Frascos","kind","Stock inicial","date","2026-09-29"),
            row("id","demo-stock-5046","record_id","demo-supply-5046","name","Fertilizantes","quantity",15,"unit","Litros","kind","Stock inicial","date","2026-09-29"))));
        all.put("campaign",new ArrayList<>(Arrays.asList(row("name","Campaña demo","projected_tons",0,"visible_jobs",0))));
        all.put("pasture",new ArrayList<>(Arrays.asList(row("name","Pastoreo y Cargas","detail","Disponibilidad forrajera óptima en parcelas norte y este. Próxima rotación en 6 días."))));
        all.put("finance",new ArrayList<>(Arrays.asList(
            row("id","demo-finance-income","description","Venta de girasoles","movement_type","income",
                "amount",480000,"currency","ARS","status","posted","category","Agricultura","counterparty","Agrosur",
                "date","2026-09-29","plot","Lote Norte"),
            row("id","demo-finance-rent","description","Alquiler de lote","movement_type","expense",
                "amount",150000,"currency","ARS","status","overdue","category","Alquileres","counterparty","Contratista",
                "date","2026-09-29","plot","Lote Norte"))));
        all.put("weather",new ArrayList<>(Arrays.asList(row("id","demo-weather","name","Estación Norte",
            "temperature","24°C","condition","Soleado","updated","Ejemplo local · 15 min","moisture","30%",
            "wind","12 km/h NO","pressure","1014 hPa","uv","Óptima (4)","rain","18 mm","etp","3.2 mm/día",
            "water_percent",62,"water_legend","Agua útil: 62%   ·   Déficit hídrico leve",
            "plot","LOTE NORTE","crop","Girasol Confite","stage","Estadío R5.1 (Apertura de capítulo floral)",
            "recommendation","Recomendación de ejemplo: mantener riego complementario durante la tarde para evitar estrés hídrico previo a floración plena."))));
        all.put("windows",new ArrayList<>(Arrays.asList(
            row("name","Pulverización foliar","detail","Ventana sugerida: 06:00 a 11:00 hs (viento < 14 km/h)","status","FAVORABLE","icon","agriculture"),
            row("name","Siembra directa","detail","Temperatura suelo: 18.5°C · Humedad suficiente","status","FAVORABLE","icon","grass"),
            row("name","Cosecha / Trilla","detail","Humedad en grano esperada: 14.8% a partir de 14:00 hs","status","PRECAUCIÓN","icon","local_shipping"))));
        all.put("climate_alerts",new ArrayList<>(Arrays.asList(row("name","Alerta preventiva: descenso térmico nocturno",
            "time","JUEVES NOCHE","detail","Escenario de demostración: descenso a 3°C con punto de rocío bajo. Riesgo moderado de helada suave en bajíos del Lote Sur. Preparar resguardos para plantines jóvenes."))));
        all.put("profile",new ArrayList<>(Arrays.asList(row("id","demo-profile","name","Laura Gómez",
            "email","laura@example.com","role","Propietaria","provider","DEMO","phone","Sin teléfono cargado",
            "timezone","America/Argentina/Buenos_Aires","created","28 de mayo de 2026, 18:17",
            "last_access","30 de septiembre de 2026, 06:38","organization","Mi campo demo",
            "slug","mi-campo-demo","membership_role","Propietaria titular","membership_title","Sin cargo asignado"))));
        all.put("rain_notes",new ArrayList<>());
        return all;
    }
}

