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
    private static volatile EdgeSwipeService connected;
    private volatile View handle; private WindowManager windows;
    private static int panelCount; private int gestureCount; private String lastFailure="none";
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changed=(p,k)-> { if("style".equals(k)||(k!=null&&k.startsWith("appearance."))) remove(); refresh(); };
    static boolean active() { return connected!=null && connected.handle!=null; }
    static void panel(boolean shown) { panelCount=Math.max(0,panelCount+(shown?1:-1)); if(connected!=null) connected.refresh(); }
    static android.graphics.Rect activationBounds() {
        android.graphics.Rect result=new android.graphics.Rect(); if(connected!=null && connected.handle!=null) { int[] p=new int[2]; connected.handle.getLocationOnScreen(p); result.set(p[0],p[1],p[0]+connected.handle.getWidth(),p[1]+connected.handle.getHeight()); } return result;
    }
    static String diagnostics() { return connected==null?"not connected":"bounds="+activationBounds()+", gestures="+connected.gestureCount+", failure="+connected.lastFailure; }
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
        if(panelCount>0 || !enabled || (lock!=null && lock.isDeviceLocked())) { remove(); return; }
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
        int width=Math.min(Ui.dp(this,96),getResources().getDisplayMetrics().widthPixels/2);
        WindowManager.LayoutParams position=new WindowManager.LayoutParams(width,Ui.dp(this,16),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
        if(Build.VERSION.SDK_INT>=30) position.setFitInsetsTypes(0);
        // Start below Android's reserved status-bar gesture band instead of fighting SystemUI.
        int status=Ui.dp(this,24);
        if(Build.VERSION.SDK_INT>=30) status=Math.max(status,windows.getCurrentWindowMetrics().getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top);
        position.y=status+Ui.dp(this,4); position.x=Ui.dp(this,12);
        position.gravity=Gravity.TOP|Gravity.RIGHT; position.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        try { windows.addView(area,position); handle=area; } catch(RuntimeException denied) { Ui.toast(this,"Edge swipe unavailable; use the Panel tile"); }
    }
    private void openPanel() {
        gestureCount++;
        if(Build.VERSION.SDK_INT>=31) performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
        try { startActivity(PanelIntents.fromTile(this)); } catch(RuntimeException denied) { lastFailure=denied.getClass().getSimpleName(); Ui.toast(this,"Open Personal Panel from its tile"); }
    }
    private void remove() { if(handle!=null) { try { windows.removeView(handle); } catch(RuntimeException detached) { } handle=null; } }
    @Override public void onDestroy() { remove(); getSharedPreferences("mobile_home",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(changed); if(connected==this) connected=null; super.onDestroy(); }
}
