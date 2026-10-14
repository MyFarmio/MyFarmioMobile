package com.myfarmio.app.ui.main;
import static com.myfarmio.app.ui.common.RecordText.*;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myfarmio.app.R;
import com.myfarmio.app.auth.SessionManager;
import com.myfarmio.app.ui.common.*;
import java.util.*;

public class ProfileFragment extends Fragment {
    private MobileUi.Sheet sheet;
    private WorkspaceViewModel workspace;
    private SessionManager session;
    private View root;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state) { return inflater.inflate(R.layout.fragment_profile,parent,false); }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        root=view; session=SessionManager.getInstance(requireContext());
        workspace=new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class); workspace.prepare(session);
        workspace.revision.observe(getViewLifecycleOwner(),revision->render());
        view.findViewById(R.id.profile_edit).setOnClickListener(v->{
            if(!workspace.isDemo()) { info("Editar perfil","Podés actualizar tu perfil desde la web."); return; }
            if(sheet!=null) sheet.dismiss(); sheet=new MobileUi.Sheet(requireContext(),"Editar perfil de demo");
            StitchDemoEditor.bind(sheet,workspace,"profile",StitchUi.demo(workspace,"profile")); sheet.show();
        });
        view.findViewById(R.id.profile_billing).setOnClickListener(v->info("Facturación y planes",workspace.isDemo()?"La demostración no tiene suscripción ni cargos.":"Administrá tu suscripción desde la web."));
        view.findViewById(R.id.profile_sessions).setOnClickListener(v->info("Sesiones y dispositivos",workspace.isDemo()?"Sesión local de demostración en este dispositivo.":"Consultá tus dispositivos vinculados desde la web."));
        view.findViewById(R.id.profile_password).setOnClickListener(v->info("Métodos de acceso",workspace.isDemo()?"La demo no utiliza contraseña.":"Gestioná tus métodos de acceso desde la web."));
        view.findViewById(R.id.profile_logout).setOnClickListener(v->new MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Cerrar sesión?").setMessage("Los datos de demostración de esta sesión se descartarán. Los registros de tu organización no se eliminan.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Cerrar sesión",(d,w)->{
                session.clearSession(); workspace.clear();
                NavHostFragment.findNavController(requireParentFragment().requireParentFragment()).navigate(R.id.action_main_to_welcome);
            }).show());
        view.findViewById(R.id.profile_mode).setOnClickListener(v->workspace.setDemo(!workspace.isDemo()));
    }
    private void render() {
        Map<String,Object> data=StitchUi.demo(workspace,"profile"); boolean demo=workspace.isDemo();
        String name=demo?value(data,"name"):safe(session.getUserName(),"Sin nombre");
        String org=demo?value(data,"organization"):safe(session.getOrganizationName(),"No informada");
        MobileUi.text(root,R.id.profile_avatar,name.isEmpty()?"?":name.substring(0,1).toUpperCase());
        MobileUi.text(root,R.id.profile_name,name);
        MobileUi.text(root,R.id.profile_email,demo?value(data,"email"):safe(session.getUserEmail(),"Correo no informado"));
        MobileUi.text(root,R.id.profile_organization,"Organización: "+org);
        MobileUi.text(root,R.id.profile_role_badge,demo?value(data,"role").toUpperCase():"MI PERFIL");
        MobileUi.text(root,R.id.profile_provider_badge,demo?"DEMO LOCAL":"CUENTA");
        MobileUi.text(root,R.id.profile_identity_title,demo?"Identidad y contacto":"Datos personales");
        int[] personal={R.id.profile_personal_0,R.id.profile_personal_1,R.id.profile_personal_2,R.id.profile_personal_3,R.id.profile_personal_4,R.id.profile_personal_5};
        String[] keys={"name","role","phone","timezone","created","last_access"};
        for(int i=0;i<personal.length;i++) MobileUi.text(root,personal[i],i==0?name:or(data,keys[i],"No informado"));
        int[] membership={R.id.profile_member_0,R.id.profile_member_1,R.id.profile_member_2,R.id.profile_member_3,R.id.profile_member_4};
        String[] memberKeys={"organization","slug","membership_role","membership_title","created"};
        for(int i=0;i<membership.length;i++) MobileUi.text(root,membership[i],i==0?org:or(data,memberKeys[i],"No informado"));
        MobileUi.text(root,R.id.profile_membership_status,demo?"EJEMPLO":"SIN CONSULTA");
        MobileUi.text(root,R.id.profile_permission_badge,demo?"DEMO":"—");
        MobileUi.text(root,R.id.profile_permissions_detail,demo?"Explorá los módulos con datos ficticios. Las altas, ediciones y eliminaciones se guardan solo durante esta sesión.":"Los permisos detallados de tu organización todavía no están disponibles en esta vista.");
        int[] permissions={R.id.profile_permission_0,R.id.profile_permission_1,R.id.profile_permission_2,R.id.profile_permission_3,R.id.profile_permission_4,R.id.profile_permission_5};
        for(int id:permissions) MobileUi.text(root,id,demo?"EXPLORAR DEMO":"SIN INFORMACIÓN");
        root.findViewById(R.id.profile_mode).setVisibility(workspace.isDemoAccount()?View.GONE:View.VISIBLE);
        MobileUi.text(root,R.id.profile_mode,demo?"Volver a mi organización":"Explorar demostración");
        MobileUi.text(root,R.id.profile_footer,demo?"Entorno de demostración · datos locales ficticios.":"Gestión agropecuaria en tu dispositivo.");
        // TODO: Conectar membresía, permisos y edición a los contratos reales del perfil.
    }
    private String safe(String value,String fallback) { return value==null||value.isEmpty()?fallback:value; }
    private void info(String title,String detail) { if(sheet!=null)sheet.dismiss(); sheet=new MobileUi.Sheet(requireContext(),title); sheet.row("MyFarmio",detail); sheet.primary("Entendido",sheet::dismiss); sheet.show(); }
    @Override public void onDestroyView() { if(sheet!=null){sheet.dismiss();sheet=null;} root=null; super.onDestroyView(); }
}

