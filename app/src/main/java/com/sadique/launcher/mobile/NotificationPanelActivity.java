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
    private LevelSlider brightness,volume;
    private List<StatusBarNotification> displayed=Collections.emptyList();
    private final Runnable changed=()->runOnUiThread(this::render);
    private final BroadcastReceiver screen=new BroadcastReceiver() { public void onReceive(Context c,Intent i) { if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())) finish(); else render(); } };
    private boolean unlocked() { KeyguardManager lock=getSystemService(KeyguardManager.class); return lock==null || !lock.isDeviceLocked(); }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); theme=new MobileTheme(getSharedPreferences("mobile_home",MODE_PRIVATE));
        LinearLayout root=Ui.column(this);
        boolean blur=Build.VERSION.SDK_INT>=31 && getSystemService(WindowManager.class).isCrossWindowBlurEnabled();
        root.setBackground(theme.panel(getResources().getDisplayMetrics().density,!blur)); setContentView(root); if(Build.VERSION.SDK_INT>=31 && blur) getWindow().setBackgroundBlurRadius(Ui.dp(this,36)); Ui.insets(this,root,20);
        LinearLayout header=Ui.row(this); LinearLayout titles=Ui.column(this);
        TextView time=Ui.text(this,theme,android.text.format.DateFormat.getTimeFormat(this).format(new Date()),42); time.setTypeface(theme.font,android.graphics.Typeface.BOLD); titles.addView(time);
        TextView day=Ui.text(this,theme,android.text.format.DateFormat.format("EEEE, d MMMM",new Date()).toString(),13); day.setTextColor(theme.muted()); titles.addView(day);
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        android.widget.ImageButton close=Ui.icon(this,theme,"close","Close control center",theme.ink,this::closePanel); close.setId(R.id.close_panel); header.addView(close); root.addView(header);
        LinearLayout quick=Ui.row(this); quick.setPadding(0,Ui.dp(this,20),0,Ui.dp(this,12));
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,Ui.dp(this,82),1); half.setMarginEnd(Ui.dp(this,6));
        quick.addView(Ui.control(this,theme,"wifi","Wi-Fi","Connection settings",()->system(Settings.ACTION_WIFI_SETTINGS)),half);
        LinearLayout.LayoutParams other=new LinearLayout.LayoutParams(0,Ui.dp(this,82),1); other.setMarginStart(Ui.dp(this,6));
        quick.addView(Ui.control(this,theme,"bluetooth","Bluetooth","Device settings",()->system(Settings.ACTION_BLUETOOTH_SETTINGS)),other); root.addView(quick);
        LinearLayout controls=Ui.row(this); controls.setPadding(0,0,0,Ui.dp(this,16));
        LinearLayout shortcuts=Ui.column(this); shortcuts.addView(Ui.control(this,theme,"sun","Display","Screen settings",()->system(Settings.ACTION_DISPLAY_SETTINGS)),new LinearLayout.LayoutParams(-1,Ui.dp(this,78)));
        View access=Ui.control(this,theme,"panel","Swipe access","Enable over apps",()-> {
            getSharedPreferences("mobile_home",MODE_PRIVATE).edit().putBoolean("edge_enabled",true).apply(); system(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        }); LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-1,Ui.dp(this,78)); gap.topMargin=Ui.dp(this,12); shortcuts.addView(access,gap);
        controls.addView(shortcuts,new LinearLayout.LayoutParams(0,Ui.dp(this,168),1.8f));
        LinearLayout sliders=Ui.row(this); sliders.setPadding(Ui.dp(this,12),0,0,0);
        int level=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128)*100/255;
        brightness=new LevelSlider(this,theme,"Brightness","sun",level,value-> {
            if(!Settings.System.canWrite(this)) return;
            try { Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL); Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.round(value*2.55f))); }
            catch(SecurityException denied) { Ui.toast(this,"Allow brightness control in Android settings"); }
        });
        sliders.addView(brightness,new LinearLayout.LayoutParams(0,Ui.dp(this,168),1));
        AudioManager audio=getSystemService(AudioManager.class); int maximum=Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume=new LevelSlider(this,theme,"Media volume","volume",audio.getStreamVolume(AudioManager.STREAM_MUSIC)*100/maximum,value-> {
            try { audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.round(value*maximum/100f),0); } catch(SecurityException denied) { Ui.toast(this,"Volume unavailable"); }
        });
        LinearLayout.LayoutParams secondSlider=new LinearLayout.LayoutParams(0,Ui.dp(this,168),1); secondSlider.setMarginStart(Ui.dp(this,10)); sliders.addView(volume,secondSlider);
        controls.addView(sliders,new LinearLayout.LayoutParams(0,Ui.dp(this,168),2)); root.addView(controls);
        TextView brightnessAccess=Ui.text(this,theme,"Allow brightness control",12); brightnessAccess.setTextColor(theme.focusInk()); brightnessAccess.setGravity(Gravity.END); brightnessAccess.setMinHeight(Ui.dp(this,40));
        brightnessAccess.setOnClickListener(v-> { try { startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))); } catch(RuntimeException unavailable) { Ui.toast(this,"Brightness settings unavailable"); } });
        brightnessAccess.setVisibility(Settings.System.canWrite(this)?View.GONE:View.VISIBLE); root.addView(brightnessAccess);
        LinearLayout tools=Ui.row(this); state=Ui.text(this,theme,"Notifications",17); state.setTypeface(theme.font,android.graphics.Typeface.BOLD); tools.addView(state,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        tools.addView(Ui.icon(this,theme,"close","Clear dismissible notifications",theme.muted(),NotificationCollector::clearDismissible)); root.addView(tools);
        notifications=Ui.column(this); root.addView(notifications);
        ListView list=new ListView(this); list.setVerticalScrollBarEnabled(false); list.setDivider(null); list.setDividerHeight(Ui.dp(this,12));
        adapter=new BaseAdapter() {
            public int getCount() { return displayed.size(); }
            public Object getItem(int position) { return displayed.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position,View recycled,ViewGroup parent) {
                try { return card(displayed.get(position),recycled); }
                catch(RuntimeException malformed) { return Ui.text(NotificationPanelActivity.this,theme,"Notification unavailable",15); }
            }
        };
        // Controls scroll with notifications on short/landscape windows; Close stays pinned above.
        LinearLayout topContent=Ui.column(this);
        while(root.getChildCount()>1) { View child=root.getChildAt(1); root.removeViewAt(1); topContent.addView(child); }
        list.addHeaderView(topContent,null,false); list.setAdapter(adapter); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        IntentFilter filter=new IntentFilter(); filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_USER_PRESENT);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(screen,filter,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(screen,filter);
        if(Build.VERSION.SDK_INT>=30 && getWindow().getInsetsController()!=null) getWindow().getInsetsController().setSystemBarsAppearance(theme.light()?android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS:0,android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    }
    @Override protected void onStart() { super.onStart(); EdgeSwipeService.panel(true); NotificationCollector.observe(changed); }
    @Override protected void onResume() { super.onResume(); if(brightness!=null) { brightness.setEnabled(Settings.System.canWrite(this)); brightness.setLevel(Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128)*100/255); } render(); if(access() && !NotificationCollector.ready()) NotificationCollector.reconnect(this); }
    @Override protected void onStop() { NotificationCollector.unobserve(changed); EdgeSwipeService.panel(false); super.onStop(); }
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
        if(item.isClearable()) top.addView(Ui.icon(this,theme,"close","Dismiss notification",theme.muted(),()->NotificationCollector.dismiss(item.getKey()))); card.addView(top);
        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE),body=n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if(body==null) body=n.extras.getCharSequence(Notification.EXTRA_TEXT);
        TextView heading=Ui.text(this,theme,NotificationText.clean(title,160),17); heading.setTypeface(theme.font,android.graphics.Typeface.BOLD); card.addView(heading);
        TextView message=Ui.text(this,theme,NotificationText.clean(body,600),15); message.setMaxLines(5); message.setEllipsize(android.text.TextUtils.TruncateAt.END); message.setTextIsSelectable(false); card.addView(message);
        if(n.contentIntent!=null) { card.setClickable(true); card.setOnClickListener(v->send(n.contentIntent,null)); }
        LinearLayout actions=Ui.row(this);
        if(n.actions!=null) for(Notification.Action action:n.actions) {
            if(action.actionIntent==null) continue;
            actions.addView(Ui.button(this,theme,NotificationText.clean(action.title,50),()-> {
                RemoteInput[] inputs=action.getRemoteInputs(); boolean reply=false; if(inputs!=null) for(RemoteInput input:inputs) reply|=input.getAllowFreeFormInput();
                if(reply) reply(action); else send(action.actionIntent,null);
            }));
        }
        if(actions.getChildCount()>0) { for(int i=0;i<actions.getChildCount();i++) { TextView action=(TextView)actions.getChildAt(i); action.setMaxLines(1); action.setEllipsize(android.text.TextUtils.TruncateAt.END); action.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1)); } card.addView(actions); }
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
