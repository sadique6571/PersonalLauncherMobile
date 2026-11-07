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
            Intent panel=new Intent(this,NotificationPanelActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if(Build.VERSION.SDK_INT>=34) startActivityAndCollapse(PendingIntent.getActivity(this,0,panel,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            else startActivityAndCollapse(panel);
        });
    }
}
