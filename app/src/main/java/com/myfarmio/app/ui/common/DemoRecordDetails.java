package com.myfarmio.app.ui.common;

import static com.myfarmio.app.ui.common.RecordText.*;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.myfarmio.app.R;
import com.myfarmio.app.data.demo.DemoRepository;
import com.myfarmio.app.data.demo.RecordRepository;
import java.util.*;

/** Demo detail actions are explicit and never invoke a backend write. */
public final class DemoRecordDetails {
    private DemoRecordDetails() {}
    @SuppressWarnings("unchecked")
    public static void bind(MobileUi.Sheet sheet,String module,Map<String,Object> source,RecordRepository repository,Runnable edit,Runnable deleted) {
        Map<String,Object> row=DemoRepository.copy(source);
        sheet.row("DEMOSTRACIÓN", "Guardado solo en memoria durante esta sesión.");
        String section="";
        for (RecordFormSchema.Field field:RecordFormSchema.fields(module)) {
            if (!section.equals(field.section)) { section=field.section; sheet.heading(section); }
            String text=value(row,field.key);
            if ("related_entity_id".equals(field.key)) text=value(row,"related_entity_name");
            else if ("herd_id".equals(field.key)) text=value(row,"_herd_name");
            else if (RecordFormSchema.DATE.equals(field.kind)) text=dateLabel(text);
            else if (RecordFormSchema.SELECT.equals(field.kind)) text=text.isEmpty() ? "" : label(text);
            sheet.row(field.label,text);
        }
        if ("livestock".equals(module)) {
            sheet.row("Animales registrados",amount(number(row,"quantity")));
            sheet.row("Peso promedio",row.containsKey("average_weight_kg") ? amount(number(row,"average_weight_kg"))+" kg" : "");
            sheet.row("Actividad y pastoreo","Los eventos sanitarios, pesajes históricos y pastoreo todavía no tienen un servicio móvil conectado.");
        }
        if ("fields".equals(module)) sheet.row("Labores, aplicaciones y cosecha","El lote y sus tareas asociadas están disponibles en esta demo. Las aplicaciones y proyecciones quedan pendientes de integración.");
        if ("tasks".equals(module)) {
            sheet.heading("Checklist");
            List<Map<String,Object>> checklist=(List<Map<String,Object>>) row.get("_checklist");
            if (checklist==null||checklist.isEmpty()) sheet.row("Pasos", "No se definieron pasos para esta tarea.");
            else for (Map<String,Object> step:checklist) {
                MaterialCheckBox checkbox=new MaterialCheckBox(sheet.body.getContext());
                checkbox.setText(value(step,"text")); checkbox.setMinHeight(MobileUi.dp(sheet.body.getContext(),48));
                checkbox.setChecked(Boolean.TRUE.equals(step.get("done")));
                sheet.body.addView(checkbox);
                checkbox.setOnCheckedChangeListener((button,checked)-> {
                    step.put("done",checked);
                    long count=checklist.stream().filter(item->Boolean.TRUE.equals(item.get("done"))).count();
                    row.put("progress",count*100.0/checklist.size());
                    repository.save(module,row);
                });
            }
            sheet.heading("Comentarios");
            List<String> comments = row.get("_comments") instanceof List ? (List<String>) row.get("_comments") : new ArrayList<>();
            row.put("_comments",comments);
            for (String comment:comments) sheet.row("Comentario demo",comment);
            TextInputLayout input=(TextInputLayout) LayoutInflater.from(sheet.body.getContext()).inflate(R.layout.item_form_field,sheet.body,false);
            input.setHint("Agregar comentario (demo)");
            EditText editor=input.findViewById(R.id.form_input);
            editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            sheet.body.addView(input);
            sheet.button("Agregar comentario",()-> {
                String text=editor.getText().toString().trim();
                if (text.isEmpty()) { input.setError("Escribí un comentario."); return; }
                input.setError(null); comments.add(text);
                repository.save(module,row);
                sheet.row("Comentario demo",text); editor.setText("");
                MobileUi.hideKeyboard(sheet.body);
                Toast.makeText(sheet.body.getContext(),"Comentario agregado en demo",Toast.LENGTH_SHORT).show();
            });
            if (!"completed".equals(value(row,"status"))) sheet.button("Marcar como completada",()-> {
                row.put("status","completed"); row.put("progress",100);
                if (checklist!=null) for (Map<String,Object> step:checklist) step.put("done",true);
                repository.save(module,row); sheet.dismiss(); deleted.run();
                Toast.makeText(sheet.body.getContext(),"Tarea completada en demo",Toast.LENGTH_SHORT).show();
            });
        }
        sheet.button("Eliminar de la demo",()->new MaterialAlertDialogBuilder(sheet.body.getContext())
            .setTitle("¿Eliminar este registro?")
            .setMessage("Se eliminará “"+or(row,"title",or(row,"name",value(row,"tag")))+"” de esta sesión demo. No afecta datos de tu organización.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(dialog,which)-> {
                try { repository.delete(module,value(row,"id")); sheet.dismiss(); deleted.run();
                    Toast.makeText(sheet.body.getContext(),"Registro eliminado de la demo",Toast.LENGTH_SHORT).show();
                } catch (IllegalArgumentException error) {
                    new MaterialAlertDialogBuilder(sheet.body.getContext()).setTitle("El registro tiene vínculos").setMessage(error.getMessage()).setPositiveButton("Entendido",null).show();
                }
            }).show());
        sheet.primary("Editar registro",edit);
    }
}
