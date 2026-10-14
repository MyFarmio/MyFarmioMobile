package com.myfarmio.app.ui.main;
import static com.myfarmio.app.ui.common.RecordText.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.ui.common.*;
import java.util.*;

/** Presentation keeps the existing finance read endpoint; all edits are explicit local demo edits. */
public class SecondaryFragment extends Fragment {
    private View root;
    private CollectionViewModel model;
    private WorkspaceViewModel workspace;
    private MobileUi.Sheet sheet;
    private boolean finance;
    private String filter="",period="";
    private List<Map<String,Object>> reportRows=Collections.emptyList();
    private final ActivityResultLauncher<String> exportPdf=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),uri->{
        if(uri==null||!isAdded()||workspace==null||!workspace.isDemo()) return;
        android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();
        try(java.io.OutputStream output=requireContext().getContentResolver().openOutputStream(uri)) {
            android.graphics.Paint paint=new android.graphics.Paint(); paint.setTextSize(12);
            int index=0,pageNumber=1;
            do {
                android.graphics.pdf.PdfDocument.Page page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNumber++).create());
                android.graphics.Canvas canvas=page.getCanvas();
                paint.setTextSize(18); canvas.drawText("MyFarmio · Reporte de demostración",36,48,paint);
                paint.setTextSize(11); canvas.drawText("Datos ficticios locales. No es un comprobante fiscal.",36,70,paint);
                int y=104;
                for(int n=0;n<18&&index<reportRows.size();n++,index++) {
                    Map<String,Object> record=reportRows.get(index);
                    String name=or(record,"description","Movimiento");
                    canvas.drawText(name.length()>65?name.substring(0,62)+"…":name,36,y,paint);
                    canvas.drawText(value(record,"date")+" · "+value(record,"movement_type")+" · ARS "+amount(number(record,"amount")),36,y+16,paint); y+=38;
                }
                pdf.finishPage(page);
            } while(index<reportRows.size());
            if(output==null) throw new java.io.IOException("No se pudo abrir el destino.");
            pdf.writeTo(output);
            Toast.makeText(requireContext(),"PDF local guardado",Toast.LENGTH_SHORT).show();
        } catch(Exception error) { info("No se pudo exportar","Elegí otra ubicación e intentá nuevamente."); }
        finally { pdf.close(); }
    });
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state) { return inflater.inflate(R.layout.fragment_secondary,parent,false); }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved) {
        root=view; finance="finance".equals(requireArguments().getString("module"));
        if(saved!=null) { filter=saved.getString("filter",""); period=saved.getString("period",""); }
        workspace=new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class); workspace.prepare(SessionManager.getInstance(requireContext()));
        model=new ViewModelProvider(this).get(CollectionViewModel.class);
        MobileUi.text(view,R.id.secondary_eyebrow,finance?"TESORERÍA Y CONTROL":"OPERACIONES CENTRALES");
        MobileUi.text(view,R.id.secondary_title,finance?"Gestión de finanzas":"Control de inventario");
        MobileUi.text(view,R.id.secondary_description,finance?"Caja, márgenes y compromisos del negocio.":"Insumos, sanidad, combustible y reposición programada.");
        view.findViewById(R.id.finance_content).setVisibility(finance?View.VISIBLE:View.GONE);
        view.findViewById(R.id.inventory_content).setVisibility(finance?View.GONE:View.VISIBLE);
        view.findViewById(R.id.secondary_filter).setOnClickListener(v->filters());
        view.findViewById(R.id.finance_period).setOnClickListener(v->periods());
        view.findViewById(R.id.secondary_export).setOnClickListener(v->export());
        view.findViewById(R.id.finance_all).setOnClickListener(v->history());
        view.findViewById(R.id.finance_history).setOnClickListener(v->history());
        view.findViewById(R.id.finance_accounts).setOnClickListener(v->info("Detalle de cuentas",workspace.isDemo()?"Caja demo en ARS. El saldo disponible incluye únicamente ingresos y gastos cobrados o pagados.":"El detalle por cuenta no está disponible en esta consulta."));
        view.findViewById(R.id.inventory_distribution).setOnClickListener(v->info("Distribución de almacén",or(StitchUi.demo(workspace,"inventory"),"depot","No hay ubicaciones de almacén disponibles.")));
        int[] categoryIds={R.id.category_seeds,R.id.category_fertilizer,R.id.category_health,R.id.category_fuel};
        String[] categories={"Semillas","Fertilizantes","Sanidad animal","Combustible"};
        for(int i=0;i<categoryIds.length;i++) { final String category=categories[i]; view.findViewById(categoryIds[i]).setOnClickListener(v->category(category)); }
        model.state.observe(getViewLifecycleOwner(),state->render());
        workspace.revision.observe(getViewLifecycleOwner(),revision->{render(); if(finance&&!workspace.isDemo())reload();});
        // TODO: Conectar inventario, reportes y escrituras reales manteniendo el repositorio local separado.
    }
    private void reload() { model.load("finance",SessionManager.getInstance(requireContext()).getOrganizationId(),true); }
    private List<Map<String,Object>> rows() { return workspace.isDemo()?workspace.records().list(finance?"finance":"inventory"):model.state.getValue()==null?Collections.emptyList():model.state.getValue().rows; }
    private MobileUi.Sheet open(String title) { if(sheet!=null)sheet.dismiss(); sheet=new MobileUi.Sheet(requireContext(),title); return sheet; }
    private void info(String title,String message) { MobileUi.Sheet current=open(title); current.row("MyFarmio",message); current.primary("Entendido",current::dismiss); current.show(); }
    public void quickCreate() {
        if(!workspace.isDemo()) { info("Solo consulta","Podés crear registros de tu organización desde la web."); return; }
        edit(Collections.emptyMap());
    }
    private void edit(Map<String,Object> row) { MobileUi.Sheet current=open((row.isEmpty()?"Nuevo":"Editar")+(finance?" movimiento":" insumo")); StitchDemoEditor.bind(current,workspace,finance?"finance":"inventory",row); current.show(); }
    private void detail(Map<String,Object> row) {
        MobileUi.Sheet current=open(or(row,finance?"description":"name","Registro"));
        if(finance) { current.row("Importe",value(row,"currency")+" "+amount(number(row,"amount"))); current.row("Fecha",dateLabel(value(row,"date"))); current.row("Estado",value(row,"status")); }
        else { current.row("Existencias",amount(number(row,"quantity"))+" "+value(row,"unit")); current.row("Lote",value(row,"lot")); current.row("Ubicación",value(row,"depot")); }
        if(workspace.isDemo()) {
            current.primary("Editar en demo",()->edit(row));
            current.danger("Eliminar de la demo",()->new MaterialAlertDialogBuilder(requireContext()).setTitle("¿Eliminar este registro local?")
                .setMessage("Esta acción no afecta a tu organización.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{
                    workspace.records().delete(finance?"finance":"inventory",value(row,"id")); current.dismiss();
                }).show());
        }
        current.show();
    }
    private void filters() {
        MobileUi.Sheet current=open("Filtrar movimientos");
        String[] names={"Todos","Ingresos","Gastos"},values={"","income","expense"};
        for(int i=0;i<names.length;i++) { final String value=values[i]; current.button(names[i],()->{filter=value;current.dismiss();render();}); }
        current.show();
    }
    private void periods() {
        MobileUi.Sheet current=open("Período");
        current.button("Todos los períodos",()->{period="";current.dismiss();render();});
        Set<String> dates=new TreeSet<>(Collections.reverseOrder());
        for(Map<String,Object> row:rows()) { String date=value(row,"date"); if(date.length()>=7) dates.add(date.substring(0,7)); }
        for(String date:dates) current.button(date,()->{period=date;current.dismiss();render();});
        current.show();
    }
    private void history() {
        MobileUi.Sheet current=open("Histórico de movimientos");
        for(Map<String,Object> row:rows()) current.button(or(row,"description","Movimiento")+" · "+value(row,"currency")+" "+amount(number(row,"amount")),()->detail(row));
        if(rows().isEmpty()) current.row("Sin movimientos","No hay registros para mostrar.");
        current.show();
    }
    private void category(String category) {
        MobileUi.Sheet current=open(category); int count=0;
        if(workspace.isDemo()) for(Map<String,Object> row:rows()) if(category.equals(value(row,"category"))) { count++; current.button(value(row,"name")+" · "+amount(number(row,"quantity"))+" "+value(row,"unit"),()->detail(row)); }
        if(count==0) current.row("Sin insumos",workspace.isDemo()?"Todavía no hay insumos en esta categoría.":"No hay existencias disponibles.");
        if(workspace.isDemo()) current.primary("Nuevo insumo",this::quickCreate);
        current.show();
    }
    private void render() {
        if(root==null||model==null)return;
        CollectionViewModel.State state=model.state.getValue(); if(state==null)return;
        boolean demo=workspace.isDemo();
        root.findViewById(R.id.secondary_loading).setVisibility(finance&&!demo&&state.loading?View.VISIBLE:View.GONE);
        root.findViewById(R.id.mobile_state).setVisibility(View.GONE);
        if(!finance) { renderInventory(); return; }
        List<Map<String,Object>> all=new ArrayList<>();
        for(Map<String,Object> row:rows()) if(period.isEmpty()||value(row,"date").startsWith(period))all.add(row);
        boolean reliable=!all.isEmpty(),mixed=false; String currency="";
        double income=0,expense=0,cash=0,receivable=0,committed=0; int pending=0;
        for(Map<String,Object> row:all) {
            String found=value(row,"currency"); if(found.isEmpty()||(!currency.isEmpty()&&!currency.equals(found)))mixed=true; if(!found.isEmpty())currency=found;
            double amount=number(row,"amount"); boolean incoming="income".equals(value(row,"movement_type")), posted="posted".equals(value(row,"status"));
            if(incoming)income+=amount; else if("expense".equals(value(row,"movement_type")))expense+=amount;
            if(posted)cash+=incoming?amount:-amount; else { pending++; if(incoming)receivable+=amount;else committed+=amount; }
        }
        reliable=reliable&&!mixed; String unit=currency+" ";
        MobileUi.text(root,R.id.secondary_hero_value,reliable?unit+amount(cash):"—");
        MobileUi.text(root,R.id.secondary_hero_detail,reliable?(cash>=0?"Superávit operativo":"Saldo negativo"):"Sin saldo disponible");
        MobileUi.text(root,R.id.finance_active,reliable?"Activo":"Sin datos");
        MobileUi.text(root,R.id.finance_metric_1,reliable?unit+amount(income-expense):"—");
        MobileUi.text(root,R.id.finance_metric_2,reliable?unit+amount(committed):"—");
        MobileUi.text(root,R.id.finance_metric_1_detail,reliable&&income>0?amount(100*(income-expense)/income)+"% de ventas":"Sin ingresos");
        MobileUi.text(root,R.id.finance_metric_2_detail,pending+" pagos pendientes");
        MobileUi.text(root,R.id.finance_receivable,reliable?unit+amount(receivable):"—");
        MobileUi.text(root,R.id.finance_receivable_status,receivable>0?"Pendiente":reliable?"Al día":"Sin datos");
        MobileUi.text(root,R.id.finance_activity_share,demo?"Agrícola":"Sin desglose");
        MobileUi.text(root,R.id.finance_activity,demo?"Agricultura":"No disponible");
        MobileUi.text(root,R.id.finance_margin,demo&&reliable?unit+amount(income-expense):"—");
        StitchUi.progress(root,R.id.finance_margin_progress,demo&&reliable?100:0);
        MobileUi.text(root,R.id.finance_budget_count,reliable?(income>=expense?"0 ALERTAS":"1 ALERTA"):"—");
        MobileUi.text(root,R.id.finance_budget_detail,reliable?(income>=expense?"Sin desvíos críticos en el período.":"Los gastos superan a los ingresos."): "Sin información disponible.");
        MobileUi.text(root,R.id.finance_period,period.isEmpty()?"Período: todos":period);
        LinearLayout list=root.findViewById(R.id.secondary_records);list.removeAllViews();
        int shown=0;
        for(Map<String,Object> row:all) {
            if(!filter.isEmpty()&&!filter.equals(value(row,"movement_type")))continue;
            if(shown++>=5)continue;
            View card=getLayoutInflater().inflate(R.layout.item_stitch_transaction,list,false);
            boolean incoming="income".equals(value(row,"movement_type"));
            MobileUi.text(card,R.id.transaction_name,or(row,"description","Movimiento"));
            MobileUi.text(card,R.id.transaction_amount,(incoming?"+ ":"− ")+value(row,"currency")+" "+amount(number(row,"amount")));
            ((TextView)card.findViewById(R.id.transaction_amount)).setTextColor(requireContext().getColor(incoming?R.color.stitch_primary:R.color.secondary));
            MobileUi.text(card,R.id.transaction_detail,or(row,"counterparty","Contraparte sin registrar")+" · "+dateLabel(value(row,"date")));
            MobileUi.text(card,R.id.transaction_plot,or(row,"plot",value(row,"category")));
            StitchUi.badge(card,R.id.transaction_type,incoming?"INGRESO":"GASTO",!incoming,false);
            String status=value(row,"status");
            StitchUi.badge(card,R.id.transaction_status,"posted".equals(status)?incoming?"COBRADO":"PAGADO":"overdue".equals(status)?"VENCIDO":"PENDIENTE",!status.equals("posted"),status.equals("overdue"));
            card.setOnClickListener(v->detail(row));list.addView(card);
        }
        if(shown==0)empty(list,filter.isEmpty()?"No hay movimientos en este período.":"Sin movimientos para este filtro.");
        LinearLayout reports=root.findViewById(R.id.finance_reports);reports.removeAllViews();
        MobileUi.text(root,R.id.finance_report_count,demo&&!all.isEmpty()?"1 LOCAL":"0 generados");
        if(demo&&!all.isEmpty()) {
            View card=getLayoutInflater().inflate(R.layout.item_stitch_report,reports,false);
            MobileUi.text(card,R.id.report_name,"Establecimiento principal");
            MobileUi.text(card,R.id.report_period,"Resumen local · "+(period.isEmpty()?"todos los períodos":period));
            MobileUi.text(card,R.id.report_balance,"Balance: "+unit+amount(income-expense)+" · "+all.size()+" registros");
            card.findViewById(R.id.report_download).setOnClickListener(v->export());
            card.findViewById(R.id.report_view).setOnClickListener(v->history()); reports.addView(card);
        } else empty(reports,"No hay reportes disponibles.");
        if(!demo&&state.error!=null)MobileUi.state(root,"No pudimos cargar los movimientos",state.error,"Reintentar",this::reload);
        else if(!demo&&!state.loading&&all.isEmpty())MobileUi.state(root,"Sin registros visibles","No hay movimientos disponibles para tu organización.","Actualizar",this::reload);
    }
    private void renderInventory() {
        boolean demo=workspace.isDemo(); List<Map<String,Object>> all=demo?rows():Collections.emptyList();
        int[] ids={R.id.category_seeds_count,R.id.category_fertilizer_count,R.id.category_health_count,R.id.category_fuel_count};
        String[] categories={"Semillas","Fertilizantes","Sanidad animal","Combustible"};
        for(int i=0;i<ids.length;i++) { int count=0; for(Map<String,Object> row:all)if(categories[i].equals(value(row,"category")))count++; MobileUi.text(root,ids[i],demo?String.valueOf(count):"—"); }
        LinearLayout supplies=root.findViewById(R.id.inventory_records), history=root.findViewById(R.id.inventory_history); supplies.removeAllViews();history.removeAllViews();
        int alerts=0;
        for(Map<String,Object> row:all) {
            double ratio=number(row,"capacity")>0?number(row,"quantity")/number(row,"capacity"):0;
            if(ratio<0.5) {
                alerts++;View card=getLayoutInflater().inflate(R.layout.item_stitch_supply,supplies,false);
                StitchUi.badge(card,R.id.supply_status,ratio<=0.15?"CRÍTICO":"BAJO",true,ratio<=0.15);
                MobileUi.text(card,R.id.supply_lot,"Lote "+value(row,"lot")+" · Vence "+value(row,"expiry"));
                MobileUi.text(card,R.id.supply_name,value(row,"name"));
                MobileUi.text(card,R.id.supply_quantity,amount(number(row,"quantity"))+" / "+amount(number(row,"capacity"))+" "+value(row,"unit"));
                MobileUi.text(card,R.id.supply_ratio,amount(ratio*100)+"% disponible");
                StitchUi.progress(card,R.id.supply_progress,ratio*100);
                ((ProgressBar)card.findViewById(R.id.supply_progress)).setProgressTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(ratio<=0.15?R.color.error:R.color.secondary)));
                card.setOnClickListener(v->detail(row));supplies.addView(card);
            }
        }
        if(demo) for(Map<String,Object> movement:workspace.records().list("inventory_movements")) {
            View card=getLayoutInflater().inflate(R.layout.item_stitch_stock_movement,history,false);
            MobileUi.text(card,R.id.stock_name,value(movement,"name"));
            MobileUi.text(card,R.id.stock_detail,value(movement,"kind")+" · "+dateLabel(value(movement,"date")));
            double quantity=number(movement,"quantity");
            MobileUi.text(card,R.id.stock_quantity,(quantity>=0?"+ ":"")+amount(quantity)+" "+value(movement,"unit"));
            card.setOnClickListener(v->info(value(movement,"name"),value(movement,"kind")+" · Movimiento local registrado el "+dateLabel(value(movement,"date"))));
            history.addView(card);
        }
        MobileUi.text(root,R.id.inventory_alert_count,demo?alerts+" ALERTAS":"SIN DATOS");
        MobileUi.text(root,R.id.inventory_depot,all.isEmpty()?"Sin depósito informado":or(all.get(0),"depot","Sin ubicación"));
        if(alerts==0)empty(supplies,demo?"No hay suministros en estado crítico.":"Sin existencias disponibles.");
        if(history.getChildCount()==0)empty(history,"Todavía no hay registros para mostrar.");
    }
    private void export() {
        if(!workspace.isDemo()) { info("Reportes financieros","Los reportes de tu organización están disponibles en la web.");return; }
        reportRows=new ArrayList<>(); for(Map<String,Object> row:rows())if(period.isEmpty()||value(row,"date").startsWith(period))reportRows.add(row);
        if(reportRows.isEmpty()){info("Sin movimientos","Agregá un movimiento en la demo para generar un reporte.");return;}
        exportPdf.launch("MyFarmio-demo.pdf");
    }
    private void empty(LinearLayout parent,String message) { TextView text=new TextView(requireContext());text.setTextAppearance(R.style.StitchBody);text.setText(message);text.setPadding(0,MobileUi.dp(requireContext(),16),0,MobileUi.dp(requireContext(),8));parent.addView(text); }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state);state.putString("filter",filter);state.putString("period",period); }
    @Override public void onDestroyView() {if(sheet!=null){sheet.dismiss();sheet=null;} root=null;super.onDestroyView();}
}

