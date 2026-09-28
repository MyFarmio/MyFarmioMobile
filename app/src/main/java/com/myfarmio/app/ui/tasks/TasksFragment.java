package com.myfarmio.app.ui.tasks;

import com.myfarmio.app.ui.common.OperationalFragment;
import com.myfarmio.app.R;

/** Native Tareas presentation backed by the existing read endpoint. */
public class TasksFragment extends OperationalFragment {
    @Override protected int screenLayout() { return R.layout.fragment_tasks; }
    @Override protected String module() { return "tasks"; }
    @Override protected String title() { return "Tareas"; }
    @Override protected String searchHint() { return "Buscar tarea o campo"; }
}
