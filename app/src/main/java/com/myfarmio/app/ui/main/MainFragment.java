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
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_main, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        SessionManager session = SessionManager.getInstance(requireContext());
        String organization = session.getOrganizationName();
        MobileUi.text(view, R.id.header_organization, organization == null || organization.isEmpty() ? "Sin organización activa" : organization);
        view.findViewById(R.id.header_profile).setOnClickListener(v -> profile());
        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_nav);
        NavHostFragment host = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.main_nav_host);
        if (host != null) {
            navController = host.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
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
    private void more() {
        MobileUi.Sheet current = open("Más de MyFarmio");
        current.heading("Tu cuenta");
        current.button("Mi perfil y organización", this::profile);
        current.heading("Operación");
        current.row("Accesos principales", "Inicio, Tareas, Campos y Ganado están disponibles en la barra inferior.");
        current.heading("Otros módulos");
        current.row("Disponibles en la versión web", "Clima, inventario, finanzas, calendario, registros y reportes todavía no tienen rutas operativas en Android.");
        current.row("Permisos", "Las consultas usan tu sesión. No se habilitan acciones de escritura sin permisos verificados.");
        // TODO: Conectar accesos secundarios a rutas reales y al catálogo de permisos.
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
                current.dismiss(); session.clearSession();
                NavHostFragment.findNavController(this).navigate(R.id.action_main_to_welcome);
            }).show());
        current.show();
    }
    @Override public void onDestroyView() {
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        navController = null;
        super.onDestroyView();
    }
}
