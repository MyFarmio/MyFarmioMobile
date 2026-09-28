package com.myfarmio.app.ui.common;

import static com.myfarmio.app.ui.common.RecordText.*;
import static com.myfarmio.app.ui.common.RecordFormSchema.*;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.myfarmio.app.R;
import com.myfarmio.app.data.demo.DemoRepository;
import com.myfarmio.app.data.demo.RecordRepository;
import java.time.LocalDate;
import java.util.*;

/** Owns an isolated edit draft. Only the demo repository supports writes today. */
public final class RecordEditor {
    private final MobileUi.Sheet sheet;
    private final RecordRepository repository;
    private final String module;
    private final Map<String,Object> original;
    private final Map<Field, TextInputLayout> controls = new LinkedHashMap<>();
    private final Map<String,List<String>> optionKeys = new HashMap<>();
    private final Map<String,String> dates = new HashMap<>();
    private final Map<String,String> initial = new HashMap<>();
    private final Runnable saved;
    public RecordEditor(MobileUi.Sheet sheet, String module, Map<String,Object> record, RecordRepository repository, Runnable saved) {
        this.sheet=sheet; this.module=module; this.repository=repository; this.saved=saved;
        original = DemoRepository.copy(record);
        sheet.row("DEMOSTRACIÓN · Solo esta sesión", "Estos cambios no se envían a tu organización. Los campos con * son obligatorios.");
        String section = "";
        for (Field field : fields(module)) {
            if (!section.equals(field.section)) { section=field.section; sheet.heading(section); }
            add(field);
        }
        if ("livestock".equals(module)) sheet.row("Cantidad y peso promedio", "En esta demo se calculan desde las fichas de animales del rodeo.");
        sheet.primary("Guardar en demo", this::save);
        sheet.guardClose(this::requestClose);
    }
    private void add(Field field) {
        Context context = sheet.body.getContext();
        TextInputLayout layout = (TextInputLayout) LayoutInflater.from(context).inflate(
            SELECT.equals(field.kind) ? R.layout.item_form_select : R.layout.item_form_field, sheet.body, false);
        layout.setHint(field.label + (field.required ? " *" : ""));
        EditText input = layout.findViewById(R.id.form_input);
        String raw = value(original,field.key);
        if (SELECT.equals(field.kind)) {
            MaterialAutoCompleteTextView select=(MaterialAutoCompleteTextView) input;
            List<String> keys=new ArrayList<>(), labels=new ArrayList<>();
            if (!field.required || field.choices.length == 0) { keys.add(""); labels.add(field.required ? "Seleccionar rodeo" : "Sin asociación"); }
            if (field.choices.length > 0) for (String key:field.choices) { keys.add(key); labels.add(label(key)); }
            else for (Map<String,Object> item:repository.list(field.key.equals("herd_id") ? "livestock" : "fields")) {
                keys.add(value(item,"id")); labels.add(value(item,"name"));
            }
            int index=keys.indexOf(raw);
            if (index < 0 && !raw.isEmpty()) { keys.add(raw); labels.add(label(raw)); index=keys.size()-1; }
            if (index < 0) index=0;
            select.setAdapter(new ArrayAdapter<>(context,android.R.layout.simple_dropdown_item_1line,labels));
            select.setText(labels.get(index),false);
            select.setTag(keys.get(index));
            select.setOnItemClickListener((parent,view,position,id) -> select.setTag(keys.get(position)));
            optionKeys.put(field.key,keys);
        } else if (DATE.equals(field.kind)) {
            dates.put(field.key,raw);
            input.setText(raw.isEmpty() ? "" : dateLabel(raw));
            input.setFocusable(false); input.setClickable(true);
            input.setContentDescription(field.label + ". Seleccionar fecha");
            input.setOnClickListener(v -> {
                LocalDate picked=date(dates.get(field.key));
                LocalDate selected=picked==null?LocalDate.now():picked;
                DatePickerDialog dialog = new DatePickerDialog(context,(picker,year,month,day) -> {
                    String next=LocalDate.of(year,month+1,day).toString();
                    dates.put(field.key,next); input.setText(dateLabel(next));
                },selected.getYear(),selected.getMonthValue()-1,selected.getDayOfMonth());
                dialog.setButton(android.content.DialogInterface.BUTTON_NEUTRAL,"Quitar fecha",(d,which) -> { dates.put(field.key,""); input.setText(""); });
                dialog.show();
            });
        } else {
            input.setText(raw);
            if (NUMBER.equals(field.kind)||PERCENT.equals(field.kind)) input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            else if (MULTI.equals(field.kind)) { input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE); input.setMinLines(2); input.setMaxLines(5); }
            else input.setSingleLine(true);
        }
        sheet.body.addView(layout);
        controls.put(field,layout);
        initial.put(field.key,read(field,layout));
    }
    private String read(Field field,TextInputLayout layout) {
        EditText input=layout.findViewById(R.id.form_input);
        if (SELECT.equals(field.kind)) return String.valueOf(input.getTag());
        if (DATE.equals(field.kind)) return dates.get(field.key);
        return input.getText().toString().trim();
    }
    private void requestClose() {
        boolean dirty=false;
        for (Map.Entry<Field,TextInputLayout> item:controls.entrySet())
            if (!Objects.equals(initial.get(item.getKey().key),read(item.getKey(),item.getValue()))) dirty=true;
        if (!dirty) { sheet.dismiss(); return; }
        new MaterialAlertDialogBuilder(sheet.body.getContext()).setTitle("¿Descartar los cambios?")
            .setMessage("El borrador no se guardó. El registro original se conserva.")
            .setNegativeButton("Seguir editando",null).setPositiveButton("Descartar",(d,w)->sheet.dismiss()).show();
    }
    private void save() {
        Map<String,Object> draft=DemoRepository.copy(original);
        TextInputLayout first=null;
        for (Map.Entry<Field,TextInputLayout> item:controls.entrySet()) {
            Field field=item.getKey(); TextInputLayout layout=item.getValue(); String raw=read(field,layout);
            layout.setError(null);
            String error=null;
            if (field.required && raw.isEmpty()) error="Completá este campo.";
            if (!raw.isEmpty() && (NUMBER.equals(field.kind)||PERCENT.equals(field.kind))) {
                try {
                    double number=Double.parseDouble(raw.replace(',','.'));
                    if (!Double.isFinite(number)||number<0||(PERCENT.equals(field.kind)&&number>100)) error=PERCENT.equals(field.kind) ? "Ingresá un valor entre 0 y 100." : "Ingresá un número mayor o igual a cero.";
                    else draft.put(field.key,number);
                } catch (NumberFormatException e) { error="Ingresá un número válido."; }
            } else if (raw.isEmpty()) draft.remove(field.key); else draft.put(field.key,raw);
            if (error!=null) { layout.setError(error); if (first==null) first=layout; }
        }
        if (first!=null) {
            first.requestFocus(); first.requestRectangleOnScreen(new android.graphics.Rect(0,0,first.getWidth(),first.getHeight()),false);
            first.announceForAccessibility("Revisá los campos marcados antes de guardar.");
            return;
        }
        sheet.action.setEnabled(false);
        try {
            repository.save(module,draft);
            sheet.dismiss(); saved.run();
        } catch (IllegalArgumentException error) {
            sheet.action.setEnabled(true);
            new MaterialAlertDialogBuilder(sheet.body.getContext()).setTitle("No se pudo guardar").setMessage(error.getMessage()).setPositiveButton("Revisar",null).show();
        }
    }
}
