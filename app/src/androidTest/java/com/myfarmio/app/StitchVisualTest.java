package com.myfarmio.app;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.myfarmio.app.auth.SessionManager;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;

/** Opt-in UI smoke/capture test. Never replaces a real user's session. */
@RunWith(AndroidJUnit4.class)
public class StitchVisualTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    @Test public void demoLoginNavigationScreensAndSpeedDial() throws Exception {
        Assume.assumeTrue("Pass -e visualDemo true on a disposable emulator",Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("visualDemo","false")));
        SessionManager session=SessionManager.getInstance(instrumentation.getTargetContext());
        Assume.assumeTrue("Do not alter real sessions",!session.isLoggedIn()||"demo-user-id".equals(session.getUserId()));
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            Thread.sleep(1200); instrumentation.waitForIdleSync();
            if(!session.isLoggedIn()) {
                scenario.onActivity(activity->((NavHostFragment)activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)).getNavController().navigate(R.id.loginFragment));
                instrumentation.waitForIdleSync();
                scenario.onActivity(activity->{assertNotNull(activity.findViewById(R.id.button_demo));activity.findViewById(R.id.button_demo).performClick();});
                Thread.sleep(1200);instrumentation.waitForIdleSync();
            }
            int[] routes={R.id.nav_dashboard,R.id.nav_campos,R.id.nav_ganado,R.id.nav_inventario,R.id.nav_finanzas,R.id.nav_clima,R.id.nav_perfil};
            String[] names={"dashboard","campos","ganado","inventario","finanzas","clima","perfil"};
            for(int i=0;i<routes.length;i++) {
                final int route=routes[i];
                scenario.onActivity(activity->host(activity).getNavController().navigate(route));
                instrumentation.waitForIdleSync();Thread.sleep(250);
                scenario.onActivity(activity->{ assertNotNull(host(activity).getChildFragmentManager().getPrimaryNavigationFragment()); checkVisibleText(activity.getWindow().getDecorView()); });
                capture(names[i]+"-top");
                for(int page=1;page<5;page++) {
                    final boolean[] moved={false};
                    scenario.onActivity(activity->{
                        Fragment active=host(activity).getChildFragmentManager().getPrimaryNavigationFragment();
                        NestedScrollView scroll=findScroll(active.requireView());
                        if(scroll!=null) { int before=scroll.getScrollY();scroll.scrollBy(0,(int)(scroll.getHeight()*0.8));moved[0]=before!=scroll.getScrollY(); }
                    });
                    instrumentation.waitForIdleSync();
                    if(!moved[0])break;
                    Thread.sleep(120); capture(names[i]+"-"+page);
                }
            }
            scenario.onActivity(activity->host(activity).getNavController().navigate(R.id.nav_dashboard));
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity->activity.findViewById(R.id.main_fab).performClick());
            Thread.sleep(300);capture("fab-open");
            scenario.onActivity(activity->{assertEquals(View.VISIBLE,activity.findViewById(R.id.fab_actions).getVisibility());activity.getOnBackPressedDispatcher().onBackPressed();});
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity->assertEquals(View.GONE,activity.findViewById(R.id.fab_actions).getVisibility()));
        }
    }
    private NavHostFragment host(MainActivity activity) {
        NavHostFragment outer=(NavHostFragment)activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        Fragment main=outer.getChildFragmentManager().getPrimaryNavigationFragment();
        return (NavHostFragment)main.getChildFragmentManager().findFragmentById(R.id.main_nav_host);
    }
    private NestedScrollView findScroll(View view) {
        if(view instanceof NestedScrollView)return (NestedScrollView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){NestedScrollView found=findScroll(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private void checkVisibleText(View view) {
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView) {
            TextView text=(TextView)view;
            if(text.getLayout()!=null&&text.getGlobalVisibleRect(new android.graphics.Rect())&&text.getId()!=R.id.header_organization)
                for(int line=0;line<text.getLayout().getLineCount();line++) assertEquals("Truncated: "+text.getText(),0,text.getLayout().getEllipsisCount(line));
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checkVisibleText(((ViewGroup)view).getChildAt(i));
    }
    private void capture(String name) throws IOException {
        Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File directory=new File(instrumentation.getTargetContext().getExternalFilesDir(null),"stitch-qa");assertTrue(directory.exists()||directory.mkdirs());
        try(FileOutputStream output=new FileOutputStream(new File(directory,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}
        bitmap.recycle();
    }
}

