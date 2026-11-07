package com.sadique.launcher.mobile;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;

/** User-enabled edge handle only. No window-content retrieval, key filtering or automatic UI actions. */
public final class EdgeSwipeService extends AccessibilityService {
    private static EdgeSwipeService connected;
    private View handle; private WindowManager windows; private boolean panelVisible;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changed=(p,k)-> { if("style".equals(k)||(k!=null&&k.startsWith("appearance."))) remove(); refresh(); };
    static boolean active() { return connected!=null && connected.handle!=null; }
    static void panel(boolean shown) { if(connected!=null) { connected.panelVisible=shown; connected.refresh(); } }
    static void stop() { if(connected!=null) connected.disableSelf(); }
    @Override protected void onServiceConnected() {
        connected=this; windows=getSystemService(WindowManager.class); getSharedPreferences("mobile_home",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(changed); refresh();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { refresh(); }
    @Override public void onInterrupt() { }
    @Override public void onConfigurationChanged(android.content.res.Configuration c) { super.onConfigurationChanged(c); remove(); refresh(); }
    private void refresh() {
        if(windows==null) return;
        KeyguardManager lock=getSystemService(KeyguardManager.class);
        boolean enabled=getSharedPreferences("mobile_home",MODE_PRIVATE).getBoolean("edge_enabled",true);
        if(panelVisible || !enabled || (lock!=null && lock.isDeviceLocked())) { remove(); return; }
        if(handle!=null) return;
        android.content.SharedPreferences prefs=getSharedPreferences("mobile_home",MODE_PRIVATE); MobileTheme theme=new MobileTheme(prefs);
        android.widget.FrameLayout area=new android.widget.FrameLayout(this); area.setContentDescription("Personal panel: swipe down");
        View line=new View(this); android.graphics.drawable.GradientDrawable dot=new android.graphics.drawable.GradientDrawable(); dot.setColor(theme.accent); dot.setCornerRadius(Ui.dp(this,3)); line.setBackground(dot);
        android.widget.FrameLayout.LayoutParams lp=new android.widget.FrameLayout.LayoutParams(Ui.dp(this,38),Ui.dp(this,3),Gravity.TOP|Gravity.CENTER_HORIZONTAL); lp.topMargin=Ui.dp(this,3); area.addView(line,lp);
        area.setOnTouchListener(new View.OnTouchListener() {
            float y; boolean opened;
            public boolean onTouch(View v,MotionEvent e) {
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN) { y=e.getRawY(); opened=false; }
                if(!opened && e.getActionMasked()==MotionEvent.ACTION_MOVE && e.getRawY()-y>Ui.dp(EdgeSwipeService.this,40)) { opened=true; openPanel(); }
                if(e.getActionMasked()==MotionEvent.ACTION_UP && !opened) v.performClick(); return true;
            }
        }); area.setOnClickListener(v->openPanel());
        int width=getResources().getDisplayMetrics().widthPixels/2;
        WindowManager.LayoutParams position=new WindowManager.LayoutParams(width,Ui.dp(this,30),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        position.gravity=Gravity.TOP|Gravity.RIGHT; position.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        try { windows.addView(area,position); handle=area; } catch(RuntimeException denied) { Ui.toast(this,"Edge swipe unavailable; use the Panel tile"); }
    }
    private void openPanel() {
        if(Build.VERSION.SDK_INT>=31) performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
        try { startActivity(PanelIntents.fromTile(this)); } catch(RuntimeException denied) { Ui.toast(this,"Open Personal Panel from its tile"); }
    }
    private void remove() { if(handle!=null) { try { windows.removeView(handle); } catch(RuntimeException detached) { } handle=null; } }
    @Override public void onDestroy() { remove(); getSharedPreferences("mobile_home",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(changed); if(connected==this) connected=null; super.onDestroy(); }
}
