package com.myfarmio.app.ui.common;

import java.util.*;

/** UI-only form definitions using the existing record keys. */
final class RecordFormSchema {
    static final String TEXT="text", MULTI="multi", NUMBER="number", PERCENT="percent", DATE="date", SELECT="select";
    static final class Field {
        final String section, key, label, kind; final boolean required; final String[] choices;
        Field(String section,String key,String label,String kind,boolean required,String... choices) {
            this.section=section; this.key=key; this.label=label; this.kind=kind; this.required=required; this.choices=choices;
        }
    }
    static List<Field> fields(String module) {
        List<Field> result = new ArrayList<>();
        if ("tasks".equals(module)) {
            result.add(new Field("Información del trabajo","title","Título",TEXT,true));
            result.add(new Field("Información del trabajo","description","Descripción",MULTI,false));
            result.add(new Field("Organización","status","Estado",SELECT,true,"pending","planned","in_progress","in_review","completed","cancelled"));
            result.add(new Field("Organización","priority","Prioridad",SELECT,true,"low","medium","high","urgent"));
            result.add(new Field("Organización","_responsible","Responsable (demo)",TEXT,false));
            result.add(new Field("Organización","related_entity_id","Lote asociado",SELECT,false));
            result.add(new Field("Fechas y seguimiento","start_date","Fecha de inicio",DATE,false));
            result.add(new Field("Fechas y seguimiento","due_date","Vencimiento",DATE,false));
            result.add(new Field("Fechas y seguimiento","progress","Avance (%)",PERCENT,false));
            result.add(new Field("Fechas y seguimiento","observations","Observaciones",MULTI,false));
        } else if ("fields".equals(module)) {
            result.add(new Field("Identificación","name","Nombre del lote",TEXT,true));
            result.add(new Field("Identificación","current_crop","Cultivo",TEXT,false));
            result.add(new Field("Identificación","zone","Zona",TEXT,false));
            result.add(new Field("Campaña","area_hectares","Superficie (ha)",NUMBER,false));
            result.add(new Field("Campaña","status","Estado",SELECT,true,"active","monitoring","issue","harvesting","resting"));
            result.add(new Field("Campaña","crop_stage","Etapa del cultivo",TEXT,false));
            result.add(new Field("Campaña","soil_moisture_percent","Humedad (%)",PERCENT,false));
            result.add(new Field("Campaña","estimated_yield","Rinde estimado (ej. 8 t/ha)",TEXT,false));
            result.add(new Field("Próxima acción","_responsible","Responsable (demo)",TEXT,false));
            result.add(new Field("Próxima acción","rotation","Rotación",TEXT,false));
            result.add(new Field("Próxima acción","next_action","Próxima acción",MULTI,false));
            result.add(new Field("Próxima acción","notes","Observaciones",MULTI,false));
        } else if ("livestock".equals(module)) {
            result.add(new Field("Identificación","name","Nombre del rodeo",TEXT,true));
            result.add(new Field("Identificación","category","Categoría",TEXT,false));
            result.add(new Field("Seguimiento","status","Estado",SELECT,true,"stable","monitoring","critical"));
            result.add(new Field("Seguimiento","_location","Potrero / ubicación",TEXT,false));
            result.add(new Field("Seguimiento","notes","Observaciones",MULTI,false));
        } else {
            result.add(new Field("Identificación","tag","Caravana / identificación",TEXT,true));
            result.add(new Field("Identificación","herd_id","Rodeo",SELECT,true));
            result.add(new Field("Identificación","category","Categoría",SELECT,true,"Ternero","Ternera","Vaquillona","Vaca","Novillo","Toro"));
            result.add(new Field("Datos productivos","weight_kg","Peso actual (kg)",NUMBER,false));
            result.add(new Field("Datos productivos","_location","Potrero / ubicación",TEXT,false));
            result.add(new Field("Datos productivos","birth_date","Fecha de nacimiento",DATE,false));
            result.add(new Field("Seguimiento","health_status","Estado sanitario",SELECT,true,"healthy","monitoring","treatment","sick","critical"));
            result.add(new Field("Seguimiento","reproductive_status","Estado reproductivo",SELECT,false,"open","pregnant","served","dry","calving"));
            result.add(new Field("Seguimiento","notes","Observaciones",MULTI,false));
        }
        return result;
    }
}
