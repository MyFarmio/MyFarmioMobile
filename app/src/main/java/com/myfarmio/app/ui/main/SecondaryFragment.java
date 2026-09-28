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
import java.util.Map;

/** Inventory is preparation-only. Finance uses the existing read endpoint, never mock totals. */
public class SecondaryFragment extends Fragment {
    private View root;
    private CollectionViewModel model;
    private WorkspaceViewModel workspace;
    private boolean finance;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_secondary,parent,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved) {
        root=view; finance="finance".equals(requireArguments().getString("module"));
        workspace=new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
        workspace.prepare(SessionManager.getInstance(requireContext()));
        model=new ViewModelProvider(this).get(CollectionViewModel.class);
        MobileUi.text(view,R.id.secondary_title,finance ? "Finanzas" : "Inventario");
        MobileUi.text(view,R.id.secondary_description,finance
            ? "Registros financieros de tu organización, en una vista de consulta."
            : "Un espacio para consultar insumos y existencias cuando su servicio esté disponible en Android.");
        model.state.observe(getViewLifecycleOwner(),state->render());
        workspace.revision.observe(getViewLifecycleOwner(),revision->{ render(); if (finance&&!workspace.isDemo()) reload(); });
    }
    private void reload() { model.load("finance",SessionManager.getInstance(requireContext()).getOrganizationId(),true); }
    private void render() {
        if (root==null) return;
        LinearLayout rows=root.findViewById(R.id.secondary_records); rows.removeAllViews();
        CollectionViewModel.State state=model.state.getValue();
        root.findViewById(R.id.secondary_loading).setVisibility(finance&&!workspace.isDemo()&&state.loading?View.VISIBLE:View.GONE);
        if (!finance||workspace.isDemo()) {
            MobileUi.state(root,finance ? "Sin movimientos de demostración" : "Inventario en preparación",
                finance ? "La demo no incluye movimientos financieros. No se calculan saldos ni se simulan operaciones. Con una organización activa se utiliza la consulta real existente."
                    : "Todavía no existe una consulta móvil de inventario conectada. No se muestran existencias ficticias. La gestión sigue disponible en la web.",null,null);
            return;
        }
        if (state.error!=null) MobileUi.state(root,"No pudimos cargar los movimientos",state.error,"Reintentar",this::reload);
        else if (state.rows.isEmpty()) MobileUi.state(root,state.loading?"Consultando registros…":"Sin registros visibles",
            state.loading?"Estamos consultando los datos accesibles de tu organización.":"No hay movimientos en la consulta actual. Las altas y los reportes se gestionan en la web.",
            state.loading?null:"Actualizar",state.loading?null:this::reload);
        else root.findViewById(R.id.mobile_state).setVisibility(View.GONE);
        for (Map<String,Object> row:state.rows) {
            View card=getLayoutInflater().inflate(R.layout.item_mobile_detail,rows,false);
            MobileUi.text(card,R.id.detail_label,or(row,"description","Registro financiero"));
            MobileUi.text(card,R.id.detail_value,value(row,"amount")+" "+value(row,"currency")+" · "+label(value(row,"status")));
            rows.addView(card);
        }
    }
    @Override public void onDestroyView() { root=null; super.onDestroyView(); }
}
