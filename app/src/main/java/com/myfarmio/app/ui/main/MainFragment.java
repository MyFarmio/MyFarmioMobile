package com.myfarmio.app.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.ui.common.MobileUi;

public class MainFragment extends Fragment {
    private NavController navController;
    private MobileUi.Sheet sheet;
    private com.myfarmio.app.ui.common.WorkspaceViewModel workspace;
    private NavController.OnDestinationChangedListener destinationListener;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_main, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        SessionManager session = SessionManager.getInstance(requireContext());
        workspace = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.myfarmio.app.ui.common.WorkspaceViewModel.class);
        workspace.prepare(session);
        String organization = session.getOrganizationName();
        MobileUi.text(view, R.id.header_organization, organization == null || organization.isEmpty() ? "Sin organización activa" : organization);
        workspace.revision.observe(getViewLifecycleOwner(), revision -> MobileUi.text(view, R.id.header_organization,
            workspace.isDemo() ? "DEMO · Solo esta sesión" : organization == null ? "Sin organización activa" : organization));
        view.findViewById(R.id.header_profile).setOnClickListener(v -> profile());
        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_nav);
        NavHostFragment host = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.main_nav_host);
        if (host != null) {
            navController = host.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
            destinationListener = (controller, destination, args) -> {
                MobileUi.enter(view.findViewById(R.id.main_nav_host));
                if (destination.getId() == R.id.nav_ganado || destination.getId() == R.id.nav_inventario || destination.getId() == R.id.nav_finanzas)
                    bottomNav.getMenu().findItem(R.id.nav_more).setChecked(true);
            };
            navController.addOnDestinationChangedListener(destinationListener);
            // Keep NavigationUI saved tab stacks; Más opens a sheet, not a false destination.
            bottomNav.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_more) { more(); return false; }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });
        }
    }
    private MobileUi.Sheet open(String title) {
        MobileUi.hideKeyboard(getView());
        if (sheet != null) sheet.dismiss();
        sheet = new MobileUi.Sheet(requireContext(), title);
        return sheet;
    }
    private void destination(int route) {
        if (sheet != null) sheet.dismiss();
        navController.navigate(route, null, new androidx.navigation.NavOptions.Builder().setLaunchSingleTop(true)
            .setRestoreState(true).setPopUpTo(navController.getGraph().getStartDestinationId(),false,true).build());
    }
    private void more() {
        MobileUi.Sheet current = open("Más de MyFarmio");
        current.heading("Tu operación");
        current.button("Ganado · Rodeos y animales", () -> destination(R.id.nav_ganado));
        current.button("Inventario · Insumos y existencias", () -> destination(R.id.nav_inventario));
        current.button("Finanzas · Registros y movimientos", () -> destination(R.id.nav_finanzas));
        current.heading("Tu espacio");
        current.button("Mi perfil y organización", this::profile);
        current.row(workspace.isDemo() ? "Modo demostración" : "Datos de tu organización", workspace.isDemo()
            ? "Los registros son ficticios. Podés probar cambios sin afectar datos reales. Se reinician al cerrar sesión o el proceso."
            : "Se conservan tus consultas y permisos existentes. No se habilitan escrituras reales desde Android.");
        if (!workspace.isDemoAccount()) current.button(workspace.isDemo() ? "Volver a mi organización" : "Explorar demostración", () -> {
            current.dismiss(); workspace.setDemo(!workspace.isDemo());
        });
        current.row("Otros módulos", "Clima, calendario, registros y reportes aún no tienen rutas móviles operativas. No se inventan accesos ni permisos.");
        current.show();
    }
    private void profile() {
        SessionManager session = SessionManager.getInstance(requireContext());
        MobileUi.Sheet current = open("Mi perfil");
        current.row("Nombre", session.getUserName());
        current.row("Correo", session.getUserEmail());
        current.heading("Organización activa");
        current.row("Establecimiento", session.getOrganizationName());
        current.row("Acceso", "Se utiliza la sesión guardada. Rol y permisos detallados no disponibles en esta versión.");
        current.button("Cerrar sesión", () -> new MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Cerrar sesión?").setMessage("Vas a necesitar tus credenciales para volver a entrar. Los registros guardados en la web no se eliminan.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Cerrar sesión", (dialog, which) -> {
                current.dismiss(); session.clearSession(); workspace.clear();
                NavHostFragment.findNavController(this).navigate(R.id.action_main_to_welcome);
            }).show());
        current.show();
    }
    @Override public void onDestroyView() {
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        if (navController != null && destinationListener != null) navController.removeOnDestinationChangedListener(destinationListener);
        destinationListener = null;
        navController = null;
        super.onDestroyView();
    }
}
