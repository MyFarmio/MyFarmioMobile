package com.myfarmio.app.ui.main;
import static com.myfarmio.app.ui.common.RecordText.*;
import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.ui.common.*;
import java.util.*;

public class ClimateFragment extends Fragment {
    private MobileUi.Sheet sheet;
    private WorkspaceViewModel workspace;
    private View root;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_climate,parent,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        root=view;
        workspace=new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
        workspace.prepare(SessionManager.getInstance(requireContext()));
        workspace.revision.observe(getViewLifecycleOwner(),revision->render());
        view.findViewById(R.id.climate_register).setOnClickListener(v->{
            if(sheet!=null) sheet.dismiss();
            sheet=new MobileUi.Sheet(requireContext(),"Pluviometría y nota de campo");
            if(workspace.isDemo()) StitchDemoEditor.bind(sheet,workspace,"rain_notes",Collections.emptyMap());
            else { sheet.row("Sin conexión a una estación","No hay una estación disponible para registrar esta lectura."); sheet.primary("Entendido",sheet::dismiss); }
            sheet.show();
        });
    }
    private void render() {
        Map<String,Object> data=StitchUi.demo(workspace,"weather");
        int[] ids={R.id.climate_temperature,R.id.climate_condition,R.id.climate_updated,R.id.climate_value_0,R.id.climate_value_1,
            R.id.climate_value_2,R.id.climate_value_3,R.id.climate_rain,R.id.climate_etp,R.id.climate_water_legend,
            R.id.climate_plot,R.id.climate_crop,R.id.climate_stage,R.id.climate_recommendation};
        String[] keys={"temperature","condition","updated","moisture","wind","pressure","uv","rain","etp","water_legend","plot","crop","stage","recommendation"};
        for(int i=0;i<ids.length;i++) MobileUi.text(root,ids[i],or(data,keys[i],"Sin lectura disponible"));
        MobileUi.text(root,R.id.climate_location,workspace.isDemo()?"EJEMPLO LOCAL · LOTE NORTE":"SIN ESTACIÓN");
        StitchUi.progress(root,R.id.climate_water_progress,number(data,"water_percent"));
        LinearLayout windows=root.findViewById(R.id.climate_windows); windows.removeAllViews();
        LinearLayout alerts=root.findViewById(R.id.climate_alerts); alerts.removeAllViews();
        if(workspace.isDemo()) {
            for(Map<String,Object> item:workspace.records().list("windows")) {
                View row=getLayoutInflater().inflate(R.layout.item_stitch_window,windows,false);
                MobileUi.text(row,R.id.window_title,value(item,"name")); MobileUi.text(row,R.id.window_detail,value(item,"detail"));
                StitchUi.badge(row,R.id.window_status,value(item,"status"),value(item,"status").equals("PRECAUCIÓN"),false);
                android.widget.ImageView icon=(android.widget.ImageView)((android.view.ViewGroup)((android.view.ViewGroup)row).getChildAt(0)).getChildAt(0);
                icon.setImageResource(value(item,"icon").equals("grass")?R.drawable.st_grass:value(item,"icon").equals("local_shipping")?R.drawable.st_local_shipping:R.drawable.st_agriculture);
                windows.addView(row);
            }
            for(Map<String,Object> item:workspace.records().list("climate_alerts")) {
                View row=getLayoutInflater().inflate(R.layout.item_stitch_climate_alert,alerts,false);
                MobileUi.text(row,R.id.alert_title,value(item,"name")); MobileUi.text(row,R.id.alert_time,value(item,"time")); MobileUi.text(row,R.id.alert_body,value(item,"detail")); alerts.addView(row);
            }
        } else {
            empty(windows,"No hay ventanas operativas disponibles."); empty(alerts,"Sin alertas climáticas consultadas.");
        }
        MobileUi.text(root,R.id.climate_note,workspace.isDemo()?"Demo local · "+workspace.records().list("rain_notes").size()+" notas guardadas durante esta sesión. No son mediciones de tu campo.":"Agregá una estación desde la web para consultar tus lecturas.");
        // TODO: Conectar lecturas, ventanas y pluviometría sin mezclar la demo con datos reales.
    }
    private void empty(LinearLayout parent,String message) {
        android.widget.TextView text=new android.widget.TextView(requireContext()); text.setTextAppearance(R.style.StitchBody); text.setText(message); text.setPadding(0,16,0,0); parent.addView(text);
    }
    @Override public void onDestroyView() { if(sheet!=null) { sheet.dismiss(); sheet=null; } root=null; super.onDestroyView(); }
}

