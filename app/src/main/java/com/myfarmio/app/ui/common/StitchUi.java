package com.myfarmio.app.ui.common;
import static com.myfarmio.app.ui.common.RecordText.*;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.myfarmio.app.R;
import java.util.*;

/** XML binders only; fictional data is isolated in the demo repository. */
public final class StitchUi {
    private StitchUi() {}
    public static Map<String,Object> demo(WorkspaceViewModel workspace,String module) {
        if (!workspace.isDemo()) return Collections.emptyMap();
        List<Map<String,Object>> rows=workspace.records().list(module);
        return rows.isEmpty()?Collections.emptyMap():rows.get(0);
    }
    public static void text(View root,int id,String value) { MobileUi.text(root,id,value); }
    public static void badge(View root,int id,String text,boolean caution,boolean error) {
        TextView view=root.findViewById(id); view.setText(text);
        view.setBackgroundResource(error?R.drawable.stitch_badge_error:caution?R.drawable.stitch_badge_caution:R.drawable.stitch_badge_success);
        view.setTextColor(root.getContext().getColor(error?R.color.error:caution?R.color.secondary:R.color.stitch_primary));
    }
    public static void progress(View root,int id,double value) {
        ((ProgressBar)root.findViewById(id)).setProgress((int)Math.max(0,Math.min(100,value)));
    }
    public static void weather(View root,WorkspaceViewModel workspace) {
        Map<String,Object> data=demo(workspace,"weather");
        text(root,R.id.weather_station,or(data,"name","Sin estación configurada"));
        text(root,R.id.weather_updated,workspace.isDemo()?"Ejemplo local":"Sin lectura");
        text(root,R.id.weather_status,workspace.isDemo()?"ÓPTIMO":"SIN DATOS");
        int[] values={R.id.weather_value_0,R.id.weather_value_1,R.id.weather_value_2};
        String[] keys={"temperature","moisture","wind"};
        int[] details={R.id.weather_detail_0,R.id.weather_detail_1,R.id.weather_detail_2};
        String[] descriptions={"Soleado","En rango","Calmo (NO)"};
        for(int i=0;i<3;i++) { text(root,values[i],or(data,keys[i],"—")); text(root,details[i],workspace.isDemo()?descriptions[i]:"Sin lectura"); }
    }
    public static void plot(View card,Map<String,Object> row) {
        text(card,R.id.record_title,or(row,"name","Sin nombre"));
        text(card,R.id.record_zone,or(row,"zone","Zona sin registrar").toUpperCase());
        text(card,R.id.record_subtitle,or(row,"current_crop","Sin cultivo")+" · "+amount(number(row,"area_hectares"))+" ha");
        badge(card,R.id.record_status,label(value(row,"status")).toUpperCase(),attention(value(row,"status")),false);
        text(card,R.id.record_progress_label,row.containsKey("progress")?amount(number(row,"progress"))+"%":"Sin registrar");
        progress(card,R.id.record_progress,number(row,"progress"));
        text(card,R.id.record_metric_one,or(row,"crop_stage","—"));
        text(card,R.id.record_metric_two,row.containsKey("soil_moisture_percent")?amount(number(row,"soil_moisture_percent"))+"%":"—");
        text(card,R.id.record_metric_three,or(row,"estimated_yield","—"));
    }
    public static void animal(View card,Map<String,Object> row) {
        text(card,R.id.record_title,or(row,"tag","Sin caravana"));
        text(card,R.id.record_category,or(row,"category","Animal").toUpperCase());
        String state=value(row,"health_status");
        badge(card,R.id.record_status,label(state).toUpperCase(),attention(state)||state.equals("treatment"),state.equals("critical"));
        text(card,R.id.record_metric_one,label(value(row,"reproductive_status")));
        text(card,R.id.record_metric_two,row.containsKey("weight_kg")?amount(number(row,"weight_kg"))+" kg":"—");
        text(card,R.id.record_metric_three,label(state));
    }
}

