package com.sadique.launcher.mobile;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

/** Shared theme identity, with a private preference namespace for each appearance. */
final class MobileTheme {
    static final String[] NAMES={"TV Glass","Classic","Dune Loom","Pearl Current","Cobalt Rail","Sage Fold","Pine Mist","Amber Night"};
    private static final int[] TOP={0xff253049,0xff18334b,0xff26363a,0xfff9f1e8,0xff12275c,0xffdce6be,0xff17382c,0xff2a1a0c};
    private static final int[] BOTTOM={0xff101621,0xff09182a,0xff111e24,0xffe9e4dc,0xff07112d,0xffc5d4ac,0xff08150f,0xff120a04};
    private static final int[] INK={0xfff5f7ff,0xfff1f5fb,0xffffedd8,0xff242d30,0xfff1f5fb,0xff0d2b1a,0xffeafff2,0xfffff3dd};
    private static final int[] ACCENT={0xffb9c8ff,0xff40d9ff,0xffedab75,0xff006d72,0xffffd92f,0xff195c36,0xff8fd0b2,0xfff5b942};
    final int id,top,bottom,ink,accent,intensity,radius;
    final Typeface font;
    final SharedPreferences prefs;
    MobileTheme(SharedPreferences prefs) {
        this.prefs=prefs; id=clamp(prefs.getInt("style",0)); top=TOP[id]; bottom=BOTTOM[id]; ink=INK[id];
        accent=prefs.getInt(key(id,"accent"),ACCENT[id]);
        intensity=Math.max(0,Math.min(100,prefs.getInt(key(id,"intensity"),75)));
        radius=prefs.getInt(key(id,"round"),id==2||id==7?12:24);
        int face=Math.max(0,Math.min(2,prefs.getInt(key(id,"font"),id==6?1:0)));
        font=Typeface.create(new String[]{"sans-serif","serif","monospace"}[face],Typeface.NORMAL);
    }
    static int clamp(int id) { return Math.max(0,Math.min(NAMES.length-1,id)); }
    static String key(int id,String name) { return "appearance."+clamp(id)+"."+name; }
    boolean light() { return id==3||id==5; }
    int muted() { return mix(ink,top,.22f); }
    int focusInk() { return contrast(accent,top)>=3?accent:ink; }
    int accentInk() { return contrast(0xff11191c,accent)>=contrast(0xffffffff,accent)?0xff11191c:0xffffffff; }
    GradientDrawable panel(float density,boolean solid) {
        int alpha=solid||!prefs.getBoolean(key(id,"transparent"),true)?255:Math.round((light()?218:170)+intensity*(light()?.32f:.65f));
        GradientDrawable d=new GlassPanel(density); d.setOrientation(GradientDrawable.Orientation.TL_BR); d.setColors(new int[]{(alpha<<24)|(top&0xffffff),(alpha<<24)|(bottom&0xffffff)});
        d.setCornerRadius(radius*density); d.setStroke(Math.max(1,Math.round(density)),mix(top,ink,.25f)); return d;
    }
    static int mix(int a,int b,float t) {
        return 0xff000000 | Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16
                | Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8 | Math.round((a&255)*(1-t)+(b&255)*t);
    }
    static double luminance(int c) { return .2126*linear((c>>16)&255)+.7152*linear((c>>8)&255)+.0722*linear(c&255); }
    private static double linear(int c) { double x=c/255d; return x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4); }
    static double contrast(int a,int b) { double x=luminance(a),y=luminance(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05); }
}
