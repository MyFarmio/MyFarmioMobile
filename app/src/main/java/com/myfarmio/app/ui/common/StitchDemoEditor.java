package com.myfarmio.app.ui.common;
import static com.myfarmio.app.ui.common.RecordText.*;
import android.view.LayoutInflater;
import android.widget.*;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myfarmio.app.R;
import java.util.*;

/** Local-only editor. Preserves the existing repository boundary and never calls an API. */
public final class StitchDemoEditor {
    private StitchDemoEditor() {}
    public static void bind(MobileUi.Sheet sheet,WorkspaceViewModel workspace,String module,Map<String,Object> input) {
        if(!workspace.isDemo()) throw new IllegalStateException("Demo editor requires explicit demo mode");
        Map<String,Object> draft=new LinkedHashMap<>(input);
        sheet.row("Demostración local","Los cambios duran esta sesión. No se envían a tu organización.");
        String[][] fields=module.equals("finance")?new String[][]{{"description","Descripción"},{"amount","Monto (ARS)"},{"counterparty","Contraparte"},{"date","Fecha (AAAA-MM-DD)"},{"plot","Lote / referencia"}}:
            module.equals("inventory")?new String[][]{{"name","Nombre del insumo"},{"quantity","Cantidad disponible"},{"capacity","Capacidad / objetivo"},{"unit","Unidad"},{"lot","Lote de insumo"},{"expiry","Vencimiento"},{"depot","Ubicación"}}:
            module.equals("profile")?new String[][]{{"name","Nombre completo"},{"email","Correo"},{"role","Cargo"},{"phone","Teléfono"}}:
            new String[][]{{"name","Nota de campo"},{"rain","Lluvia (mm)"}};
        Map<String,TextInputEditText> editors=new LinkedHashMap<>();
        for(String[] field:fields) {
            TextInputLayout box=(TextInputLayout)LayoutInflater.from(sheet.body.getContext()).inflate(R.layout.item_stitch_input,sheet.body,false);
            box.setHint(field[1]);
            TextInputEditText edit=box.findViewById(R.id.stitch_input);
            edit.setText(value(input,field[0]));
            if(Arrays.asList("amount","quantity","capacity","rain").contains(field[0]))
                edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if(field[0].equals("email")) edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            editors.put(field[0],edit); sheet.body.addView(box);
        }
        Spinner type=null, status=null, category=null;
        if(module.equals("finance")) {
            type=choice(sheet,"Tipo de movimiento",new String[]{"Ingreso","Gasto"},"expense".equals(value(input,"movement_type"))?1:0);
            status=choice(sheet,"Estado",new String[]{"Cobrado / pagado","Pendiente","Vencido"}, "posted".equals(value(input,"status"))?0:"overdue".equals(value(input,"status"))?2:1);
        }
        if(module.equals("inventory")) category=choice(sheet,"Categoría",new String[]{"Semillas","Fertilizantes","Sanidad animal","Combustible","Protección vegetal"},Math.max(0,Arrays.asList("Semillas","Fertilizantes","Sanidad animal","Combustible","Protección vegetal").indexOf(value(input,"category"))));
        Spinner movementType=type,movementStatus=status,supplyCategory=category;
        sheet.guardClose(() -> new MaterialAlertDialogBuilder(sheet.body.getContext()).setTitle("¿Descartar los cambios?")
            .setNegativeButton("Seguir editando",null).setPositiveButton("Descartar",(d,w)->sheet.dismiss()).show());
        sheet.button("Cancelar",sheet::dismiss);
        sheet.primary("Guardar en demo",() -> {
            for(Map.Entry<String,TextInputEditText> entry:editors.entrySet()) {
                String value=entry.getValue().getText().toString().trim();
                if(Arrays.asList("amount","quantity","capacity","rain").contains(entry.getKey())) {
                    try { double n=Double.parseDouble(value.replace(',','.')); if(!Double.isFinite(n)||n<0) throw new NumberFormatException(); draft.put(entry.getKey(),n); }
                    catch(NumberFormatException e) { entry.getValue().setError("Ingresá un número válido, mayor o igual a cero"); entry.getValue().requestFocus(); return; }
                } else draft.put(entry.getKey(),value);
            }
            String nameKey=module.equals("finance")?"description":"name";
            if(value(draft,nameKey).isEmpty()) { editors.get(nameKey).setError("Este campo es obligatorio"); return; }
            if(module.equals("finance")) {
                if(date(value(draft,"date"))==null) { editors.get("date").setError("Usá AAAA-MM-DD"); return; }
                draft.put("currency","ARS"); draft.put("movement_type",movementType.getSelectedItemPosition()==0?"income":"expense");
                draft.put("status",new String[]{"posted","pending","overdue"}[movementStatus.getSelectedItemPosition()]);
                draft.putIfAbsent("category","General");
            }
            if(module.equals("inventory")) {
                if(number(draft,"capacity")<=0) { editors.get("capacity").setError("El objetivo debe ser mayor a cero"); return; }
                draft.put("category",supplyCategory.getSelectedItem().toString());
                double ratio=number(draft,"quantity")/number(draft,"capacity");
                draft.put("status",ratio<=0.15?"critical":ratio<0.5?"low":"healthy");
            }
            if(module.equals("profile")&&!android.util.Patterns.EMAIL_ADDRESS.matcher(value(draft,"email")).matches()) { editors.get("email").setError("Revisá el correo"); return; }
            try { workspace.records().save(module,draft); sheet.dismiss(); Toast.makeText(sheet.body.getContext(),"Guardado en la demo local",Toast.LENGTH_SHORT).show(); }
            catch(IllegalArgumentException e) { editors.get(nameKey).setError(e.getMessage()); }
        });
    }
    private static Spinner choice(MobileUi.Sheet sheet,String title,String[] values,int selected) {
        sheet.heading(title);
        Spinner view=new Spinner(sheet.body.getContext()); view.setMinimumHeight(MobileUi.dp(sheet.body.getContext(),48)); view.setContentDescription(title);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(sheet.body.getContext(),android.R.layout.simple_spinner_item,values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); view.setAdapter(adapter); view.setSelection(selected); sheet.body.addView(view); return view;
    }
}

