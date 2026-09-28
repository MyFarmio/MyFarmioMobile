package com.myfarmio.app.ui.dashboard;

import static com.myfarmio.app.ui.common.RecordText.*;
import com.myfarmio.app.data.demo.RecordRepository;
import java.time.LocalDate;
import java.util.*;

/** Dashboard projection from the same session repository used by all demo CRUD screens. */
final class DemoDashboard {
    private DemoDashboard() {}
    static DashboardViewModel.DashboardState summarize(RecordRepository repository) {
        DashboardViewModel.DashboardState state=new DashboardViewModel.DashboardState();
        LocalDate today=LocalDate.now();
        List<Map<String,Object>> plots=repository.list("fields");
        state.totalPlots=plots.size();
        for (Map<String,Object> plot:plots) {
            state.totalHectares+=number(plot,"area_hectares");
            if (attention(value(plot,"status"))) {
                state.alertPlots++;
                state.alerts.add(new DashboardViewModel.AlertItem("Lote "+value(plot,"name").replaceFirst("^Lote ",""),
                    or(plot,"next_action",label(value(plot,"status"))),"Alta"));
            }
            if (!value(plot,"current_crop").isEmpty()) state.cropPulse.add(new DashboardViewModel.CropPulseItem(
                value(plot,"current_crop")+" · "+value(plot,"name"),amount(number(plot,"area_hectares"))+" ha · "+or(plot,"crop_stage","Sin etapa"),0));
        }
        List<Map<String,Object>> tasks=repository.list("tasks");
        tasks.sort(Comparator.comparing(row->or(row,"due_date","9999-12-31")));
        for (Map<String,Object> task:tasks) {
            if (Arrays.asList("completed","done","cancelled").contains(value(task,"status"))) continue;
            boolean critical=Arrays.asList("high","urgent").contains(value(task,"priority"))||overdue(task,today);
            if (today.equals(date(value(task,"due_date")))) state.dueTodayTasks++;
            if (critical) {
                state.criticalTasks++;
                state.alerts.add(new DashboardViewModel.AlertItem(value(task,"title"),
                    "Vence "+dateLabel(value(task,"due_date"))+" · "+or(task,"related_entity_name","Sin lote"),label(value(task,"priority"))));
            }
            if (state.agenda.size()<5 && date(value(task,"due_date"))!=null)
                state.agenda.add(new DashboardViewModel.AgendaItem(value(task,"due_date"),value(task,"title"),value(task,"related_entity_name")));
        }
        List<Map<String,Object>> herds=repository.list("livestock");
        state.livestockHerds=herds.size();
        state.livestockAnimals=repository.list("animals").size();
        for (Map<String,Object> herd:herds) if (attention(value(herd,"status"))) state.livestockCritical++;
        // No finance/weather fixtures: the view displays availability, never an invented balance.
        return state;
    }
}
