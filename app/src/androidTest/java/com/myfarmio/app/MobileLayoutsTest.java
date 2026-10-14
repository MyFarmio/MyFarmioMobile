package com.myfarmio.app;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Spinner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Runs on an Android device. Does not log in, clear a session or write backend data. */
@RunWith(AndroidJUnit4.class)
public class MobileLayoutsTest {
    @Test public void priorityLayoutsInflateUnderTheActualTheme() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context themed = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_MyFarmio);
            LayoutInflater inflater = LayoutInflater.from(themed);
            int[] layouts = {R.layout.fragment_dashboard, R.layout.fragment_tasks, R.layout.fragment_fields,
                R.layout.fragment_livestock, R.layout.fragment_secondary,
                R.layout.fragment_climate, R.layout.fragment_profile, R.layout.fragment_login, R.layout.sheet_mobile,
                R.layout.item_mobile_record, R.layout.item_mobile_summary,
                R.layout.item_stitch_plot, R.layout.item_stitch_herd, R.layout.item_stitch_animal,
                R.layout.item_stitch_supply, R.layout.item_stitch_transaction,
                R.layout.item_stitch_report, R.layout.item_stitch_window,
                R.layout.item_stitch_climate_alert, R.layout.item_stitch_input};
            for (int layout : layouts) assertNotNull(inflater.inflate(layout, new FrameLayout(themed), false));
        });
    }
    @Test public void dashboardLoadingIdIsAContainerNotAProgressBar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context themed = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_MyFarmio);
            View dashboard = LayoutInflater.from(themed).inflate(R.layout.fragment_dashboard, new FrameLayout(themed), false);
            assertTrue(dashboard.findViewById(R.id.progress_overlay) instanceof FrameLayout);
        });
    }
    @Test public void legacyTaskDetailStillInflates() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context themed = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_MyFarmio);
            View detail = LayoutInflater.from(themed).inflate(R.layout.dialog_task_detail, new FrameLayout(themed), false);
            assertNotNull(detail.findViewById(R.id.tv_detail_responsible));
            assertTrue(detail.findViewById(R.id.spinner_detail_status) instanceof Spinner);
        });
    }
}
