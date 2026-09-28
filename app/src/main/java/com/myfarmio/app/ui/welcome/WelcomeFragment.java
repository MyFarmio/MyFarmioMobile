package com.myfarmio.app.ui.welcome;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.myfarmio.app.R;
import com.myfarmio.app.ui.common.MobileUi;
public class WelcomeFragment extends Fragment {
    private int page;
    private final String[] titles={"Tu campo, en una mirada","Del pendiente a lo hecho","Cada lote. Cada animal."};
    private final String[] descriptions={
        "Reuní prioridades, cultivos y seguimiento del rodeo en un resumen claro para empezar el día.",
        "Organizá responsables, fechas y pasos. Encontrá la próxima tarea sin perder tiempo.",
        "Llevá la información de tu establecimiento a donde trabajás. Todo conectado, desde el teléfono."};
    private final int[] illustrations={R.drawable.illustration_farm,R.drawable.illustration_work,R.drawable.illustration_herd};
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_welcome,parent,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        if (OnboardingPreferences.completed(requireContext())) { NavHostFragment.findNavController(this).navigate(R.id.action_welcome_to_login); return; }
        if (state!=null) page=state.getInt("page",0);
        view.findViewById(R.id.onboard_skip).setOnClickListener(v->finish());
        view.findViewById(R.id.btn_login).setOnClickListener(v->{ if (page==2) finish(); else { page++; render(); } });
        view.findViewById(R.id.onboard_previous).setOnClickListener(v->{ if (page>0) { page--; render(); } });
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (page>0) { page--; render(); } else { setEnabled(false); requireActivity().getOnBackPressedDispatcher().onBackPressed(); }
            }
        });
        render();
    }
    private void render() {
        View view=requireView();
        MobileUi.text(view,R.id.onboard_title,titles[page]);
        MobileUi.text(view,R.id.onboard_description,descriptions[page]);
        MobileUi.text(view,R.id.onboard_step,"PASO "+(page+1)+" DE 3");
        MobileUi.text(view,R.id.btn_login,page==2 ? "Comenzar" : "Siguiente");
        view.findViewById(R.id.onboard_previous).setVisibility(page==0 ? View.GONE : View.VISIBLE);
        ((ImageView)view.findViewById(R.id.onboard_art)).setImageResource(illustrations[page]);
        LinearProgressIndicator progress=view.findViewById(R.id.onboard_progress);
        progress.setProgressCompat((page+1)*100/3,android.animation.ValueAnimator.areAnimatorsEnabled());
        progress.setContentDescription("Paso "+(page+1)+" de 3");
        MobileUi.enter(view.findViewById(R.id.onboard_content));
        view.findViewById(R.id.onboard_title).announceForAccessibility(titles[page]);
    }
    private void finish() {
        OnboardingPreferences.complete(requireContext());
        NavHostFragment.findNavController(this).navigate(R.id.action_welcome_to_login);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state); state.putInt("page",page); }
}
