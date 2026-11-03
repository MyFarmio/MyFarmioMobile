package com.myfarmio.app.ui.common;

import static com.myfarmio.app.ui.common.RecordText.*;
import static com.myfarmio.app.ui.common.RecordFormSchema.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.datepicker.MaterialDatePicker;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.myfarmio.app.R;
import com.myfarmio.app.data.demo.DemoRepository;
import com.myfarmio.app.data.demo.RecordRepository;
import com.myfarmio.app.data.SupabaseRecordRepository;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/** Owns an isolated edit draft and commits through the repository's demo or Supabase boundary. */
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
    private boolean restoredDraft;
    public void markRestored() { restoredDraft = true; }
    public Map<String,Object> snapshot() {
        Map<String,Object> draft=DemoRepository.copy(original);
        for (Map.Entry<Field,TextInputLayout> item:controls.entrySet())
            draft.put(item.getKey().key,read(item.getKey(),item.getValue()));
        return draft;
    }
    public RecordEditor(MobileUi.Sheet sheet, String module, Map<String,Object> record, RecordRepository repository, Runnable saved) {
        this.sheet=sheet; this.module=module; this.repository=repository; this.saved=saved;
        original = DemoRepository.copy(record);
        if (!(repository instanceof SupabaseRecordRepository)) sheet.row(sheet.body.getContext().getString(R.string.form_demo_title), sheet.body.getContext().getString(R.string.form_demo_local));
        int section = 0;
        for (Field field : fields(module)) {
            if ("inventory".equals(module) && !record.isEmpty() && "current_quantity".equals(field.key)) continue;
            if (section != field.sectionRes) { section=field.sectionRes; sheet.heading(sheet.body.getContext().getString(section)); }
            add(field);
        }
        if ("livestock".equals(module)) sheet.row(sheet.body.getContext().getString(R.string.form_quantity_weight), sheet.body.getContext().getString(R.string.form_livestock_demo_note));
        sheet.primary(repository instanceof SupabaseRecordRepository ? sheet.body.getContext().getString(R.string.form_save) : sheet.body.getContext().getString(R.string.form_save_demo), this::save);
        sheet.guardClose(this::requestClose);
    }
    private void add(Field field) {
        Context context = sheet.body.getContext();
        TextInputLayout layout = (TextInputLayout) LayoutInflater.from(context).inflate(
            SELECT.equals(field.kind) ? R.layout.item_form_select : R.layout.item_form_field, sheet.body, false);
        String label=context.getString(field.labelRes);
        layout.setHint(label + (field.required ? context.getString(R.string.form_required_suffix) : ""));
        layout.setStartIconDrawable(iconFor(field.key));
        EditText input = layout.findViewById(R.id.form_input);
        String raw = value(original,field.key);
        if (SELECT.equals(field.kind)) {
            MaterialAutoCompleteTextView select=(MaterialAutoCompleteTextView) input;
            List<String> keys=new ArrayList<>(), labels=new ArrayList<>();
            if (!field.required || field.choices.length == 0) { keys.add(""); labels.add(field.required ? context.getString(R.string.form_select_herd) : context.getString(R.string.form_unassigned)); }
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
            select.setOnItemClickListener((parent,view,position,id) -> { select.setTag(keys.get(position)); layout.setError(null); });
            optionKeys.put(field.key,keys);
        } else if (DATE.equals(field.kind)) {
            dates.put(field.key,raw);
            input.setText(raw.isEmpty() ? "" : dateLabel(raw));
            input.setFocusable(false); input.setClickable(true);
            input.setContentDescription(context.getString(R.string.form_date_select_accessibility,label));
            input.setOnClickListener(v -> {
                LocalDate picked=date(dates.get(field.key));
                LocalDate selected=picked==null?LocalDate.now():picked;
                MaterialDatePicker.Builder<Long> builder=MaterialDatePicker.Builder.datePicker()
                    .setTitleText(context.getString(R.string.form_select_date));
                builder.setSelection(selected.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
                MaterialDatePicker<Long> picker=builder.build();
                picker.addOnPositiveButtonClickListener(selection -> {
                    String next=Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate().toString();
                    dates.put(field.key,next); input.setText(dateLabel(next)); layout.setError(null);
                });
                FragmentActivity activity=activity(context);
                if (activity != null) picker.show(activity.getSupportFragmentManager(), "form-date-"+field.key);
            });
        } else {
            input.setText(raw);
            if (NUMBER.equals(field.kind)||PERCENT.equals(field.kind)) input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            else if (MULTI.equals(field.kind)) { input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE); input.setMinLines(2); input.setMaxLines(5); }
            else { input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); input.setSingleLine(true); }
        }
        sheet.body.addView(layout);
        controls.put(field,layout);
        initial.put(field.key,read(field,layout));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (layout.getError()==null) return;
                String current=s.toString().trim();
                String error=validate(field,current,context);
                layout.setError(error);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
    private android.graphics.drawable.Drawable iconFor(String key) {
        int id;
        switch (key) {
            case "date": case "due_date": case "start_date": case "birth_date": case "expiry_date": id=R.drawable.st_calendar_month; break;
            case "herd_id": case "category": case "health_status": case "reproductive_status": id=R.drawable.st_pets; break;
            case "amount": case "movement_type": case "payment_method": id=R.drawable.st_payments; break;
            case "area_hectares": case "current_crop": case "crop_stage": case "soil_moisture_percent": id=R.drawable.st_grass; break;
            case "current_quantity": case "target_quantity": case "reorder_point": case "unit": id=R.drawable.st_inventory_2; break;
            case "status": case "priority": case "progress": id=R.drawable.st_task_alt; break;
            case "notes": case "observations": case "description": id=R.drawable.st_description; break;
            default: id=R.drawable.st_edit;
        }
        return androidx.appcompat.content.res.AppCompatResources.getDrawable(sheet.body.getContext(),id);
    }
    private FragmentActivity activity(Context context) {
        Context current=context;
        while (current instanceof ContextWrapper) {
            if (current instanceof FragmentActivity) return (FragmentActivity) current;
            current=((ContextWrapper)current).getBaseContext();
        }
        return current instanceof FragmentActivity ? (FragmentActivity)current : null;
    }
    private String validate(Field field,String raw,Context context) {
        if (field.required && raw.isEmpty()) return context.getString(R.string.form_required_error);
        if (!raw.isEmpty() && (NUMBER.equals(field.kind)||PERCENT.equals(field.kind))) {
            try {
                double value=Double.parseDouble(raw.replace(',','.'));
                if (!Double.isFinite(value)||value<0) return context.getString(R.string.form_nonnegative_error);
                if (PERCENT.equals(field.kind)&&value>100) return context.getString(R.string.form_percent_error);
            } catch (NumberFormatException error) { return context.getString(R.string.form_number_error); }
        }
        return null;
    }
    private String read(Field field,TextInputLayout layout) {
        EditText input=layout.findViewById(R.id.form_input);
        if (SELECT.equals(field.kind)) return String.valueOf(input.getTag());
        if (DATE.equals(field.kind)) return dates.get(field.key);
        return input.getText().toString().trim();
    }
    private void requestClose() {
        boolean dirty=restoredDraft;
        for (Map.Entry<Field,TextInputLayout> item:controls.entrySet())
            if (!Objects.equals(initial.get(item.getKey().key),read(item.getKey(),item.getValue()))) dirty=true;
        if (!dirty) { sheet.dismiss(); return; }
        Context context=sheet.body.getContext();
        new MaterialAlertDialogBuilder(context).setTitle(R.string.form_discard_title)
            .setMessage(R.string.form_discard_message)
            .setNegativeButton(R.string.form_keep_editing,null).setPositiveButton(R.string.form_discard,(d,w)->sheet.dismiss()).show();
    }
    private void save() {
        Map<String,Object> draft=DemoRepository.copy(original);
        TextInputLayout first=null;
        for (Map.Entry<Field,TextInputLayout> item:controls.entrySet()) {
            Field field=item.getKey(); TextInputLayout layout=item.getValue(); String raw=read(field,layout);
            layout.setError(null);
            String error=validate(field,raw,sheet.body.getContext());
            if (!raw.isEmpty() && (NUMBER.equals(field.kind)||PERCENT.equals(field.kind))) {
                try {
                    double number=Double.parseDouble(raw.replace(',','.'));
                    if (error!=null) { /* keep the localized validation message */ }
                    else draft.put(field.key,number);
                } catch (NumberFormatException e) { error=sheet.body.getContext().getString(R.string.form_number_error); }
            } else if (raw.isEmpty()) {
                if (!original.isEmpty() && original.containsKey(field.key)) draft.put(field.key, null);
                else draft.remove(field.key);
            } else draft.put(field.key,raw);
            if (error!=null) { layout.setError(error); if (first==null) first=layout; }
        }
        if (first!=null) {
            first.requestFocus(); first.requestRectangleOnScreen(new android.graphics.Rect(0,0,first.getWidth(),first.getHeight()),false);
            first.announceForAccessibility(sheet.body.getContext().getString(R.string.form_review_fields));
            return;
        }
        sheet.action.setEnabled(false);
        sheet.action.setText(R.string.form_saving);
        repository.saveAsync(module, draft, (savedRecord, error) -> {
            if (error == null) { sheet.dismiss(); saved.run(); return; }
            sheet.action.setEnabled(true);
            sheet.action.setText(repository instanceof SupabaseRecordRepository ? R.string.form_save : R.string.form_save_demo);
            new MaterialAlertDialogBuilder(sheet.body.getContext()).setTitle(R.string.form_save_error).setMessage(error).setPositiveButton(R.string.form_review,null).show();
        });
    }
}
