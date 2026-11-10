package com.myfarmio.app.data.weather;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Current public forecast, shared by dashboard and climate detail. */
public final class WeatherViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean loading;
        public final String error;
        public final String temperature;
        public final String condition;
        public final String updated;
        public final String humidity;
        public final String soilMoisture;
        public final String wind;
        public final String pressure;
        public final String uv;
        public final String rain;
        public final String et0;
        public final String location;
        public final double soilPercent;

        private State(boolean loading, String error, String temperature, String condition,
                      String updated, String humidity, String soilMoisture, String wind,
                      String pressure, String uv, String rain, String et0, String location,
                      double soilPercent) {
            this.loading = loading; this.error = error; this.temperature = temperature;
            this.condition = condition; this.updated = updated; this.humidity = humidity;
            this.soilMoisture = soilMoisture; this.wind = wind; this.pressure = pressure;
            this.uv = uv; this.rain = rain; this.et0 = et0; this.location = location;
            this.soilPercent = soilPercent;
        }
        static State loading(State previous, String fetching) {
            return previous == null
                ? new State(true, null, "—", "", fetching, "—", "—", "—", "—", "—", "—", "—", "Ubicación actual", 0)
                : new State(true, null, previous.temperature, previous.condition, "Actualizando…", previous.humidity,
                    previous.soilMoisture, previous.wind, previous.pressure, previous.uv, previous.rain,
                    previous.et0, previous.location, previous.soilPercent);
        }
        static State failure(String message, String noData, State previous) {
            if (previous == null) return new State(false, message, "—", "", noData, "—", "—", "—", "—", "—", "—", "—", "Ubicación actual", 0);
            return new State(false, message, previous.temperature, previous.condition, previous.updated,
                previous.humidity, previous.soilMoisture, previous.wind, previous.pressure, previous.uv,
                previous.rain, previous.et0, previous.location, previous.soilPercent);
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build();
    private Call activeCall;
    private double lastLatitude = Double.NaN;
    private double lastLongitude = Double.NaN;
    private long lastSuccessfulLoad;

    public WeatherViewModel(@NonNull Application application) { super(application); }
    public MutableLiveData<State> state() { return state; }

    public void load(double latitude, double longitude) {
        State current = state.getValue();
        boolean sameLocation = Math.abs(lastLatitude - latitude) < 0.0005
            && Math.abs(lastLongitude - longitude) < 0.0005;
        if (activeCall != null && !activeCall.isCanceled() && sameLocation) return;
        if (sameLocation && current != null && current.error == null
            && SystemClock.elapsedRealtime() - lastSuccessfulLoad < 5 * 60 * 1000L) return;
        if (activeCall != null) activeCall.cancel();
        lastLatitude = latitude;
        lastLongitude = longitude;
        State previous = state.getValue();
        state.setValue(State.loading(previous, getApplication().getString(com.myfarmio.app.R.string.climate_fetching)));
        HttpUrl url = HttpUrl.parse("https://api.open-meteo.com/v1/forecast").newBuilder()
            .addQueryParameter("latitude", String.valueOf(latitude))
            .addQueryParameter("longitude", String.valueOf(longitude))
            .addQueryParameter("current", "temperature_2m,relative_humidity_2m,precipitation,weather_code,pressure_msl,wind_speed_10m")
            .addQueryParameter("hourly", "soil_moisture_0_to_7cm,uv_index")
            .addQueryParameter("daily", "precipitation_sum,et0_fao_evapotranspiration")
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("forecast_days", "1")
            .build();
        Request request = new Request.Builder().url(url).get().build();
        activeCall = client.newCall(request);
        activeCall.enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) return;
                main.post(() -> state.setValue(State.failure(getApplication().getString(com.myfarmio.app.R.string.climate_connection_error), getApplication().getString(com.myfarmio.app.R.string.climate_no_data), previous)));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (call.isCanceled()) { response.close(); return; }
                try (Response result = response) {
                    if (!result.isSuccessful() || result.body() == null) {
                        main.post(() -> state.setValue(State.failure(getApplication().getString(com.myfarmio.app.R.string.climate_service_error), getApplication().getString(com.myfarmio.app.R.string.climate_no_data), previous)));
                        return;
                    }
                    JSONObject json = new JSONObject(result.body().string());
                    State parsed = parse(json, latitude, longitude);
                    main.post(() -> {
                        lastSuccessfulLoad = SystemClock.elapsedRealtime();
                        state.setValue(parsed);
                    });
                } catch (Exception error) {
                    main.post(() -> state.setValue(State.failure(getApplication().getString(com.myfarmio.app.R.string.climate_response_error), getApplication().getString(com.myfarmio.app.R.string.climate_no_data), previous)));
                }
            }
        });
    }

    private State parse(JSONObject json, double latitude, double longitude) throws Exception {
        JSONObject current = json.getJSONObject("current");
        JSONObject hourly = json.optJSONObject("hourly");
        JSONObject daily = json.optJSONObject("daily");
        int index = matchingHour(hourly, current.optString("time", ""));
        double soil = number(hourly, "soil_moisture_0_to_7cm", index);
        double soilPercent = Double.isNaN(soil) ? 0 : Math.max(0, Math.min(100, soil * 100));
        double code = current.optDouble("weather_code", -1);
        String pressure = formatted(current.optDouble("pressure_msl", Double.NaN), " hPa", 0);
        return new State(false, null,
            formatted(current.optDouble("temperature_2m", Double.NaN), "°", 1),
            condition((int) code), current.optString("time", "Pronóstico actual").replace('T', ' '),
            formatted(current.optDouble("relative_humidity_2m", Double.NaN), "%", 0),
            Double.isNaN(soil) ? getApplication().getString(com.myfarmio.app.R.string.climate_no_data) : formatted(soil, " m³/m³", 2),
            formatted(current.optDouble("wind_speed_10m", Double.NaN), " km/h", 0),
            pressure,
            formatted(number(hourly, "uv_index", index), "", 1),
            formatted(number(daily, "precipitation_sum", 0), " mm", 1),
            formatted(number(daily, "et0_fao_evapotranspiration", 0), " mm", 1),
            String.format(Locale.US, "%.4f, %.4f", latitude, longitude), soilPercent);
    }

    private static int matchingHour(JSONObject hourly, String currentTime) {
        if (hourly == null) return 0;
        JSONArray times = hourly.optJSONArray("time");
        if (times == null) return 0;
        String currentHour = currentTime.length() >= 13 ? currentTime.substring(0, 13) : currentTime;
        for (int i = 0; i < times.length(); i++) {
            String hour = times.optString(i);
            if (hour.startsWith(currentHour)) return i;
        }
        return 0;
    }
    private static double number(JSONObject object, String key, int index) {
        if (object == null) return Double.NaN;
        JSONArray values = object.optJSONArray(key);
        if (values == null || index < 0 || index >= values.length() || values.isNull(index)) return Double.NaN;
        return values.optDouble(index, Double.NaN);
    }
    private static String formatted(double value, String suffix, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Sin dato";
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("es", "AR"));
        DecimalFormat format = new DecimalFormat(decimals == 0 ? "0" : "0." + (decimals == 1 ? "0" : "00"), symbols);
        return format.format(value) + suffix;
    }
    private static String condition(int code) {
        if (code == 0) return "Despejado";
        if (code == 1 || code == 2) return "Algo nublado";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Niebla";
        if (code >= 51 && code <= 57) return "Llovizna";
        if (code >= 61 && code <= 67) return "Lluvia";
        if (code >= 71 && code <= 77) return "Nieve";
        if (code >= 80 && code <= 82) return "Chaparrones";
        if (code >= 85 && code <= 86) return "Nevadas";
        if (code >= 95) return "Tormenta";
        return "Condición actual";
    }
    @Override protected void onCleared() {
        if (activeCall != null) activeCall.cancel();
        super.onCleared();
    }
}
