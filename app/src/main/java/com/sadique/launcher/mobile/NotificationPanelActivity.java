package com.sadique.launcher.mobile;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.view.*;
import android.widget.*;
import java.util.*;

/** An app-owned panel. It deliberately never collapses or disables Android's real shade. */
public final class NotificationPanelActivity extends Activity {
    private MobileTheme theme;
    private LinearLayout notifications;
    private TextView state;
    private BaseAdapter adapter;
    private List<StatusBarNotification> displayed=Collections.emptyList();
    private final Runnable changed=()->runOnUiThread(this::render);
    private final BroadcastReceiver screen=new BroadcastReceiver() { public void onReceive(Context c,Intent i) { if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())) finish(); else render(); } };
    private boolean unlocked() { KeyguardManager lock=getSystemService(KeyguardManager.class); return lock==null || !lock.isDeviceLocked(); }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); theme=new MobileTheme(getSharedPreferences("mobile_home",MODE_PRIVATE));
        LinearLayout root=Ui.column(this); root.setBackground(theme.panel(getResources().getDisplayMetrics().density,true)); setContentView(root); Ui.insets(this,root,20);
        LinearLayout header=Ui.row(this); header.addView(Ui.text(this,theme,"Personal Panel",25),new LinearLayout.LayoutParams(0,-2,1)); android.widget.TextView close=Ui.button(this,theme,"Close",this::closePanel); close.setId(R.id.close_panel); header.addView(close); root.addView(header);
        TextView caption=Ui.text(this,theme,MobileTheme.NAMES[theme.id]+" · "+android.text.format.DateFormat.format("EEE, d MMM",new Date()),14); caption.setPadding(0,Ui.dp(this,8),0,Ui.dp(this,12)); root.addView(caption);
        LinearLayout quick=Ui.row(this);
        quick.addView(Ui.button(this,theme,"Wi-Fi",()->system(Settings.ACTION_WIFI_SETTINGS)),new LinearLayout.LayoutParams(0,-2,1));
        quick.addView(Ui.button(this,theme,"Bluetooth",()->system(Settings.ACTION_BLUETOOTH_SETTINGS)),new LinearLayout.LayoutParams(0,-2,1));
        quick.addView(Ui.button(this,theme,"Display",()->system(Settings.ACTION_DISPLAY_SETTINGS)),new LinearLayout.LayoutParams(0,-2,1)); root.addView(quick);
        TextView hint=Ui.text(this,theme,"Connection controls open Android settings. Your theme styles this panel, not protected system screens.",12); hint.setTextColor(theme.muted()); hint.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10)); root.addView(hint);
        AudioManager audio=getSystemService(AudioManager.class); root.addView(Ui.text(this,theme,"Media volume",15));
        SeekBar volume=new SeekBar(this); volume.setMax(Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))); volume.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC)); volume.setProgressTintList(ColorStateList.valueOf(theme.focusInk())); root.addView(volume);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar,int value,boolean user) { if(user) try { audio.setStreamVolume(AudioManager.STREAM_MUSIC,value,0); } catch(SecurityException denied) { Ui.toast(NotificationPanelActivity.this,"Volume unavailable"); } }
            public void onStartTrackingTouch(SeekBar bar) {} public void onStopTrackingTouch(SeekBar bar) {}
        });
        LinearLayout tools=Ui.row(this); state=Ui.text(this,theme,"Notifications",16); tools.addView(state,new LinearLayout.LayoutParams(0,-2,1)); tools.addView(Ui.button(this,theme,"Clear dismissible",NotificationCollector::clearDismissible)); root.addView(tools);
        notifications=Ui.column(this); root.addView(notifications);
        ListView list=new ListView(this); list.setDivider(null); list.setDividerHeight(Ui.dp(this,12));
        adapter=new BaseAdapter() {
            public int getCount() { return displayed.size(); }
            public Object getItem(int position) { return displayed.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position,View recycled,ViewGroup parent) {
                try { return card(displayed.get(position),recycled); }
                catch(RuntimeException malformed) { return Ui.text(NotificationPanelActivity.this,theme,"Notification unavailable",15); }
            }
        };
        list.setAdapter(adapter); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        IntentFilter filter=new IntentFilter(); filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_USER_PRESENT);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(screen,filter,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(screen,filter);
        if(Build.VERSION.SDK_INT>=30 && getWindow().getInsetsController()!=null) getWindow().getInsetsController().setSystemBarsAppearance(theme.light()?android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS:0,android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    }
    @Override protected void onStart() { super.onStart(); NotificationCollector.observe(changed); }
    @Override protected void onResume() { super.onResume(); render(); if(access() && !NotificationCollector.ready()) NotificationCollector.reconnect(this); }
    @Override protected void onStop() { NotificationCollector.unobserve(changed); super.onStop(); }
    @Override protected void onDestroy() { unregisterReceiver(screen); super.onDestroy(); }
    private void closePanel() { if(isTaskRoot()) finishAndRemoveTask(); else finish(); }
    private boolean access() { return getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(new ComponentName(this,NotificationCollector.class)); }
    private void system(String action) { try { startActivity(new Intent(action)); } catch(RuntimeException unavailable) { Ui.toast(this,"Setting unavailable"); } }
    private void render() {
        if(isDestroyed() || notifications==null) return; notifications.removeAllViews(); displayed=Collections.emptyList(); adapter.notifyDataSetChanged();
        if(!unlocked()) { state.setText("Unlock to view notifications"); return; }
        if(!access()) {
            state.setText("Notification access is off");
            TextView explanation=Ui.text(this,theme,"Allow notification access to display and dismiss notifications here. Content stays in memory on your phone. Android may hide sensitive notifications.",16); notifications.addView(explanation);
            notifications.addView(Ui.button(this,theme,"Enable notification access",()->system(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))); return;
        }
        if(!NotificationCollector.ready()) { state.setText("Waiting for Android notification service"); return; }
        List<StatusBarNotification> items=NotificationCollector.snapshot(); state.setText(items.isEmpty()?"No notifications":"Notifications · "+items.size());
        displayed=items; adapter.notifyDataSetChanged();
    }
    private View card(StatusBarNotification item,View recycled) {
        Notification n=item.getNotification(); LinearLayout card=recycled instanceof LinearLayout?(LinearLayout)recycled:Ui.column(this); card.removeAllViews(); card.setPadding(Ui.dp(this,16),Ui.dp(this,14),Ui.dp(this,16),Ui.dp(this,14)); card.setBackground(theme.panel(getResources().getDisplayMetrics().density,false));
        String app=item.getPackageName(); try { app=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(app,0)).toString(); } catch(Exception gone) { /* Package can be removed. */ }
        LinearLayout top=Ui.row(this); ImageView icon=new ImageView(this);
        try { if(n.getSmallIcon()!=null) icon.setImageDrawable(n.getSmallIcon().loadDrawable(this)); } catch(RuntimeException gone) { /* Icon may no longer resolve. */ }
        icon.setColorFilter(theme.ink); top.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,22),Ui.dp(this,22)));
        TextView appName=Ui.text(this,theme,"  "+app,13); top.addView(appName,new LinearLayout.LayoutParams(0,-2,1));
        if(item.isClearable()) top.addView(Ui.button(this,theme,"Dismiss",()->NotificationCollector.dismiss(item.getKey()))); card.addView(top);
        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE),body=n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if(body==null) body=n.extras.getCharSequence(Notification.EXTRA_TEXT);
        TextView heading=Ui.text(this,theme,NotificationText.clean(title,160),17); heading.setTypeface(theme.font,android.graphics.Typeface.BOLD); card.addView(heading);
        TextView message=Ui.text(this,theme,NotificationText.clean(body,600),15); message.setTextIsSelectable(false); card.addView(message);
        if(n.contentIntent!=null) card.addView(Ui.button(this,theme,"Open",()->send(n.contentIntent,null)));
        if(n.actions!=null) for(Notification.Action action:n.actions) {
            if(action.actionIntent==null) continue;
            card.addView(Ui.button(this,theme,NotificationText.clean(action.title,50),()-> {
                RemoteInput[] inputs=action.getRemoteInputs(); boolean reply=false; if(inputs!=null) for(RemoteInput input:inputs) reply|=input.getAllowFreeFormInput();
                if(reply) reply(action); else send(action.actionIntent,null);
            }));
        }
        return card;
    }
    private void reply(Notification.Action action) {
        LinearLayout body=Ui.column(this); EditText input=new EditText(this); input.setTextColor(theme.ink); input.setHintTextColor(theme.muted()); input.setHint("Reply"); input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE); body.addView(input);
        final Dialog[] sheet={null}; body.addView(Ui.button(this,theme,"Send reply",()-> {
            if(!unlocked()) { sheet[0].dismiss(); return; }
            String value=input.getText().toString().trim(); if(value.isEmpty()) return;
            Bundle results=new Bundle(); for(RemoteInput remote:action.getRemoteInputs()) if(remote.getAllowFreeFormInput()) results.putCharSequence(remote.getResultKey(),value);
            Intent intent=new Intent(); RemoteInput.addResultsToIntent(action.getRemoteInputs(),intent,results); send(action.actionIntent,intent); sheet[0].dismiss();
        })); sheet[0]=Ui.sheet(this,theme,"Reply",body);
    }
    private void send(PendingIntent action,Intent data) {
        if(!unlocked()) return;
        try { action.send(this,0,data); } catch(PendingIntent.CanceledException|SecurityException unavailable) { Ui.toast(this,"Notification action no longer available"); }
    }
}
