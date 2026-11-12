package com.sadique.launcher.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Full-screen rendering of the original offline V17 mechanical movement. */
public final class HarmonyV17ClockActivity extends Activity {
    private HarmonyV17ClockView watch;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff071525);
        getWindow().setNavigationBarColor(0xff071525);
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(0xff071525);
        watch=new HarmonyV17ClockView(this);
        root.addView(watch,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
    }
    @Override protected void onResume(){super.onResume();if(watch!=null)watch.active(true);}
    @Override protected void onPause(){if(watch!=null)watch.active(false);super.onPause();}
    @Override protected void onDestroy(){if(watch!=null){watch.close();watch=null;}super.onDestroy();}
}
