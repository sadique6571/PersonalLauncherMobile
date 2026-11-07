package com.sadique.launcher.mobile;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;

/** Full-height capsule fill with touch and accessibility progress actions. */
final class LevelSlider extends View {
    final MobileTheme theme; final String label,symbol; final java.util.function.IntConsumer change;
    final Paint ink=new Paint(3); final RectF bounds=new RectF(); final Path clip=new Path();
    final Symbols plainIcon,filledIcon;
    int value;
    LevelSlider(Context c,MobileTheme t,String label,String symbol,int value,java.util.function.IntConsumer change) {
        super(c); plainIcon=new Symbols(symbol,t.ink); filledIcon=new Symbols(symbol,t.accentInk()); theme=t; this.label=label; this.symbol=symbol; this.change=change; this.value=value; setFocusable(true); setClickable(true); describe();
        android.graphics.drawable.GradientDrawable shell=t.panel(c.getResources().getDisplayMetrics().density,false); shell.setCornerRadius(Ui.dp(c,28)); setBackground(shell);
    }
    private void describe() { setContentDescription(label+" "+value+" percent"); }
    @Override protected void onSizeChanged(int w,int h,int ow,int oh) { bounds.set(0,0,w,h); clip.reset(); clip.addRoundRect(bounds,Ui.dp(getContext(),28),Ui.dp(getContext(),28),Path.Direction.CW); }
    @Override protected void onDraw(Canvas c) {
        int save=c.save(); c.clipPath(clip); float top=getHeight()*(1-value/100f);
        ink.setColor(theme.accent); ink.setAlpha(isEnabled()?240:100); c.drawRect(0,top,getWidth(),getHeight(),ink);
        int size=Ui.dp(getContext(),26),left=(getWidth()-size)/2,y=Ui.dp(getContext(),18);
        plainIcon.setBounds(left,y,left+size,y+size); filledIcon.setBounds(left,y,left+size,y+size); plainIcon.draw(c);
        ink.setAlpha(255); ink.setColor(theme.ink); ink.setTextAlign(Paint.Align.CENTER); ink.setTypeface(theme.font); ink.setTextSize(Ui.dp(getContext(),22));
        float baseline=getHeight()-Ui.dp(getContext(),22); c.drawText(value+"%",getWidth()/2f,baseline,ink);
        int fill=c.save(); c.clipRect(0,top,getWidth(),getHeight()); filledIcon.draw(c); ink.setColor(theme.accentInk()); c.drawText(value+"%",getWidth()/2f,baseline,ink); c.restoreToCount(fill); c.restoreToCount(save);
    }
    void setLevel(int value) { this.value=Math.max(0,Math.min(100,value)); describe(); invalidate(); }
    private void adjust(int value) { this.value=Math.max(0,Math.min(100,value)); describe(); invalidate(); change.accept(this.value); }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(!isEnabled()) return false;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN || e.getActionMasked()==MotionEvent.ACTION_MOVE || e.getActionMasked()==MotionEvent.ACTION_UP) {
            adjust(Math.round(100*(1-e.getY()/getHeight()))); if(e.getActionMasked()==MotionEvent.ACTION_UP) performClick(); return true;
        } return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) { super.onInitializeAccessibilityNodeInfo(info); info.setClassName(android.widget.SeekBar.class.getName()); info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,0,100,value)); if(isEnabled()) info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS); }
    @Override public boolean performAccessibilityAction(int action,Bundle data) { if(isEnabled() && action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId() && data!=null) { adjust(Math.round(data.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE))); return true; } return super.performAccessibilityAction(action,data); }
}
