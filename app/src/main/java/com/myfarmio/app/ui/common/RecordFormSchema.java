package com.myfarmio.app.ui.common;

import java.util.*;
import com.myfarmio.app.R;

/** UI-only form definitions using the existing record keys. */
final class RecordFormSchema {
    static final String TEXT="text", MULTI="multi", NUMBER="number", PERCENT="percent", DATE="date", SELECT="select";
    static final class Field {
        final int sectionRes, labelRes; final String key, kind; final boolean required; final String[] choices;
        Field(int sectionRes,String key,int labelRes,String kind,boolean required,String... choices) {
            this.sectionRes=sectionRes; this.key=key; this.labelRes=labelRes; this.kind=kind; this.required=required; this.choices=choices;
        }
    }
    static List<Field> fields(String module) {
        List<Field> result = new ArrayList<>();
        if ("tasks".equals(module)) {
            result.add(new Field(R.string.form_section_work,"title",R.string.form_title,TEXT,true));
            result.add(new Field(R.string.form_section_work,"description",R.string.form_description,MULTI,false));
            result.add(new Field(R.string.form_section_organization,"status",R.string.form_status,SELECT,true,"pending","planned","in_progress","completed","cancelled"));
            result.add(new Field(R.string.form_section_organization,"priority",R.string.form_priority,SELECT,true,"low","medium","high","urgent"));
            result.add(new Field(R.string.form_section_organization,"related_entity_id",R.string.form_related_plot,SELECT,false));
            result.add(new Field(R.string.form_section_tracking,"start_date",R.string.form_start_date,DATE,false));
            result.add(new Field(R.string.form_section_tracking,"due_date",R.string.form_due_date,DATE,false));
            result.add(new Field(R.string.form_section_tracking,"progress",R.string.form_progress,PERCENT,false));
            result.add(new Field(R.string.form_section_tracking,"observations",R.string.form_observations,MULTI,false));
        } else if ("fields".equals(module)) {
            result.add(new Field(R.string.form_section_identity,"name",R.string.form_plot_name,TEXT,true));
            result.add(new Field(R.string.form_section_identity,"current_crop",R.string.form_crop,TEXT,false));
            result.add(new Field(R.string.form_section_identity,"zone",R.string.form_zone,TEXT,false));
            result.add(new Field(R.string.form_section_campaign,"area_hectares",R.string.form_area,NUMBER,false));
            result.add(new Field(R.string.form_section_campaign,"status",R.string.form_status,SELECT,true,"optimal","monitoring","harvest","resting","issue","inactive"));
            result.add(new Field(R.string.form_section_campaign,"crop_stage",R.string.form_crop_stage,TEXT,false));
            result.add(new Field(R.string.form_section_campaign,"soil_moisture_percent",R.string.form_moisture,PERCENT,false));
            result.add(new Field(R.string.form_section_campaign,"estimated_yield",R.string.form_yield,TEXT,false));
            result.add(new Field(R.string.form_section_next_action,"rotation",R.string.form_rotation,TEXT,false));
            result.add(new Field(R.string.form_section_next_action,"next_action",R.string.form_next_action,MULTI,false));
            result.add(new Field(R.string.form_section_next_action,"notes",R.string.form_observations,MULTI,false));
        } else if ("livestock".equals(module)) {
            result.add(new Field(R.string.form_section_identity,"name",R.string.form_herd_name,TEXT,true));
            result.add(new Field(R.string.form_section_identity,"category",R.string.form_category,TEXT,true));
            result.add(new Field(R.string.form_section_tracking,"status",R.string.form_status,SELECT,true,"stable","monitoring","attention","inactive"));
            result.add(new Field(R.string.form_section_tracking,"notes",R.string.form_observations,MULTI,false));
        } else if ("finance".equals(module)) {
            result.add(new Field(R.string.form_section_movement,"description",R.string.form_concept,TEXT,true));
            result.add(new Field(R.string.form_section_movement,"movement_type",R.string.form_type,SELECT,true,"income","expense"));
            result.add(new Field(R.string.form_section_movement,"amount",R.string.form_amount,NUMBER,true));
            result.add(new Field(R.string.form_section_movement,"date",R.string.form_date,DATE,true));
            result.add(new Field(R.string.form_section_classification,"category",R.string.form_category,SELECT,true,
                "otros","ventas","venta_granos","venta_hacienda","servicios_prestados","alquileres","otros_ingresos",
                "insumos","semillas","fertilizantes","agroquimicos","maquinaria","sanidad","alimentacion_animal",
                "combustible","servicios","servicios_contratados","transporte","mano_obra","mantenimiento","impuestos","salarios","otros_gastos"));
            result.add(new Field(R.string.form_section_classification,"finance_module",R.string.form_module,SELECT,true,
                "general","agricultura","ganaderia","maquinaria","inventario","administracion"));
            result.add(new Field(R.string.form_section_tracking,"counterparty",R.string.form_counterparty,TEXT,false));
            result.add(new Field(R.string.form_section_tracking,"payment_method",R.string.form_payment_method,SELECT,false,
                "transferencia","efectivo","tarjeta","cheque","mercado_pago","cuenta_corriente","canje","otro"));
            result.add(new Field(R.string.form_section_tracking,"status",R.string.form_status,SELECT,true,"paid","collected","pending","overdue","scheduled","cancelled"));
            result.add(new Field(R.string.form_section_tracking,"due_date",R.string.form_due_date,DATE,false));
            result.add(new Field(R.string.form_section_tracking,"reference_code",R.string.form_reference,TEXT,false));
            result.add(new Field(R.string.form_section_tracking,"notes",R.string.form_observations,MULTI,false));
        } else if ("inventory".equals(module)) {
            result.add(new Field(R.string.form_section_supply,"name",R.string.form_name,TEXT,true));
            result.add(new Field(R.string.form_section_supply,"description",R.string.form_description,TEXT,false));
            result.add(new Field(R.string.form_section_supply,"lot",R.string.form_lot,TEXT,false));
            result.add(new Field(R.string.form_section_stock,"unit",R.string.form_unit,TEXT,true));
            result.add(new Field(R.string.form_section_stock,"current_quantity",R.string.form_current_quantity,NUMBER,false));
            result.add(new Field(R.string.form_section_stock,"target_quantity",R.string.form_target_quantity,NUMBER,false));
            result.add(new Field(R.string.form_section_stock,"reorder_point",R.string.form_reorder_point,NUMBER,false));
            result.add(new Field(R.string.form_section_stock,"status",R.string.form_status,SELECT,true,"ok","low","critical"));
            result.add(new Field(R.string.form_section_tracking,"expiry_date",R.string.form_expiry_date,DATE,false));
        } else {
            result.add(new Field(R.string.form_section_identity,"tag",R.string.form_tag,TEXT,true));
            result.add(new Field(R.string.form_section_identity,"herd_id",R.string.form_herd,SELECT,true));
            result.add(new Field(R.string.form_section_identity,"category",R.string.form_category,TEXT,true));
            result.add(new Field(R.string.form_section_productivity,"weight_kg",R.string.form_weight,NUMBER,false));
            result.add(new Field(R.string.form_section_productivity,"birth_date",R.string.form_birth_date,DATE,false));
            result.add(new Field(R.string.form_section_tracking,"health_status",R.string.form_health_status,SELECT,true,"healthy","sick","treatment","quarantine","deceased","sold"));
            result.add(new Field(R.string.form_section_tracking,"reproductive_status",R.string.form_reproductive_status,SELECT,false,"open","pregnant","lactating","breeding","not_applicable"));
            result.add(new Field(R.string.form_section_tracking,"notes",R.string.form_observations,MULTI,false));
        }
        return result;
    }
}
