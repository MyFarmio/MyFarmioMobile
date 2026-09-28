package com.myfarmio.app.ui.dashboard;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.myfarmio.app.network.SupabaseApiService;
import com.myfarmio.app.network.SupabaseConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardViewModel extends AndroidViewModel {

    public MutableLiveData<DashboardState> state = new MutableLiveData<>();
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public MutableLiveData<String> error = new MutableLiveData<>(null);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
    }

    public static class DashboardState {
        public String orgName;
        // UI availability metadata; keep failed queries distinct from successful empty results.
        public final java.util.Set<String> unavailableSections = new java.util.LinkedHashSet<>();
        public int alertPlots;
        public int totalPlots;
        public double totalHectares;

        public int criticalTasks;
        public int dueTodayTasks;

        public int livestockCritical;
        public int livestockAnimals;
        public int livestockHerds;

        public double financeBalance;
        public int financePending;

        public List<AlertItem> alerts = new ArrayList<>();
        public List<CropPulseItem> cropPulse = new ArrayList<>();
        public List<AgendaItem> agenda = new ArrayList<>();
    }

    public static class AlertItem {
        public String title;
        public String detail;
        public String priority; // Alta, Media, etc.

        public AlertItem(String title, String detail, String priority) {
            this.title = title;
            this.detail = detail;
            this.priority = priority;
        }
    }

    public static class CropPulseItem {
        public String title;
        public String detail;
        public int valuePercent;

        public CropPulseItem(String title, String detail, int valuePercent) {
            this.title = title;
            this.detail = detail;
            this.valuePercent = valuePercent;
        }
    }

    public static class AgendaItem {
        public String time;
        public String title;
        public String reference;

        public AgendaItem(String time, String title, String reference) {
            this.time = time;
            this.title = title;
            this.reference = reference;
        }
    }

    public void loadDashboard(String orgId) {
        if (TextUtils.isEmpty(orgId)) {
            error.postValue("Organization id is required");
            return;
        }

        isLoading.postValue(true);
        error.postValue(null);

        SupabaseApiService api = SupabaseConfig.getRetrofit().create(SupabaseApiService.class);

        DashboardState s = new DashboardState();

        AtomicInteger remaining = new AtomicInteger(5);
        // Helper to finish
        Runnable tryFinish = () -> {
            if (remaining.decrementAndGet() <= 0) {
                s.totalPlots = s.totalPlots; // no-op to ensure memory visibility
                state.postValue(s);
                isLoading.postValue(false);
            }
        };

        // Date string for due today comparison
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = df.format(new Date());

        // Plots
        Call<List<Map<String, Object>>> plotsCall = api.getPlots("eq." + orgId, "is.null", "id,status,area_hectares,current_crop,crop_stage,soil_moisture_percent");
        plotsCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body();
                    s.totalPlots = list.size();
                    double hectares = 0.0;
                    for (Map<String, Object> p : list) {
                        String status = p.get("status") != null ? p.get("status").toString() : null;
                        if ("issue".equalsIgnoreCase(status) || "monitoring".equalsIgnoreCase(status)) {
                            s.alertPlots++;
                            String id = p.get("id") != null ? p.get("id").toString() : "";
                            String area = p.get("area_hectares") != null ? p.get("area_hectares").toString() : "0";
                            String detail = area + " ha";
                            s.alerts.add(new AlertItem("Lote " + id, detail, "Alta"));
                        }

                        Object areaObj = p.get("area_hectares");
                        if (areaObj != null) {
                            try {
                                hectares += Double.parseDouble(areaObj.toString());
                            } catch (Exception e) {
                                // ignore
                            }
                        }

                        Object crop = p.get("current_crop");
                        if (crop != null) {
                            String cropName = crop.toString();
                            s.cropPulse.add(new CropPulseItem(cropName, p.get("crop_stage") != null ? p.get("crop_stage").toString() : "", 0));
                        }
                    }
                    s.totalHectares = hectares;
                } else {
                    s.unavailableSections.add("Campos");
                }
                tryFinish.run();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                s.unavailableSections.add("Campos");
                tryFinish.run();
            }
        });

        // Tasks
        Call<List<Map<String, Object>>> tasksCall = api.getTasks("eq." + orgId, "is.null", "id,status,priority,due_date");
        tasksCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body();
                    for (Map<String, Object> t : list) {
                        String priority = t.get("priority") != null ? t.get("priority").toString() : "";
                        if ("urgent".equalsIgnoreCase(priority)) {
                            s.criticalTasks++;
                            s.alerts.add(new AlertItem("Tarea crítica", t.get("id") != null ? t.get("id").toString() : "", "Alta"));
                        }

                        Object due = t.get("due_date");
                        if (due != null) {
                            String dueStr = due.toString();
                            // compare date prefix YYYY-MM-DD
                            if (dueStr.length() >= 10 && dueStr.substring(0, 10).equals(today)) {
                                s.dueTodayTasks++;
                            }
                        }
                    }
                } else {
                    s.unavailableSections.add("Tareas");
                }
                tryFinish.run();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                s.unavailableSections.add("Tareas");
                tryFinish.run();
            }
        });

        // Herds (optional)
        Call<List<Map<String, Object>>> herdsCall = api.getHerds("eq." + orgId, "is.null", "id,status");
        herdsCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body();
                    s.livestockHerds = list.size();
                    // crude: count animals if present (not requested) - leave 0
                } else {
                    s.unavailableSections.add("Ganado");
                }
                tryFinish.run();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                s.unavailableSections.add("Ganado");
                tryFinish.run();
            }
        });

        // Finance (optional)
        Call<List<Map<String, Object>>> financeCall = api.getFinance("eq." + orgId, "is.null", "id,movement_type,amount,status");
        financeCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body();
                    double balance = 0.0;
                    int pending = 0;
                    for (Map<String, Object> r : list) {
                        String mv = r.get("movement_type") != null ? r.get("movement_type").toString() : "";
                        Object amt = r.get("amount");
                        double v = 0.0;
                        if (amt != null) {
                            try { v = Double.parseDouble(amt.toString()); } catch (Exception e) { }
                        }
                        if ("income".equalsIgnoreCase(mv)) balance += v;
                        if ("expense".equalsIgnoreCase(mv)) balance -= v;
                        String status = r.get("status") != null ? r.get("status").toString() : "";
                        if (!"posted".equalsIgnoreCase(status)) pending++;
                    }
                    s.financeBalance = balance;
                    s.financePending = pending;
                } else {
                    s.unavailableSections.add("Finanzas");
                }
                tryFinish.run();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                s.unavailableSections.add("Finanzas");
                tryFinish.run();
            }
        });

        // Calendar events (optional)
        Call<List<Map<String, Object>>> calCall = api.getCalendar("eq." + orgId, "id,title,start_time,reference_label", "start_time.asc", 3);
        calCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body();
                    for (Map<String, Object> e : list) {
                        String start = e.get("start_time") != null ? e.get("start_time").toString() : "";
                        String title = e.get("title") != null ? e.get("title").toString() : "";
                        String ref = e.get("reference_label") != null ? e.get("reference_label").toString() : "";
                        s.agenda.add(new AgendaItem(start, title, ref));
                    }
                } else {
                    s.unavailableSections.add("Agenda");
                }
                tryFinish.run();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                s.unavailableSections.add("Agenda");
                tryFinish.run();
            }
        });
    }
}
