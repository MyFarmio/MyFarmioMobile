package com.myfarmio.app.ui.main;

import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.ui.common.MobileUi;
import com.myfarmio.app.ui.common.OperationalFragment;
import com.myfarmio.app.ui.common.WorkspaceViewModel;
import com.myfarmio.app.data.weather.WeatherViewModel;
import com.myfarmio.app.data.offline.OfflineConnectivity;
import com.myfarmio.app.data.offline.OfflineStore;
import com.myfarmio.app.data.offline.OfflineSyncScheduler;

/** Post-login Stitch shell. Navigation and the existing child destinations remain native. */
public class MainFragment extends Fragment {
    private NavController navController;
    private NavHostFragment host;
    private WorkspaceViewModel workspace;
    private MobileUi.Sheet sheet;
    private NavController.OnDestinationChangedListener destinationListener;
    private View root;
    private boolean speedDialOpen;
    private androidx.activity.OnBackPressedCallback dialBack;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private OfflineStore offlineStore;
    private SessionManager session;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_main, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        session = SessionManager.getInstance(requireContext());
        offlineStore = OfflineStore.get(requireContext());
        TextView offlineStatus = view.findViewById(R.id.offline_status);
        offlineStatus.setOnClickListener(v -> {
            if (offlineStore == null || session == null) return;
            offlineStore.retryFailed(session.getUserId(), session.getOrganizationId());
            OfflineSyncScheduler.schedule(requireContext());
            updateOfflineStatus(view);
        });
        offlineStore.revision().observe(getViewLifecycleOwner(), revision -> updateOfflineStatus(view));
        updateOfflineStatus(view);
        connectivityManager = (ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(@NonNull Network network) { view.post(() -> {
                    updateOfflineStatus(view);
                    if (isAdded()) OfflineSyncScheduler.schedule(requireContext());
                }); }
                @Override public void onLost(@NonNull Network network) { view.post(() -> updateOfflineStatus(view)); }
                @Override public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) { view.post(() -> updateOfflineStatus(view)); }
            };
            try { connectivityManager.registerDefaultNetworkCallback(networkCallback); }
            catch (RuntimeException ignored) { networkCallback = null; }
        }
        workspace = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
        workspace.prepare(session);
        updateOfflineStatus(view);
        WeatherViewModel weather = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        weather.state().observe(getViewLifecycleOwner(), current -> {
            String temperature = current == null ? "—" : current.temperature;
            MobileUi.text(view, R.id.header_weather, temperature);
        });
        workspace.revision.observe(getViewLifecycleOwner(), revision -> {
            if(workspace.isDemo()) {
                String demoName=com.myfarmio.app.ui.common.RecordText.or(com.myfarmio.app.ui.common.StitchUi.demo(workspace,"profile"),"name","Demo");
                MobileUi.text(view,R.id.header_profile,demoName.substring(0,1).toUpperCase());
            }
        });
        String name = workspace.isDemo() ? com.myfarmio.app.ui.common.RecordText.or(com.myfarmio.app.ui.common.StitchUi.demo(workspace,"profile"),"name","Demo") : session.getUserName();
        if (name != null && !name.trim().isEmpty()) ((TextView) view.findViewById(R.id.header_profile)).setText(name.trim().substring(0, 1).toUpperCase());
        view.findViewById(R.id.header_profile).setOnClickListener(v -> destination(R.id.nav_perfil));
        view.findViewById(R.id.header_weather).setOnClickListener(v -> destination(R.id.nav_clima));
        view.findViewById(R.id.header_back).setOnClickListener(v -> {
            if (navController != null && !navController.navigateUp()) destination(R.id.nav_tareas);
        });
        host = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.main_nav_host);
        if (host == null) return;
        navController = host.getNavController();
        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_nav);
        bottomNav.setItemActiveIndicatorEnabled(false); // The selector surrounds icon AND label, as in Stitch.
        NavigationUI.setupWithNavController(bottomNav, navController);
        dialBack = new androidx.activity.OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { closeSpeedDial(); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),dialBack);
        destinationListener = (controller, destination, args) -> {
            closeSpeedDial();
            MobileUi.transitionPage(view.findViewById(R.id.main_nav_host));
            int id = destination.getId();
            if (id == R.id.nav_tareas || id == R.id.nav_campos || id == R.id.nav_ganado
                || id == R.id.nav_inventario || id == R.id.nav_finanzas) animateSelectedTab(bottomNav);
            bottomNav.setTag(R.id.nav_perfil, id == R.id.nav_perfil);
            bottomNav.setVisibility(id == R.id.nav_perfil ? View.GONE : View.VISIBLE);
            boolean secondary = id == R.id.nav_perfil || id == R.id.nav_clima || id == R.id.nav_finance_history;
            view.findViewById(R.id.header_back).setVisibility(secondary ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.header_weather).setVisibility(id == R.id.nav_perfil ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.main_fab).setVisibility(id == R.id.nav_perfil || id == R.id.nav_clima || id == R.id.nav_finance_history ? View.GONE : View.VISIBLE);
            configureFabActions(id);
            if (secondary) {
                for(int index=0;index<bottomNav.getMenu().size();index++) {
                    android.view.MenuItem item=bottomNav.getMenu().getItem(index);
                    item.setCheckable(false); item.setChecked(false); item.setCheckable(true);
                }
            }
        };
        navController.addOnDestinationChangedListener(destinationListener);
        FloatingActionButton fab = view.findViewById(R.id.main_fab);
        fab.setOnClickListener(v -> {
            int current = navController == null || navController.getCurrentDestination() == null
                ? R.id.nav_tareas : navController.getCurrentDestination().getId();
            if (current == R.id.nav_ganado) setSpeedDial(!speedDialOpen);
            else if (current == R.id.nav_campos || current == R.id.nav_tareas) quickCreate(current, false);
            else if (current == R.id.nav_inventario || current == R.id.nav_finanzas) quickSecondary(current);
        });
        view.findViewById(R.id.fab_scrim).setOnClickListener(v -> closeSpeedDial());
        view.findViewById(R.id.fab_field).setOnClickListener(v -> quickCreate(R.id.nav_campos, false));
        view.findViewById(R.id.fab_task).setOnClickListener(v -> quickCreate(R.id.nav_tareas, false));
        view.findViewById(R.id.fab_animal).setOnClickListener(v -> quickCreate(R.id.nav_ganado, true));
        view.findViewById(R.id.fab_herd).setOnClickListener(v -> quickCreate(R.id.nav_ganado, false));
        view.findViewById(R.id.fab_supply).setOnClickListener(v -> quickSecondary(R.id.nav_inventario));
        view.findViewById(R.id.fab_movement).setOnClickListener(v -> quickSecondary(R.id.nav_finanzas));
    }
    private void animateSelectedTab(BottomNavigationView bottomNav) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled() || !(bottomNav.getChildAt(0) instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup menuView = (android.view.ViewGroup) bottomNav.getChildAt(0);
        for (int index = 0; index < bottomNav.getMenu().size() && index < menuView.getChildCount(); index++) {
            if (!bottomNav.getMenu().getItem(index).isChecked()) continue;
            View selected = menuView.getChildAt(index);
            selected.animate().cancel();
            selected.setScaleX(.92f); selected.setScaleY(.92f);
            selected.animate().scaleX(1f).scaleY(1f)
                .setDuration(260L)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.15f))
                .start();
            return;
        }
    }
    private void updateOfflineStatus(View view) {
        if (!isAdded() || offlineStore == null || session == null || workspace == null) return;
        TextView banner = view.findViewById(R.id.offline_status);
        if (workspace.isDemo() || session.getUserId() == null || session.getOrganizationId() == null) {
            banner.setVisibility(View.GONE);
            return;
        }
        boolean online = OfflineConnectivity.isUsable(requireContext());
        int pending = offlineStore.pendingCount(session.getUserId(), session.getOrganizationId());
        int failed = offlineStore.failedCount(session.getUserId(), session.getOrganizationId());
        if (failed > 0) banner.setText(getString(R.string.offline_failed_count, failed));
        else if (!online && pending > 0) banner.setText(getString(R.string.offline_pending_count, pending));
        else if (!online) banner.setText(R.string.offline_no_connection);
        else if (pending > 0) banner.setText(getString(R.string.offline_syncing_count, pending));
        banner.setVisibility(!online || pending > 0 ? View.VISIBLE : View.GONE);
        banner.setAlpha(online && failed == 0 ? 0.82f : 1f);
    }
    private void configureFabActions(int destinationId) {
        if (root == null) return;
        boolean livestock = destinationId == R.id.nav_ganado;
        root.findViewById(R.id.fab_field).setVisibility(View.GONE);
        root.findViewById(R.id.fab_task).setVisibility(View.GONE);
        root.findViewById(R.id.fab_animal).setVisibility(livestock ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.fab_herd).setVisibility(livestock ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.fab_supply).setVisibility(View.GONE);
        root.findViewById(R.id.fab_movement).setVisibility(View.GONE);
        View fab = root.findViewById(R.id.main_fab);
        fab.setContentDescription(livestock ? "Elegir qué agregar" : "Agregar registro");
    }
    private void quickSecondary(int route) {
        closeSpeedDial(); destination(route);
        root.post(() -> {
            if(host==null||!isAdded())return;
            Fragment active=host.getChildFragmentManager().getPrimaryNavigationFragment();
            if(active instanceof SecondaryFragment)((SecondaryFragment)active).quickCreate();
        });
    }

    private void setSpeedDial(boolean open) {
        if (root == null) return;
        speedDialOpen = open;
        if(dialBack!=null)dialBack.setEnabled(open);
        root.findViewById(R.id.main_header).setForeground(open ? new android.graphics.drawable.ColorDrawable(0x73000000) : null);
        View content=root.findViewById(R.id.main_nav_host);
        content.setImportantForAccessibility(open?View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        if(android.os.Build.VERSION.SDK_INT>=31) content.setRenderEffect(open?android.graphics.RenderEffect.createBlurEffect(4f,4f,android.graphics.Shader.TileMode.CLAMP):null);
        root.findViewById(R.id.fab_scrim).setVisibility(open ? View.VISIBLE : View.GONE);
        View actions = root.findViewById(R.id.fab_actions);
        actions.setVisibility(open ? View.VISIBLE : View.GONE);
        FloatingActionButton fab = root.findViewById(R.id.main_fab);
        fab.animate().rotation(open ? 45f : 0f).setDuration(180).start();
        fab.setContentDescription(open ? "Cerrar acciones rápidas" : "Abrir acciones rápidas");
        if (open) { actions.setAlpha(0f); actions.setTranslationY(MobileUi.dp(requireContext(), 12));
            actions.animate().alpha(1f).translationY(0f).setDuration(180).start(); }
    }
    private void closeSpeedDial() { if (speedDialOpen) setSpeedDial(false); }
    private void destination(int route) {
        closeSpeedDial();
        if (navController == null || navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() == route) return;
        navController.navigate(route, null, new NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
            .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true).build());
    }
    private void quickCreate(int route, boolean animal) {
        closeSpeedDial(); destination(route);
        if (root == null) return;
        root.post(() -> {
            if (host == null || !isAdded()) return;
            Fragment active = host.getChildFragmentManager().getPrimaryNavigationFragment();
            if (active instanceof OperationalFragment) ((OperationalFragment) active).quickCreate(animal);
        });
    }
    private void unavailableAction(String title, String message) {
        if (sheet != null) sheet.dismiss();
        sheet = new MobileUi.Sheet(requireContext(), title);
        sheet.row("Servicio pendiente", message);
        sheet.primary("Entendido", () -> sheet.dismiss());
        sheet.show();
    }
    @Override public void onDestroyView() {
        if (connectivityManager != null && networkCallback != null) {
            try { connectivityManager.unregisterNetworkCallback(networkCallback); } catch (RuntimeException ignored) { }
        }
        connectivityManager = null; networkCallback = null; offlineStore = null; session = null;
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        if (navController != null && destinationListener != null) navController.removeOnDestinationChangedListener(destinationListener);
        root = null; host = null; navController = null; destinationListener = null; dialBack=null;
        super.onDestroyView();
    }
}
