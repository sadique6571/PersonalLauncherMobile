package com.sadique.launcher.mobile;

/** Test-only stand-in for the Gallery task; never included in the release variant. */
public final class RoutingUnderlayActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle saved) {
        super.onCreate(saved); android.widget.TextView text=new android.widget.TextView(this);
        text.setText("Routing test: underlying app"); setContentView(text);
    }
}
