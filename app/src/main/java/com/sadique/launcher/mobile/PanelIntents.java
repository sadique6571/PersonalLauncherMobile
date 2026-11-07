package com.sadique.launcher.mobile;

import android.content.Context;
import android.content.Intent;

final class PanelIntents {
    private PanelIntents() { }
    static Intent fromTile(Context context) {
        // Never reuse Home's task or bring it above the app the user was viewing.
        return new Intent(context,NotificationPanelActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    }
}
