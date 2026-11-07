package com.sadique.launcher.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.*;
import android.widget.*;

final class Ui {
    private Ui() { }
    static int dp(Context c,float v) { return Math.round(v*c.getResources().getDisplayMetrics().density); }
    static TextView text(Context c,MobileTheme t,String value,int size) {
        TextView v=new TextView(c); v.setText(value); v.setTextSize(size); v.setTextColor(t.ink); v.setTypeface(t.font); return v;
    }
    static LinearLayout column(Context c) { LinearLayout v=new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    static LinearLayout row(Context c) { LinearLayout v=new LinearLayout(c); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    static TextView button(Context c,MobileTheme t,String name,Runnable action) {
        TextView b=text(c,t,name,14); b.setGravity(Gravity.CENTER); b.setMinHeight(dp(c,48)); b.setPadding(dp(c,14),dp(c,8),dp(c,14),dp(c,8));
        b.setBackground(t.panel(c.getResources().getDisplayMetrics().density,false)); b.setFocusable(true); b.setClickable(true);
        b.setContentDescription(name); b.setOnClickListener(v->action.run()); return b;
    }
    static Dialog sheet(Activity a,MobileTheme t,String title,LinearLayout body) {
        Dialog d=new Dialog(a); LinearLayout outer=column(a); int p=dp(a,20); outer.setPadding(p,p,p,p);
        outer.setBackground(t.panel(a.getResources().getDisplayMetrics().density,true));
        TextView heading=text(a,t,title,23); heading.setPadding(0,0,0,p); outer.addView(heading);
        ScrollView scroll=new ScrollView(a); scroll.setFillViewport(false); scroll.addView(body);
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        outer.addView(button(a,t,"Close",d::dismiss)); d.setContentView(outer);
        Window w=d.getWindow(); if(w!=null) { w.setBackgroundDrawableResource(android.R.color.transparent); w.setGravity(Gravity.BOTTOM); }
        d.show(); if(w!=null) w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.8f)); return d;
    }
    static void insets(Activity a,View content,int horizontal) {
        Window w=a.getWindow(); w.setStatusBarColor(Color.TRANSPARENT); w.setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=30) w.setDecorFitsSystemWindows(false);
        content.setOnApplyWindowInsetsListener((v,in)-> {
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                left=bars.left; top=bars.top; right=bars.right; bottom=bars.bottom;
            } else { left=in.getSystemWindowInsetLeft(); top=in.getSystemWindowInsetTop(); right=in.getSystemWindowInsetRight(); bottom=in.getSystemWindowInsetBottom(); }
            v.setPadding(left+dp(a,horizontal),top+dp(a,8),right+dp(a,horizontal),bottom+dp(a,8)); return in;
        });
        content.requestApplyInsets();
    }
    static void toast(Context c,String text) { Toast.makeText(c,text,Toast.LENGTH_SHORT).show(); }
}
