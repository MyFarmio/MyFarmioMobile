package com.myfarmio.app.ui.common;
import static com.myfarmio.app.ui.common.RecordText.*;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.myfarmio.app.R;
import com.myfarmio.app.data.weather.WeatherViewModel;
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
    public static void weather(View root, WeatherViewModel.State state) {
        boolean hasData = state != null && !"—".equals(state.temperature);
        text(root,R.id.weather_station,hasData?state.location:root.getContext().getString(R.string.climate_dashboard_label));
        text(root,R.id.weather_updated,hasData?state.updated:root.getContext().getString(R.string.climate_dashboard_tap));
        text(root,R.id.weather_status,state == null ? root.getContext().getString(R.string.climate_status_no_data) : state.loading ? root.getContext().getString(R.string.climate_status_loading) : state.error == null ? state.condition.toUpperCase() : root.getContext().getString(R.string.climate_status_error));
        text(root,R.id.weather_value_0,hasData?state.temperature:"—");
        text(root,R.id.weather_detail_0,hasData?state.condition:root.getContext().getString(R.string.climate_no_forecast));
        text(root,R.id.weather_value_1,hasData?state.humidity:"—");
        text(root,R.id.weather_detail_1,hasData?root.getContext().getString(R.string.climate_air_humidity):root.getContext().getString(R.string.climate_no_reading));
        text(root,R.id.weather_value_2,hasData?state.wind:"—");
        text(root,R.id.weather_detail_2,hasData?root.getContext().getString(R.string.climate_wind):root.getContext().getString(R.string.climate_no_reading));
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

