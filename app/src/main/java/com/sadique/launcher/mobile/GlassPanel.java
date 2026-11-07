package com.sadique.launcher.mobile;

import android.graphics.*;
import android.graphics.drawable.GradientDrawable;

/** A cached bevel over the selected palette; window blur is a separate, optional OS effect. */
final class GlassPanel extends GradientDrawable {
    final Paint rim=new Paint(3); final RectF edge=new RectF(); final float unit;
    GlassPanel(float density) { unit=density; rim.setStyle(Paint.Style.STROKE); rim.setStrokeWidth(Math.max(1,density)); }
    @Override protected void onBoundsChange(Rect b) {
        super.onBoundsChange(b); edge.set(b); edge.inset(unit,unit);
        rim.setShader(new LinearGradient(b.left,b.top,b.right,Math.max(b.top+1,b.bottom),new int[]{0x88ffffff,0x08ffffff,0x337fabc8},new float[]{0,.55f,1},Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas c) { super.draw(c); rim.setAlpha(getAlpha()); c.drawRoundRect(edge,Math.max(0,getCornerRadius()-unit),Math.max(0,getCornerRadius()-unit),rim); }
}
