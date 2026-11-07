package com.sadique.launcher.mobile;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;

/** Takes over only a deliberate vertical Home gesture; child taps and scrolling keep working. */
final class GestureFrame extends FrameLayout {
    interface Route { boolean accept(boolean down,float rawY); void swipe(boolean down); }
    private float x,y; private boolean captured,down,allowDown,allowUp; private final Route route;
    GestureFrame(Context c,Route route) { super(c); this.route=route; }
    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN) { x=e.getRawX(); y=e.getRawY(); captured=false; allowDown=route.accept(true,y); allowUp=route.accept(false,y); }
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && !captured) {
            float dy=e.getRawY()-y;
            if(Math.abs(dy)>Ui.dp(getContext(),65) && Math.abs(dy)>Math.abs(e.getRawX()-x)*1.6f && (dy>0?allowDown:allowUp)) { captured=true; down=dy>0; return true; }
        }
        return false;
    }
    @Override public void requestDisallowInterceptTouchEvent(boolean disallow) { if(!disallow || (!allowDown&&!allowUp)) super.requestDisallowInterceptTouchEvent(disallow); }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(captured && e.getActionMasked()==MotionEvent.ACTION_UP) { route.swipe(down); captured=false; performClick(); return true; }
        return captured || super.onTouchEvent(e);
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
