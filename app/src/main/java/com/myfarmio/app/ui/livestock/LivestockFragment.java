package com.myfarmio.app.ui.livestock;

import com.myfarmio.app.ui.common.OperationalFragment;
import com.myfarmio.app.R;

/** Native Ganado presentation backed by the existing read endpoint. */
public class LivestockFragment extends OperationalFragment {
    @Override protected int screenLayout() { return R.layout.fragment_livestock; }
    @Override protected String module() { return "livestock"; }
    @Override protected String title() { return "Ganado"; }
    @Override protected String searchHint() { return "Buscar rodeo o categoría"; }
}
