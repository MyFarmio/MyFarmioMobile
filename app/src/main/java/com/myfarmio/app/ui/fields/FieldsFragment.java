package com.myfarmio.app.ui.fields;

import com.myfarmio.app.ui.common.OperationalFragment;
import com.myfarmio.app.R;

/** Native Campos presentation backed by the existing read endpoint. */
public class FieldsFragment extends OperationalFragment {
    @Override protected int screenLayout() { return R.layout.fragment_fields; }
    @Override protected String module() { return "fields"; }
    @Override protected String title() { return "Campos"; }
    @Override protected String searchHint() { return "Buscar lote, cultivo o zona"; }
}
