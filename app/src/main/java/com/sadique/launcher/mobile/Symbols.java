package com.sadique.launcher.mobile;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Scalable stroke icons, shared by the Home toolbar and control center. */
final class Symbols extends Drawable {
    final String name; final int color; final Paint p=new Paint(3);
    Symbols(String name,int color) { this.name=name; this.color=color; p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
    @Override public void draw(Canvas c) {
        Rect b=getBounds(); int save=c.save(); c.translate(b.left,b.top); c.scale(b.width()/24f,b.height()/24f);
        p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f);
        switch(name) {
            case "search": c.drawCircle(10,10,6,p); c.drawLine(15,15,21,21,p); break;
            case "close": c.drawLine(6,6,18,18,p); c.drawLine(18,6,6,18,p); break;
            case "apps": for(int y=5;y<=17;y+=6) for(int x=5;x<=17;x+=6) { p.setStyle(Paint.Style.FILL); c.drawCircle(x,y,1.6f,p); } break;
            case "settings": c.drawCircle(12,12,4,p); for(int i=0;i<8;i++) { double a=i*Math.PI/4; c.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,p); } c.drawCircle(12,12,8,p); break;
            case "wifi": c.drawArc(2,4,22,24,225,90,false,p); c.drawArc(6,9,18,21,225,90,false,p); p.setStyle(Paint.Style.FILL); c.drawCircle(12,18,1.7f,p); break;
            case "bluetooth": c.drawLine(11,3,11,21,p); c.drawLine(11,3,18,9,p); c.drawLine(18,9,6,18,p); c.drawLine(6,6,18,15,p); c.drawLine(18,15,11,21,p); break;
            case "sun": c.drawCircle(12,12,4,p); for(int i=0;i<8;i++) { double a=i*Math.PI/4; c.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,p); } break;
            case "volume": { Path s=new Path(); s.moveTo(4,9); s.lineTo(8,9); s.lineTo(13,5); s.lineTo(13,19); s.lineTo(8,15); s.lineTo(4,15); s.close(); c.drawPath(s,p); c.drawArc(10,6,22,18,-55,110,false,p); break; }
            case "panel": c.drawRoundRect(4,3,20,21,5,5,p); c.drawLine(8,8,16,8,p); c.drawLine(8,12,16,12,p); c.drawLine(8,16,13,16,p); break;
            default: c.drawCircle(12,12,8,p);
        }
        c.restoreToCount(save);
    }
    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
