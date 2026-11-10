package com.myfarmio.app.ui.tasks;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.myfarmio.app.ui.common.OperationalFragment;
import com.myfarmio.app.R;

/** Native Tareas presentation backed by the existing read endpoint. */
public class TasksFragment extends OperationalFragment {
    @Override protected int screenLayout() { return R.layout.fragment_tasks; }
    @Override protected String module() { return "tasks"; }
    @Override protected String title() { return "Tareas"; }
    @Override protected String searchHint() { return "Buscar tarea o campo"; }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setTopMargin(view.findViewById(R.id.collection_title), 0);
        setTopMargin(view.findViewById(R.id.collection_search_box), 12);
        setTopMargin(view.findViewById(R.id.collection_toolbar), 4);
        setTopMargin(view.findViewById(R.id.collection_list), 10);
    }

    private void setTopMargin(View view, int dp) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (!(raw instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) raw;
        margins.topMargin = Math.round(dp * view.getResources().getDisplayMetrics().density);
        view.setLayoutParams(margins);
    }
}
