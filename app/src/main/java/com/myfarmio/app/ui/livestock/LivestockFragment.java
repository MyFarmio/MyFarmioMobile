package com.myfarmio.app.ui.livestock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.myfarmio.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Mobile livestock surface. It intentionally keeps its small local data set isolated until the
 * Android app receives the livestock REST repository already available in the web product.
 */
public class LivestockFragment extends Fragment {
    private final List<Animal> animals = new ArrayList<>();
    private LinearLayout container;
    private TextView total;
    private TextView count;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_livestock, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        container = view.findViewById(R.id.ll_livestock_container);
        total = view.findViewById(R.id.tv_livestock_total);
        count = view.findViewById(R.id.tv_livestock_count);
        seedIfNeeded();
        render("");
        TextInputEditText search = view.findViewById(R.id.input_livestock_search);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            public void onTextChanged(CharSequence s, int st, int b, int c) { render(s.toString()); }
            public void afterTextChanged(android.text.Editable s) { }
        });
        view.findViewById(R.id.btn_livestock_filter).setOnClickListener(v ->
            new MaterialAlertDialogBuilder(requireContext()).setTitle("Ganado")
                .setMessage("La búsqueda permite localizar por caravana, rodeo o categoría.")
                .setPositiveButton("Entendido", null).show());
    }

    private void seedIfNeeded() {
        if (!animals.isEmpty()) return;
        animals.add(new Animal("Caravana 184", "Vaca · Rodeo Norte", "En seguimiento", "482 kg", "Potrero 3"));
        animals.add(new Animal("Caravana 241", "Vaquillona · Rodeo Cría", "Control hoy", "416 kg", "Corral sanitario"));
        animals.add(new Animal("Caravana 097", "Toro · Reproductores", "Óptimo", "728 kg", "Potrero 1"));
    }

    private void render(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        container.removeAllViews(); int visible = 0;
        for (Animal animal : animals) {
            if (!normalized.isEmpty() && !(animal.tag + animal.meta).toLowerCase().contains(normalized)) continue;
            View row = getLayoutInflater().inflate(R.layout.item_livestock_animal, container, false);
            ((TextView) row.findViewById(R.id.tv_animal_tag)).setText(animal.tag);
            ((TextView) row.findViewById(R.id.tv_animal_meta)).setText(animal.meta);
            ((TextView) row.findViewById(R.id.tv_animal_status)).setText(animal.status.toUpperCase());
            ((TextView) row.findViewById(R.id.tv_animal_weight)).setText(animal.weight);
            ((TextView) row.findViewById(R.id.tv_animal_location)).setText(animal.location);
            row.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle(animal.tag)
                .setMessage(animal.meta + "\n" + animal.status + "\n" + animal.weight + " · " + animal.location)
                .setPositiveButton("Cerrar", null).show());
            container.addView(row); visible++;
        }
        total.setText(animals.size() + " animales");
        count.setText(String.valueOf(visible));
    }

    private static class Animal { final String tag, meta, status, weight, location; Animal(String t, String m, String s, String w, String l) { tag=t;meta=m;status=s;weight=w;location=l; } }
}
