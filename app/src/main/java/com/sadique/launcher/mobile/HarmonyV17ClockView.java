package com.sadique.launcher.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * Exact offline Harmony V17 HTML/SVG mechanical watch, with the source physics engine.
 * Only used when selected; it is not a RemoteViews/AppWidget animation.
 */
final class HarmonyV17ClockView extends FrameLayout {
    private static final String ASSET = "file:///android_asset/harmony_mechanical_v17.html";
    private WebView web;
    private boolean closed;

    @SuppressLint("SetJavaScriptEnabled")
    HarmonyV17ClockView(Context context) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        setBackgroundColor(Color.TRANSPARENT);
        setContentDescription("Harmony Mechanical V17 open-heart clock");
        web = new WebView(context);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setFocusable(false);
        web.setFocusableInTouchMode(false);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setBlockNetworkLoads(true);
        s.setDomStorageEnabled(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url) {
                return !ASSET.equals(url);
            }
        });
        addView(web,new FrameLayout.LayoutParams(-1,-1));
        web.loadUrl(ASSET);
    }
    void active(boolean active) {
        if(closed || web==null)return;
        if(active)web.onResume();else web.onPause();
    }
    void close(){
        if(closed)return;
        closed=true;
        if(web!=null){
            web.onPause();
            web.stopLoading();
            web.loadUrl("about:blank");
            removeView(web);
            web.destroy();
            web=null;
        }
    }
}
