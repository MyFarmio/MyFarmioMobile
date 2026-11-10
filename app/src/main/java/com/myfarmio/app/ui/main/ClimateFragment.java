package com.myfarmio.app.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;
import com.myfarmio.app.R;
import com.myfarmio.app.data.weather.WeatherViewModel;

public class ClimateFragment extends Fragment {
    private WeatherViewModel weather;
    private View root;
    private ActivityResultLauncher<String[]> locationPermission;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean permissionRequestMade;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable locationTimeout;

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        permissionRequestMade = state != null && state.getBoolean("climatePermissionRequested", false);
        locationPermission = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
            boolean granted = Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_COARSE_LOCATION))
                || Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_FINE_LOCATION));
            if (granted) locate();
            else showMessage(getString(R.string.climate_location_permission_denied));
        });
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_climate, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        root = view;
        weather = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        view.findViewById(R.id.climate_windows_card).setVisibility(View.GONE);
        view.findViewById(R.id.climate_crop_card).setVisibility(View.GONE);
        view.findViewById(R.id.climate_alerts_card).setVisibility(View.GONE);
        weather.state().observe(getViewLifecycleOwner(), this::render);
        view.findViewById(R.id.climate_register).setOnClickListener(v -> refresh());
        refresh();
    }

    @Override public void onResume() {
        super.onResume();
        if (root != null) refresh();
    }

    private void refresh() {
        if (!isAdded()) return;
        boolean coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!coarse && !fine) {
            if (!permissionRequestMade) {
                permissionRequestMade = true;
                showMessage(getString(R.string.climate_location_permission_prompt));
                locationPermission.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
            } else showMessage(getString(R.string.climate_location_permission_denied));
            return;
        }
        if (locationListener != null) return;
        locate();
    }

    private void locate() {
        if (!isAdded() || root == null || locationListener != null) return;
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            showMessage(getString(R.string.climate_location_enable));
            return;
        }
        try {
            boolean fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
            List<String> providers = new ArrayList<>();
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER);
            if (fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER);
            if (providers.isEmpty()) {
                showMessage(getString(R.string.climate_location_enable));
                return;
            }
            Location last = null;
            for (String provider : providers) {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (last == null || candidate.getTime() > last.getTime())) last = candidate;
            }
            if (last != null && System.currentTimeMillis() - last.getTime() < 5 * 60 * 1000L) {
                fetch(last); return;
            }
            showMessage(getString(R.string.climate_location_searching));
            locationListener = new LocationListener() {
                @Override public void onLocationChanged(@NonNull Location location) {
                    stopLocationUpdates();
                    fetch(location);
                }
                @Override public void onProviderDisabled(@NonNull String provider) {
                    stopLocationUpdates(); showMessage(getString(R.string.climate_location_enable));
                }
                @Override public void onProviderEnabled(@NonNull String provider) { }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            };
            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider, 0L, 0f, locationListener, Looper.getMainLooper());
            }
            locationTimeout = () -> {
                stopLocationUpdates();
                showMessage(getString(R.string.climate_location_timeout));
            };
            handler.postDelayed(locationTimeout, 15_000L);
        } catch (SecurityException exception) {
            showMessage(getString(R.string.climate_location_permission_error));
        } catch (RuntimeException exception) {
            showMessage(getString(R.string.climate_location_error));
        }
    }

    private void fetch(Location location) {
        if (weather != null) weather.load(location.getLatitude(), location.getLongitude());
    }

    private void render(WeatherViewModel.State state) {
        if (root == null || state == null) return;
        text(R.id.climate_location, state.location.toUpperCase());
        text(R.id.climate_temperature, state.temperature);
        text(R.id.climate_condition, state.condition);
        text(R.id.climate_updated, state.updated);
        text(R.id.climate_value_0, state.humidity);
        text(R.id.climate_value_1, state.wind);
        text(R.id.climate_value_2, state.pressure);
        text(R.id.climate_value_3, state.uv);
        text(R.id.climate_rain, state.rain);
        text(R.id.climate_etp, state.et0);
        text(R.id.climate_water_legend, getString(R.string.climate_water_soil_legend, state.soilMoisture));
        root.findViewById(R.id.climate_water_progress).setVisibility(state.soilMoisture.startsWith("Sin ") ? View.GONE : View.VISIBLE);
        com.myfarmio.app.ui.common.StitchUi.progress(root, R.id.climate_water_progress, state.soilPercent);
        if (state.error != null) showMessage(state.error);
        else if (!state.loading) showMessage(getString(R.string.climate_weather_source));
        com.google.android.material.button.MaterialButton button = root.findViewById(R.id.climate_register);
        button.setEnabled(!state.loading);
        button.setText(state.loading ? R.string.climate_update_loading : R.string.climate_update_button);
    }

    private void text(int id, String value) {
        TextView view = root.findViewById(id);
        view.setText(value == null || value.isEmpty() ? "—" : value);
    }
    private void showMessage(String message) {
        if (root != null) text(R.id.climate_note, message);
    }
    private void stopLocationUpdates() {
        if (locationTimeout != null) { handler.removeCallbacks(locationTimeout); locationTimeout = null; }
        if (locationManager != null && locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
            locationListener = null;
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        out.putBoolean("climatePermissionRequested", permissionRequestMade);
        super.onSaveInstanceState(out);
    }
    @Override public void onDestroyView() {
        stopLocationUpdates(); root = null; super.onDestroyView();
    }
}
