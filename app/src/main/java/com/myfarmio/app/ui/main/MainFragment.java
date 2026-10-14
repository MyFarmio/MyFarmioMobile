package com.myfarmio.app.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
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

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_main, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        SessionManager session = SessionManager.getInstance(requireContext());
        workspace = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
        workspace.prepare(session);
        String organization = session.getOrganizationName();
        workspace.revision.observe(getViewLifecycleOwner(), revision -> {
            java.util.Map<String,Object> weather = com.myfarmio.app.ui.common.StitchUi.demo(workspace,"weather");
            MobileUi.text(view,R.id.header_weather,com.myfarmio.app.ui.common.RecordText.or(weather,"temperature","—"));
            if(workspace.isDemo()) {
                String demoName=com.myfarmio.app.ui.common.RecordText.or(com.myfarmio.app.ui.common.StitchUi.demo(workspace,"profile"),"name","Demo");
                MobileUi.text(view,R.id.header_profile,demoName.substring(0,1).toUpperCase());
            }
            if (navController == null || navController.getCurrentDestination() == null
                || (navController.getCurrentDestination().getId() != R.id.nav_clima && navController.getCurrentDestination().getId() != R.id.nav_perfil))
                MobileUi.text(view, R.id.header_organization, workspace.isDemo() ? "Mi campo demo · DEMO LOCAL"
                    : organization == null || organization.isEmpty() ? "Sin organización activa" : organization);
        });
        String name = workspace.isDemo() ? com.myfarmio.app.ui.common.RecordText.or(com.myfarmio.app.ui.common.StitchUi.demo(workspace,"profile"),"name","Demo") : session.getUserName();
        if (name != null && !name.trim().isEmpty()) ((TextView) view.findViewById(R.id.header_profile)).setText(name.trim().substring(0, 1).toUpperCase());
        view.findViewById(R.id.header_profile).setOnClickListener(v -> destination(R.id.nav_perfil));
        view.findViewById(R.id.header_weather).setOnClickListener(v -> destination(R.id.nav_clima));
        view.findViewById(R.id.header_logo).setOnClickListener(v -> {
            if (navController != null && !navController.popBackStack()) destination(R.id.nav_dashboard);
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
            MobileUi.enter(view.findViewById(R.id.main_nav_host));
            int id = destination.getId();
            bottomNav.setTag(R.id.nav_perfil, id == R.id.nav_perfil);
            bottomNav.setVisibility(id == R.id.nav_perfil ? View.GONE : View.VISIBLE);
            boolean secondary = id == R.id.nav_perfil || id == R.id.nav_clima || id == R.id.nav_tareas;
            ImageView logo = view.findViewById(R.id.header_logo);
            logo.setImageResource(secondary ? R.drawable.ic_arrow_back_stitch : R.drawable.ic_iconmyfarmio);
            logo.setContentDescription(secondary ? "Volver" : "MyFarmio");
            MobileUi.text(view, R.id.header_brand, id == R.id.nav_perfil ? "Mi Perfil" : "MyFarmio");
            if (secondary) MobileUi.text(view, R.id.header_organization, workspace.isDemo() ? "DEMO LOCAL" : id == R.id.nav_clima ? "AGROMETEOROLOGÍA" : "");
            else MobileUi.text(view, R.id.header_organization, workspace.isDemo() ? "Mi campo demo · DEMO LOCAL" : organization == null ? "Sin organización activa" : organization);
            view.findViewById(R.id.header_weather).setVisibility(id == R.id.nav_perfil ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.main_fab).setVisibility(id == R.id.nav_perfil || id == R.id.nav_clima ? View.GONE : View.VISIBLE);
            if (secondary) {
                for(int index=0;index<bottomNav.getMenu().size();index++) {
                    android.view.MenuItem item=bottomNav.getMenu().getItem(index);
                    item.setCheckable(false); item.setChecked(false); item.setCheckable(true);
                }
            }
        };
        navController.addOnDestinationChangedListener(destinationListener);
        FloatingActionButton fab = view.findViewById(R.id.main_fab);
        fab.setOnClickListener(v -> setSpeedDial(!speedDialOpen));
        view.findViewById(R.id.fab_scrim).setOnClickListener(v -> closeSpeedDial());
        view.findViewById(R.id.fab_field).setOnClickListener(v -> quickCreate(R.id.nav_campos, false));
        view.findViewById(R.id.fab_animal).setOnClickListener(v -> quickCreate(R.id.nav_ganado, true));
        view.findViewById(R.id.fab_supply).setOnClickListener(v -> quickSecondary(R.id.nav_inventario));
        view.findViewById(R.id.fab_movement).setOnClickListener(v -> quickSecondary(R.id.nav_finanzas));
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
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        if (navController != null && destinationListener != null) navController.removeOnDestinationChangedListener(destinationListener);
        root = null; host = null; navController = null; destinationListener = null; dialBack=null;
        super.onDestroyView();
    }
}
