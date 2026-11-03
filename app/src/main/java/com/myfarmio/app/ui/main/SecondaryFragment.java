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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.content.res.ColorStateList;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.data.SupabaseRecordRepository;
import com.myfarmio.app.data.demo.RecordRepository;
import com.myfarmio.app.ui.common.*;
import java.util.*;

/** Finance and inventory views share Supabase-backed collections with the web application. */
public class SecondaryFragment extends Fragment {
    private View root;
    private CollectionViewModel model;
    private WorkspaceViewModel workspace;
    private MobileUi.Sheet sheet;
    private boolean finance;
    private String filter="",period="";
    private List<Map<String,Object>> reportRows=Collections.emptyList();
    private final ActivityResultLauncher<String> exportPdf=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),uri->{
        setExportLoading(false);
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
            Toast.makeText(requireContext(),getString(R.string.finance_export_saved),Toast.LENGTH_SHORT).show();
        } catch(Exception error) { info("No se pudo exportar","Elegí otra ubicación e intentá nuevamente."); }
        finally { pdf.close(); }
    });
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state) { return inflater.inflate(R.layout.fragment_secondary,parent,false); }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved) {
        root=view; finance="finance".equals(requireArguments().getString("module"));
        if(saved!=null) { filter=saved.getString("filter",""); period=saved.getString("period",""); }
        workspace=new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class); workspace.prepare(SessionManager.getInstance(requireContext()));
        model=new ViewModelProvider(this).get(CollectionViewModel.class);
        view.findViewById(R.id.secondary_eyebrow).setVisibility(View.GONE);
        view.findViewById(R.id.secondary_description).setVisibility(View.GONE);
        MobileUi.text(view,R.id.secondary_title,finance?"Finanzas":"Inventario");
        view.findViewById(R.id.finance_content).setVisibility(finance?View.VISIBLE:View.GONE);
        view.findViewById(R.id.inventory_content).setVisibility(finance?View.GONE:View.VISIBLE);
        view.findViewById(R.id.secondary_filter).setOnClickListener(v->filters());
        view.findViewById(R.id.finance_period).setOnClickListener(v->periods());
        view.findViewById(R.id.secondary_export).setOnClickListener(v->export());
        view.findViewById(R.id.finance_history).setOnClickListener(v->history());
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = view.findViewById(R.id.secondary_swipe);
        swipe.setColorSchemeResources(R.color.stitch_primary);
        swipe.setOnRefreshListener(this::reload);
        view.findViewById(R.id.finance_accounts).setOnClickListener(v->info("Detalle de cuentas",workspace.isDemo()?"Caja demo en ARS. El saldo disponible incluye únicamente ingresos y gastos cobrados o pagados.":"El detalle por cuenta no está disponible en esta consulta."));
        view.findViewById(R.id.inventory_distribution).setOnClickListener(v->info("Distribución de almacén",or(StitchUi.demo(workspace,"inventory"),"depot","No hay ubicaciones de almacén disponibles.")));
        int[] categoryIds={R.id.category_seeds,R.id.category_fertilizer,R.id.category_health,R.id.category_fuel};
        String[] categories={"Total","Disponibles","Bajo","Críticos"};
        for(int i=0;i<categoryIds.length;i++) { final String category=categories[i]; view.findViewById(categoryIds[i]).setOnClickListener(v->category(category)); }
        model.state.observe(getViewLifecycleOwner(),state->render());
        workspace.revision.observe(getViewLifecycleOwner(),revision->{render(); if(!workspace.isDemo())reload();});
        if(!workspace.isDemo()) reload();
    }
    private void reload() {
        SessionManager session=SessionManager.getInstance(requireContext());
        model.load(finance?"finance":"inventory",session.getUserId(),session.getOrganizationId(),true);
    }
    private List<Map<String,Object>> rows() {
        List<Map<String,Object>> result=new ArrayList<>();
        if(workspace.isDemo()) result.addAll(workspace.records().list(finance?"finance":"inventory"));
        else {
            if(model.state.getValue()==null) return Collections.emptyList();
            for(Map<String,Object> raw:model.state.getValue().rows) {
                Map<String,Object> row=finance
                    ? SupabaseRecordRepository.normalizeFinanceRow(raw) : new LinkedHashMap<>(raw);
                if(!finance) {
                    row.put("quantity",raw.get("current_quantity"));
                    row.put("capacity",raw.get("target_quantity"));
                    row.put("expiry",raw.get("expiry_date"));
                }
                result.add(row);
            }
        }
        if (finance) result.sort((left,right) -> {
            int byDate = value(right,"date").compareTo(value(left,"date"));
            if (byDate != 0) return byDate;
            int byCreated = value(right,"created_at").compareTo(value(left,"created_at"));
            return byCreated != 0 ? byCreated : value(right,"id").compareTo(value(left,"id"));
        });
        return result;
    }
    private MobileUi.Sheet open(String title) { if(sheet!=null)sheet.dismiss(); sheet=new MobileUi.Sheet(requireContext(),title); return sheet; }
    private void info(String title,String message) { MobileUi.Sheet current=open(title); current.row("MyFarmio",message); current.primary("Entendido",current::dismiss); current.show(); }
    public void quickCreate() {
        edit(Collections.emptyMap());
    }
    private void edit(Map<String,Object> row) {
        String module=finance?"finance":"inventory";
        MobileUi.Sheet current=open((row.isEmpty()?"Nuevo":"Editar")+(finance?" movimiento":" insumo"));
        if(workspace.isDemo()) StitchDemoEditor.bind(current,workspace,module,row);
        else {
            RecordRepository repository=new SupabaseRecordRepository(requireContext(), SessionManager.getInstance(requireContext()));
            new com.myfarmio.app.ui.common.RecordEditor(current,module,row,repository,()->{
                Toast.makeText(requireContext(),"Guardado",Toast.LENGTH_SHORT).show(); reload();
            });
        }
        current.show();
    }
    private void detail(Map<String,Object> row) {
        String itemTitle=or(row,finance?"description":"name","Registro");
        String status=value(row,"status");
        MobileUi.Sheet current=open(itemTitle);
        current.detailHeader(MobileUi.detailIcon(finance?"finance":"inventory"),itemTitle,label(status),MobileUi.detailStatusColor(status));
        if(finance) current.metricRow(R.drawable.st_payments,R.string.detail_amount,(value(row,"currency").isEmpty()?"":value(row,"currency")+" ")+amount(number(row,"amount")),R.drawable.st_calendar_month,R.string.detail_date,dateLabel(value(row,"date")));
        else current.metricRow(R.drawable.st_inventory_2,R.string.detail_stock,amount(number(row,"quantity"))+" "+value(row,"unit"),R.drawable.st_warehouse,R.string.detail_depot,value(row,"depot"));
        if(finance) { current.row("Importe",(value(row,"currency").isEmpty()?"":value(row,"currency")+" ")+amount(number(row,"amount"))); current.row("Fecha",dateLabel(value(row,"date"))); current.row("Estado",value(row,"status")); }
        else { current.row("Existencias",amount(number(row,"quantity"))+" "+value(row,"unit")); current.row("Lote",value(row,"lot")); current.row("Ubicación",value(row,"depot")); }
        if(workspace.isDemo()) {
            current.primary("Editar en demo",()->edit(row));
            current.danger("Eliminar de la demo",()->new MaterialAlertDialogBuilder(requireContext()).setTitle("¿Eliminar este registro local?")
                .setMessage("Esta acción no afecta a tu organización.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{
                    workspace.records().delete(finance?"finance":"inventory",value(row,"id")); current.dismiss();
                }).show());
        } else {
            current.primary("Editar",()->{current.dismiss();edit(row);});
            current.danger("Eliminar",()->new MaterialAlertDialogBuilder(requireContext()).setTitle("¿Eliminar registro?")
                .setMessage("Se ocultará de la lista en la web y en mobile.").setNegativeButton("Cancelar",null)
                .setPositiveButton("Eliminar",(d,w)->new SupabaseRecordRepository(requireContext(), SessionManager.getInstance(requireContext()))
                    .deleteAsync(finance?"finance":"inventory",value(row,"id"),error->{
                        if(error!=null) Toast.makeText(requireContext(),error,Toast.LENGTH_LONG).show();
                        else {current.dismiss();reload();}
                    })).show());
        }
        current.show();
    }
    private void filters() {
        MobileUi.Sheet current=open(getString(R.string.finance_filter_title));
        current.heading(getString(R.string.finance_filter_heading));
        String[] names={getString(R.string.finance_filter_all),getString(R.string.finance_filter_income),getString(R.string.finance_filter_expense)};
        String[] values={"","income","expense"};
        String[] selected={filter};
        ChipGroup choices=new ChipGroup(requireContext()); choices.setSingleSelection(true); choices.setSelectionRequired(true);
        Chip[] chips=new Chip[names.length];
        for(int i=0;i<names.length;i++) {
            final String choice=values[i];
            chips[i]=choiceChip(names[i],choice.equals(filter)||(choice.isEmpty()&&filter.isEmpty()));
            choices.addView(chips[i]);
            chips[i].setOnCheckedChangeListener((button,checked)->{ if(checked) selected[0]=choice; });
        }
        current.body.addView(choices);
        current.button(getString(R.string.finance_clear),()->{ selected[0]=""; chips[0].setChecked(true); });
        current.primary(getString(R.string.finance_apply),()->{filter=selected[0];current.dismiss();render();});
        current.show();
    }
    private void periods() {
        MobileUi.Sheet current=open(getString(R.string.finance_period_title));
        current.heading(getString(R.string.finance_period_heading));
        String[] selected={period};
        ChipGroup choices=new ChipGroup(requireContext()); choices.setSingleSelection(true); choices.setSelectionRequired(true);
        Set<String> dates=new TreeSet<>(Collections.reverseOrder());
        for(Map<String,Object> row:rows()) { String date=value(row,"date"); if(date.length()>=7) dates.add(date.substring(0,7)); }
        Chip all=choiceChip(getString(R.string.finance_period_all),period.isEmpty()); choices.addView(all);
        all.setOnCheckedChangeListener((button,checked)->{if(checked)selected[0]="";});
        for(String date:dates) {
            Chip chip=choiceChip(date,date.equals(period)); choices.addView(chip);
            chip.setOnCheckedChangeListener((button,checked)->{if(checked)selected[0]=date;});
        }
        current.body.addView(choices);
        current.button(getString(R.string.finance_clear),()->{selected[0]="";all.setChecked(true);});
        current.primary(getString(R.string.finance_apply),()->{period=selected[0];current.dismiss();render();});
        current.show();
    }
    private Chip choiceChip(String text,boolean checked) {
        Chip chip=new Chip(requireContext());
        chip.setText(text); chip.setCheckable(true); chip.setClickable(true); chip.setChecked(checked);
        int[][] states={new int[]{android.R.attr.state_checked},new int[]{}};
        int[] colors={requireContext().getColor(R.color.stitch_selected),requireContext().getColor(R.color.surface_container_lowest)};
        chip.setChipBackgroundColor(new ColorStateList(states,colors));
        chip.setChipStrokeColor(ColorStateList.valueOf(requireContext().getColor(R.color.outline_variant)));
        chip.setChipStrokeWidth(MobileUi.dp(requireContext(),1));
        chip.setTextColor(requireContext().getColor(R.color.primary));
        return chip;
    }
    private void history() {
        if (root == null) return;
        androidx.navigation.NavController controller = androidx.navigation.Navigation.findNavController(root);
        if (controller.getCurrentDestination() != null && controller.getCurrentDestination().getId() == R.id.nav_finanzas)
            controller.navigate(R.id.nav_finance_history);
    }
    private void category(String category) {
        MobileUi.Sheet current=open(category); int count=0;
        for(Map<String,Object> row:rows()) {
            String status=value(row,"status");
            boolean matches=workspace.isDemo()?category.equals(value(row,"category"))
                :"Total".equals(category)||"Disponibles".equals(category)&&"ok".equals(status)
                    ||"Bajo".equals(category)&&"low".equals(status)||"Críticos".equals(category)&&"critical".equals(status);
            if(matches) {count++;current.button(value(row,"name")+" · "+amount(number(row,"quantity"))+" "+value(row,"unit"),()->detail(row));}
        }
        if(count==0) current.row("Sin insumos","No hay registros en este estado.");
        current.show();
    }
    private void render() {
        if(root==null||model==null)return;
        CollectionViewModel.State state=model.state.getValue(); if(state==null)return;
        boolean demo=workspace.isDemo();
        root.findViewById(R.id.secondary_loading).setVisibility(!demo&&state.loading?View.VISIBLE:View.GONE);
        ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout)root.findViewById(R.id.secondary_swipe))
            .setRefreshing(!demo && state.loading && state.loaded);
        root.findViewById(R.id.mobile_state).setVisibility(View.GONE);
        if(!finance) { renderInventory(); return; }
        List<Map<String,Object>> all=new ArrayList<>();
        for(Map<String,Object> row:rows()) if(period.isEmpty()||value(row,"date").startsWith(period))all.add(row);
        boolean reliable=!all.isEmpty(),mixed=false; String currency="";
        double income=0,expense=0,cash=0,receivable=0,committed=0; int pending=0;
        for(Map<String,Object> row:all) {
            String found=value(row,"currency"); if(!found.isEmpty()&&(!currency.isEmpty()&&!currency.equals(found)))mixed=true; if(!found.isEmpty())currency=found;
            double amount=number(row,"amount"); String recordStatus=value(row,"status");
            boolean incoming="income".equals(value(row,"movement_type")), posted="posted".equals(recordStatus)||"paid".equals(recordStatus)||"collected".equals(recordStatus);
            if(incoming)income+=amount; else if("expense".equals(value(row,"movement_type")))expense+=amount;
            if(posted)cash+=incoming?amount:-amount; else { pending++; if(incoming)receivable+=amount;else committed+=amount; }
        }
        reliable=reliable&&!mixed; String unit=currency.isEmpty()?"":currency+" ";
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
        renderFinanceControls();
        LinearLayout list=root.findViewById(R.id.secondary_records);list.removeAllViews();
        int shown=0;
        for(Map<String,Object> row:all) {
            if(!filter.isEmpty()&&!filter.equals(value(row,"movement_type")))continue;
            if(shown++>=5)continue;
            View card=getLayoutInflater().inflate(R.layout.item_stitch_transaction,list,false);
            boolean incoming="income".equals(value(row,"movement_type"));
            MobileUi.text(card,R.id.transaction_name,or(row,"description","Movimiento"));
            MobileUi.text(card,R.id.transaction_amount,(incoming?"+ ":"− ")+(value(row,"currency").isEmpty()?"":value(row,"currency")+" ")+amount(number(row,"amount")));
            ((TextView)card.findViewById(R.id.transaction_amount)).setTextColor(requireContext().getColor(incoming?R.color.stitch_primary:R.color.secondary));
            MobileUi.text(card,R.id.transaction_detail,dateLabel(value(row,"date")));
            MobileUi.text(card,R.id.transaction_plot,or(row,"plot",value(row,"category")));
            StitchUi.badge(card,R.id.transaction_type,incoming?"INGRESO":"GASTO",!incoming,false);
            String status=value(row,"status");
            boolean settled="posted".equals(status)||"paid".equals(status)||"collected".equals(status);
            StitchUi.badge(card,R.id.transaction_status,settled?incoming?"COBRADO":"PAGADO":"overdue".equals(status)?"VENCIDO":"PENDIENTE",!settled,status.equals("overdue"));
            card.setOnClickListener(v->detail(row));list.addView(card);
        }
        if(shown==0 && (demo || !all.isEmpty()))empty(list,filter.isEmpty()?"No hay movimientos en este período.":"Sin movimientos para este filtro.");
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
        else if(!demo&&!state.loading&&all.isEmpty())MobileUi.state(root,"Sin registros visibles","No hay movimientos disponibles. Creá desde el botón +.",null,null);
    }
    private void renderInventory() {
        boolean demo=workspace.isDemo(); List<Map<String,Object>> all=rows();
        int[] ids={R.id.category_seeds_count,R.id.category_fertilizer_count,R.id.category_health_count,R.id.category_fuel_count};
        int ready=0,low=0,critical=0;
        for(Map<String,Object> row:all) {String status=value(row,"status");if("ok".equals(status))ready++;else if("low".equals(status))low++;else if("critical".equals(status))critical++;}
        MobileUi.text(root,ids[0],String.valueOf(all.size())); MobileUi.text(root,ids[1],String.valueOf(ready));
        MobileUi.text(root,ids[2],String.valueOf(low)); MobileUi.text(root,ids[3],String.valueOf(critical));
        LinearLayout supplies=root.findViewById(R.id.inventory_records), history=root.findViewById(R.id.inventory_history); supplies.removeAllViews();history.removeAllViews();
        int alerts=0;
        for(Map<String,Object> row:all) {
            double ratio=number(row,"capacity")>0?number(row,"quantity")/number(row,"capacity"):"critical".equals(value(row,"status"))?0:"low".equals(value(row,"status"))?0.3:1;
            boolean attention="low".equals(value(row,"status"))||"critical".equals(value(row,"status"))||ratio<0.5;
            if(attention) alerts++;
            {
                View card=getLayoutInflater().inflate(R.layout.item_stitch_supply,supplies,false);
                boolean criticalStatus="critical".equals(value(row,"status"))||ratio<=0.15;
                StitchUi.badge(card,R.id.supply_status,attention?(criticalStatus?"CRÍTICO":"BAJO"):"OK",attention,criticalStatus);
                MobileUi.text(card,R.id.supply_lot,"Lote "+value(row,"lot")+" · Vence "+value(row,"expiry"));
                MobileUi.text(card,R.id.supply_name,value(row,"name"));
                MobileUi.text(card,R.id.supply_quantity,amount(number(row,"quantity"))+" / "+amount(number(row,"capacity"))+" "+value(row,"unit"));
                MobileUi.text(card,R.id.supply_ratio,amount(ratio*100)+"% disponible");
                StitchUi.progress(card,R.id.supply_progress,ratio*100);
                ((ProgressBar)card.findViewById(R.id.supply_progress)).setProgressTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(criticalStatus?R.color.error:R.color.secondary)));
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
        MobileUi.text(root,R.id.inventory_alert_count,alerts+" ALERTAS");
        MobileUi.text(root,R.id.inventory_depot,all.size()+" insumos");
        if(all.isEmpty() && demo)empty(supplies,"No hay insumos registrados.");
        if(history.getChildCount()==0)empty(history,"Todavía no hay registros para mostrar.");
        CollectionViewModel.State state=model.state.getValue();
        if(!demo&&state!=null&&state.error!=null)MobileUi.state(root,"No pudimos cargar inventario",state.error,"Reintentar",this::reload);
        else if(!demo&&state!=null&&!state.loading&&all.isEmpty())MobileUi.state(root,"Sin registros visibles","No hay insumos disponibles. Creá desde el botón +.",null,null);
    }
    private void export() {
        if(!workspace.isDemo()) { info("Reportes financieros","Los reportes de tu organización están disponibles en la web.");return; }
        reportRows=new ArrayList<>(); for(Map<String,Object> row:rows())if(period.isEmpty()||value(row,"date").startsWith(period))reportRows.add(row);
        if(reportRows.isEmpty()){info("Sin movimientos","Agregá un movimiento en la demo para generar un reporte.");return;}
        setExportLoading(true);
        try { exportPdf.launch("MyFarmio-demo.pdf"); }
        catch (Exception error) { setExportLoading(false); info("No se pudo exportar","Elegí otra ubicación e intentá nuevamente."); }
    }
    private void renderFinanceControls() {
        if(root==null)return;
        String filterText=filter.isEmpty()?getString(R.string.finance_filter_button):getString(R.string.finance_filter_active_button,
            filter.equals("income")?getString(R.string.finance_filter_income):getString(R.string.finance_filter_expense));
        String periodText=period.isEmpty()?getString(R.string.finance_period_button):getString(R.string.finance_period_active_button,period);
        ((com.google.android.material.button.MaterialButton)root.findViewById(R.id.secondary_filter)).setText(filterText);
        ((com.google.android.material.button.MaterialButton)root.findViewById(R.id.finance_period)).setText(periodText);
        ChipGroup active=root.findViewById(R.id.finance_active_chips); active.removeAllViews();
        active.setVisibility(filter.isEmpty()&&period.isEmpty()?View.GONE:View.VISIBLE);
        if(!filter.isEmpty()) addActiveChip(active,getString(R.string.finance_active_filter_format,
            filter.equals("income")?getString(R.string.finance_filter_income):getString(R.string.finance_filter_expense)),()->{filter="";render();});
        if(!period.isEmpty()) addActiveChip(active,getString(R.string.finance_active_period_format,period),()->{period="";render();});
    }
    private void addActiveChip(ChipGroup group,String text,Runnable remove) {
        Chip chip=new Chip(requireContext()); chip.setText(text); chip.setCloseIconResource(R.drawable.ic_close); chip.setCloseIconVisible(true);
        chip.setCloseIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
        chip.setChipBackgroundColor(ColorStateList.valueOf(requireContext().getColor(R.color.primary_fixed)));
        chip.setTextColor(requireContext().getColor(R.color.primary)); chip.setOnCloseIconClickListener(v->remove.run());
        group.addView(chip);
    }
    private void setExportLoading(boolean loading) {
        if(root==null)return;
        View progress=root.findViewById(R.id.finance_export_progress); if(progress==null)return;
        progress.setVisibility(loading?View.VISIBLE:View.GONE);
        com.google.android.material.button.MaterialButton button=root.findViewById(R.id.secondary_export);
        button.setEnabled(!loading); button.setText(loading?R.string.finance_export_loading:R.string.finance_export_button);
    }
    private void empty(LinearLayout parent,String message) { TextView text=new TextView(requireContext());text.setTextAppearance(R.style.StitchBody);text.setText(message);text.setPadding(0,MobileUi.dp(requireContext(),16),0,MobileUi.dp(requireContext(),8));parent.addView(text); }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state);state.putString("filter",filter);state.putString("period",period); }
    @Override public void onDestroyView() {if(sheet!=null){sheet.dismiss();sheet=null;} root=null;super.onDestroyView();}
}

