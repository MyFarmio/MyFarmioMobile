package com.myfarmio.app.ui.common;

import static com.myfarmio.app.ui.common.RecordText.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.myfarmio.app.R;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/** Local-only editor. Preserves the existing repository boundary and never calls an API. */
public final class StitchDemoEditor {
    private static final String TEXT="text", NUMBER="number", DATE="date";
    private static final class Field {
        final String key,kind; final int label,section;
        Field(String key,int label,String kind,int section){this.key=key;this.label=label;this.kind=kind;this.section=section;}
    }
    private StitchDemoEditor() {}

    public static void bind(MobileUi.Sheet sheet,WorkspaceViewModel workspace,String module,Map<String,Object> input) {
        if(!workspace.isDemo()) throw new IllegalStateException("Demo editor requires explicit demo mode");
        Context context=sheet.body.getContext();
        Map<String,Object> draft=new LinkedHashMap<>(input);
        sheet.row(context.getString(R.string.demo_local_title),context.getString(R.string.demo_local_body));
        Map<String,TextInputEditText> editors=new LinkedHashMap<>();
        Map<String,TextInputLayout> controls=new HashMap<>();
        Map<String,String> dates=new HashMap<>();
        int section=0;
        for(Field field:fields(module)) {
            if(section!=field.section){section=field.section;sheet.heading(context.getString(section));}
            TextInputLayout box=(TextInputLayout)LayoutInflater.from(context).inflate(R.layout.item_stitch_input,sheet.body,false);
            box.setHint(context.getString(field.label)); box.setStartIconDrawable(icon(context,field.key));
            TextInputEditText edit=box.findViewById(R.id.stitch_input);
            String raw=value(input,field.key);
            if(NUMBER.equals(field.kind)) edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            else if("email".equals(field.key)) edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            else edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            if(DATE.equals(field.kind)) {
                dates.put(field.key,raw); edit.setText(raw.isEmpty()?"":dateLabel(raw)); edit.setFocusable(false); edit.setClickable(true);
                box.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
                edit.setOnClickListener(v->openDate(context,field.key,dates,edit,box));
            } else edit.setText(raw);
            editors.put(field.key,edit); controls.put(field.key,box); sheet.body.addView(box);
            edit.addTextChangedListener(new TextWatcher(){
                @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                    if(DATE.equals(field.kind)&&s.length()==0)dates.put(field.key,"");
                    if(box.getError()!=null)box.setError(validate(field,s.toString().trim(),context));
                }
                @Override public void afterTextChanged(Editable s){}
            });
        }
        MaterialAutoCompleteTextView movementType=null,movementStatus=null,supplyCategory=null;
        if(module.equals("finance")) {
            movementType=choice(sheet,context.getString(R.string.demo_movement_type),context.getResources().getStringArray(R.array.demo_movement_types),"expense".equals(value(input,"movement_type"))?1:0,R.drawable.st_payments);
            movementStatus=choice(sheet,context.getString(R.string.demo_record_status),context.getResources().getStringArray(R.array.demo_finance_statuses),"posted".equals(value(input,"status"))?0:"overdue".equals(value(input,"status"))?2:1,R.drawable.st_task_alt);
        }
        if(module.equals("inventory")) supplyCategory=choice(sheet,context.getString(R.string.demo_category),context.getResources().getStringArray(R.array.demo_inventory_categories),Math.max(0,Arrays.asList(context.getResources().getStringArray(R.array.demo_inventory_categories)).indexOf(value(input,"category"))),R.drawable.st_inventory_2);
        MaterialAutoCompleteTextView type=movementType,status=movementStatus,category=supplyCategory;
        sheet.guardClose(() -> new MaterialAlertDialogBuilder(context).setTitle(R.string.form_discard_title)
            .setNegativeButton(R.string.form_keep_editing,null).setPositiveButton(R.string.form_discard,(d,w)->sheet.dismiss()).show());
        sheet.primary(context.getString(R.string.form_save_demo),() -> {
            sheet.action.setEnabled(false); sheet.action.setText(R.string.form_saving);
            for(Map.Entry<String,TextInputEditText> entry:editors.entrySet()) {
                String key=entry.getKey(),raw=entry.getValue().getText().toString().trim();
                Field field=findField(module,key); String error=validate(field,raw,context);
                if(error!=null){controls.get(key).setError(error);sheet.action.setEnabled(true);sheet.action.setText(R.string.form_save_demo);return;}
                if(DATE.equals(field.kind))draft.put(key,dates.getOrDefault(key,""));
                else if(NUMBER.equals(field.kind))draft.put(key,Double.parseDouble(raw.replace(',','.')));
                else draft.put(key,raw);
            }
            String nameKey=module.equals("finance")?"description":"name";
            if(value(draft,nameKey).isEmpty()){controls.get(nameKey).setError(context.getString(R.string.form_required_error));sheet.action.setEnabled(true);sheet.action.setText(R.string.form_save_demo);return;}
            if(module.equals("finance")) {
                if(date(value(draft,"date"))==null){controls.get("date").setError(context.getString(R.string.demo_date_required));sheet.action.setEnabled(true);sheet.action.setText(R.string.form_save_demo);return;}
                draft.put("currency","ARS"); draft.put("movement_type",selected(type)==0?"income":"expense");
                draft.put("status",new String[]{"posted","pending","overdue"}[selected(status)]);
                draft.putIfAbsent("category",context.getString(R.string.demo_general_category));
            }
            if(module.equals("inventory")) {
                if(number(draft,"capacity")<=0){controls.get("capacity").setError(context.getString(R.string.demo_capacity_error));sheet.action.setEnabled(true);sheet.action.setText(R.string.form_save_demo);return;}
                draft.put("category",category.getText().toString());
                double ratio=number(draft,"quantity")/number(draft,"capacity");
                draft.put("status",ratio<=0.15?"critical":ratio<0.5?"low":"healthy");
            }
            if(module.equals("profile")&&!android.util.Patterns.EMAIL_ADDRESS.matcher(value(draft,"email")).matches()){
                controls.get("email").setError(context.getString(R.string.login_email_invalid));sheet.action.setEnabled(true);sheet.action.setText(R.string.form_save_demo);return;
            }
            try {workspace.records().save(module,draft);sheet.dismiss();Toast.makeText(context,context.getString(R.string.demo_saved),Toast.LENGTH_SHORT).show();}
            catch(IllegalArgumentException error){controls.get(nameKey).setError(error.getMessage());sheet.action.setEnabled(true);sheet.action.setText(R.string.form_save_demo);}
        });
    }

    private static List<Field> fields(String module) {
        List<Field> out=new ArrayList<>();
        if(module.equals("finance")) {
            out.add(new Field("description",R.string.form_concept,TEXT,R.string.form_section_movement));
            out.add(new Field("amount",R.string.form_amount,NUMBER,R.string.form_section_movement));
            out.add(new Field("counterparty",R.string.form_counterparty,TEXT,R.string.form_section_classification));
            out.add(new Field("date",R.string.form_date,DATE,R.string.form_section_movement));
            out.add(new Field("plot",R.string.demo_plot_reference,TEXT,R.string.form_section_classification));
        } else if(module.equals("inventory")) {
            out.add(new Field("name",R.string.form_name,TEXT,R.string.form_section_supply));
            out.add(new Field("quantity",R.string.form_current_quantity,NUMBER,R.string.form_section_stock));
            out.add(new Field("capacity",R.string.form_target_quantity,NUMBER,R.string.form_section_stock));
            out.add(new Field("unit",R.string.form_unit,TEXT,R.string.form_section_stock));
            out.add(new Field("lot",R.string.form_lot,TEXT,R.string.form_section_supply));
            out.add(new Field("expiry",R.string.form_expiry_date,DATE,R.string.form_section_tracking));
            out.add(new Field("depot",R.string.demo_depot,TEXT,R.string.form_section_tracking));
        } else if(module.equals("profile")) {
            out.add(new Field("name",R.string.demo_full_name,TEXT,R.string.demo_account_section));
            out.add(new Field("email",R.string.email_hint,TEXT,R.string.demo_account_section));
            out.add(new Field("role",R.string.demo_role,TEXT,R.string.demo_account_section));
            out.add(new Field("phone",R.string.demo_phone,TEXT,R.string.demo_account_section));
        } else {
            out.add(new Field("name",R.string.demo_note_name,TEXT,R.string.demo_weather_section));
            out.add(new Field("rain",R.string.demo_rain,NUMBER,R.string.demo_weather_section));
        }
        return out;
    }
    private static Field findField(String module,String key){for(Field field:fields(module))if(field.key.equals(key))return field;return fields(module).get(0);}
    private static String validate(Field field,String raw,Context context){
        if(field==null)return null;
        if((field.key.equals("name")||field.key.equals("description")||NUMBER.equals(field.kind))&&raw.isEmpty())return context.getString(R.string.form_required_error);
        if(NUMBER.equals(field.kind)&&!raw.isEmpty())try{double n=Double.parseDouble(raw.replace(',','.'));if(!Double.isFinite(n)||n<0)return context.getString(R.string.form_nonnegative_error);}catch(NumberFormatException e){return context.getString(R.string.form_number_error);}
        return null;
    }
    private static MaterialAutoCompleteTextView choice(MobileUi.Sheet sheet,String title,String[] values,int selected,int icon) {
        TextInputLayout box=(TextInputLayout)LayoutInflater.from(sheet.body.getContext()).inflate(R.layout.item_form_select,sheet.body,false);
        box.setHint(title);box.setStartIconDrawable(icon);
        MaterialAutoCompleteTextView view=box.findViewById(R.id.form_input);
        view.setAdapter(new ArrayAdapter<>(sheet.body.getContext(),android.R.layout.simple_dropdown_item_1line,values));
        view.setText(values[selected],false);view.setTag(selected);
        view.setOnItemClickListener((parent,row,position,id)->view.setTag(position));
        sheet.body.addView(box);return view;
    }
    private static int selected(MaterialAutoCompleteTextView view){try{return Integer.parseInt(String.valueOf(view.getTag()));}catch(Exception ignored){return 0;}}
    private static android.graphics.drawable.Drawable icon(Context context,String key){return androidx.appcompat.content.res.AppCompatResources.getDrawable(context,iconId(key));}
    private static int iconId(String key){
        switch(key){case "date":case "expiry":return R.drawable.st_calendar_month;case "amount":case "counterparty":return R.drawable.st_payments;case "quantity":case "capacity":case "unit":return R.drawable.st_inventory_2;case "rain":return R.drawable.st_water_drop;case "email":return R.drawable.st_mail;default:return R.drawable.st_edit;}
    }
    private static void openDate(Context context,String key,Map<String,String> dates,TextInputEditText edit,TextInputLayout box){
        LocalDate selected=date(dates.getOrDefault(key,""));if(selected==null)selected=LocalDate.now();
        MaterialDatePicker<Long> picker=MaterialDatePicker.Builder.datePicker().setTitleText(context.getString(R.string.form_select_date))
            .setSelection(selected.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()).build();
        picker.addOnPositiveButtonClickListener(value->{String iso=Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate().toString();dates.put(key,iso);edit.setText(dateLabel(iso));box.setError(null);});
        FragmentActivity activity=activity(context);if(activity!=null)picker.show(activity.getSupportFragmentManager(),"demo-date-"+key);
    }
    private static FragmentActivity activity(Context context){Context current=context;while(current instanceof ContextWrapper){if(current instanceof FragmentActivity)return(FragmentActivity)current;current=((ContextWrapper)current).getBaseContext();}return current instanceof FragmentActivity?(FragmentActivity)current:null;}
}
