package com.sadique.launcher.mobile;

import android.app.Notification;
import android.content.ComponentName;
import android.os.*;
import android.service.notification.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/** System-bound, permission-gated and memory-only. This app has no INTERNET permission. */
public final class NotificationCollector extends NotificationListenerService {
    private static volatile NotificationCollector connected;
    private static final Map<String,StatusBarNotification> active=new HashMap<>();
    private static final Set<Runnable> listeners=new CopyOnWriteArraySet<>();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable publish=()-> { for(Runnable listener:listeners) listener.run(); };
    static boolean ready() { return connected!=null; }
    static void observe(Runnable listener) { listeners.add(listener); }
    static void unobserve(Runnable listener) { listeners.remove(listener); }
    static synchronized List<StatusBarNotification> snapshot() {
        List<StatusBarNotification> result=new ArrayList<>(active.values()); result.sort((a,b)->Long.compare(b.getPostTime(),a.getPostTime())); return result;
    }
    private void changed() { main.removeCallbacks(publish); main.postDelayed(publish,120); }
    private static synchronized void put(StatusBarNotification item) {
        if(item==null) return; active.put(item.getKey(),item);
        if(active.size()>200) { StatusBarNotification oldest=Collections.min(active.values(),Comparator.comparingLong(StatusBarNotification::getPostTime)); active.remove(oldest.getKey()); }
    }
    @Override public void onListenerConnected() {
        connected=this;
        synchronized(NotificationCollector.class) { active.clear(); }
        try { for(StatusBarNotification item:getActiveNotifications()) put(item); } catch(RuntimeException denied) { /* Access can be revoked while binding. */ }
        changed();
    }
    @Override public void onNotificationPosted(StatusBarNotification item) { if(connected==this) { put(item); changed(); } }
    @Override public void onNotificationRemoved(StatusBarNotification item) {
        synchronized(NotificationCollector.class) { active.remove(item.getKey()); } changed();
    }
    @Override public void onListenerDisconnected() { clear(); }
    @Override public void onDestroy() { clear(); super.onDestroy(); }
    private void clear() {
        if(connected==this) { connected=null; synchronized(NotificationCollector.class) { active.clear(); } }
        main.removeCallbacks(publish); for(Runnable listener:listeners) listener.run();
    }
    static void dismiss(String key) {
        NotificationCollector service=connected; if(service==null) return;
        try { service.cancelNotification(key); } catch(RuntimeException revoked) { /* System remains authoritative. */ }
    }
    static void clearDismissible() { for(StatusBarNotification item:snapshot()) if(item.isClearable()) dismiss(item.getKey()); }
    static void reconnect(android.content.Context c) { requestRebind(new ComponentName(c,NotificationCollector.class)); }
}
