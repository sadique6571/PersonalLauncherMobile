package com.sadique.launcher.mobile;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** User-added tile opens the themed panel over the current app without going Home first. */
public final class PanelTileService extends TileService {
    @Override public void onStartListening() { Tile tile=getQsTile(); if(tile!=null) { tile.setState(Tile.STATE_ACTIVE); tile.setLabel(getString(R.string.panel_title)); tile.updateTile(); } }
    @SuppressWarnings("deprecation") @Override public void onClick() {
        unlockAndRun(()-> {
            Intent panel=PanelIntents.fromTile(this);
            if(Build.VERSION.SDK_INT>=34) startActivityAndCollapse(PendingIntent.getActivity(this,0,panel,PendingIntent.FLAG_CANCEL_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            else openBeforeAndroid14(panel);
        });
    }
    // The PendingIntent overload does not exist before API 34; the call site guards this fallback.
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    @SuppressWarnings("deprecation") private void openBeforeAndroid14(Intent panel) { startActivityAndCollapse(panel); }

}
