package com.sadique.launcher.mobile;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;

/** Observes the Home touch stream even when a scrolling child disallows interception. */
final class GestureFrame extends FrameLayout {
    interface Route { boolean accept(boolean down,float rawY); void swipe(boolean down); }
    private float x,y,lastDistance;
    private boolean captured,down,allowDown,allowUp,tracking,routed;
    private int events;
    private final Route route;
    GestureFrame(Context c,Route route) { super(c); this.route=route; }
    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        events++;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN) {
            x=e.getRawX(); y=e.getRawY(); captured=false; routed=false; tracking=true;
            allowDown=route.accept(true,y); allowUp=route.accept(false,y);
        }
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && !captured) {
            float dy=e.getRawY()-y; lastDistance=dy;
            if(Math.abs(dy)>Ui.dp(getContext(),65) && Math.abs(dy)>Math.abs(e.getRawX()-x)*1.6f && (dy>0?allowDown:allowUp)) {
                MotionEvent cancel=MotionEvent.obtain(e); cancel.setAction(MotionEvent.ACTION_CANCEL); super.dispatchTouchEvent(cancel); cancel.recycle(); captured=true; down=dy>0;
            }
        }
        if(captured) {
            if(e.getActionMasked()==MotionEvent.ACTION_UP) { captured=false; tracking=false; routed=true; route.swipe(down); performClick(); }
            else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL) { captured=false; tracking=false; }
            return true;
        }
        boolean keep=tracking; boolean handled=super.dispatchTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL) tracking=false;
        return handled||keep;
    }
    String diagnostics() { return "events="+events+", dy="+lastDistance+", down="+allowDown+", up="+allowUp+", routed="+routed; }
    @Override public boolean performClick() { super.performClick(); return true; }
}
