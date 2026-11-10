package com.sadique.launcher.mobile;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Touch-first Home. No TV helpers, ADB service, accessibility interception or SystemUI changes. */
public final class HomeActivity extends Activity {
    private android.content.SharedPreferences prefs;
    private MobileTheme theme;
    private AppCatalog catalog;
    private List<AppCatalog.Entry> apps=Collections.emptyList();
    private AppCatalog.Adapter homeAdapter,drawerAdapter,dockAdapter;
    private GridView grid,dock;
    private LinearLayout body,dockShell,widgetCard;
    private EditText search;
    private TextView clock,date,heading,weather,libraryButton;
    private ScrollView libraryView;
    private boolean drawer,resumed,library;
    private Bitmap photo;
    private Wallpaper wallpaper;
    private String photoUri="";
    private int photoRequest;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService images=Executors.newSingleThreadExecutor();
    private final Runnable iconRefresh=()-> { if(homeAdapter!=null) { homeAdapter.notifyDataSetChanged(); drawerAdapter.notifyDataSetChanged(); dockAdapter.notifyDataSetChanged(); } };
    private final Runnable tick=new Runnable() { public void run() { updateTime(); if(resumed) main.postDelayed(this,1000*(60-Calendar.getInstance().get(Calendar.SECOND))); } };
    private final BroadcastReceiver packages=new BroadcastReceiver() { public void onReceive(Context c,Intent i) { catalog.invalidate(); scan(); } };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs=getSharedPreferences("mobile_home",MODE_PRIVATE); catalog=new AppCatalog(this);
        catalog.onIcon=()-> { main.removeCallbacks(iconRefresh); main.postDelayed(iconRefresh,40); };
        IntentFilter filter=new IntentFilter(); filter.addAction(Intent.ACTION_PACKAGE_ADDED); filter.addAction(Intent.ACTION_PACKAGE_REMOVED); filter.addAction(Intent.ACTION_PACKAGE_CHANGED); filter.addDataScheme("package");
        if(Build.VERSION.SDK_INT>=33) registerReceiver(packages,filter,Context.RECEIVER_EXPORTED); else registerReceiver(packages,filter);
        if(Build.VERSION.SDK_INT>=33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->showDrawer(false));
        build(); scan();
    }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); showDrawer(false); }
    @Override protected void onResume() { super.onResume(); resumed=true; main.removeCallbacks(tick); tick.run(); if(!photoUri.equals(prefs.getString("wallpaper",""))) loadPhoto(); WidgetKit.refreshAll(this); }
    @Override protected void onPause() { resumed=false; main.removeCallbacks(tick); super.onPause(); }
    @Override protected void onDestroy() { unregisterReceiver(packages); main.removeCallbacksAndMessages(null); catalog.close(); images.shutdownNow(); super.onDestroy(); }
    // Only Android 9–12 use this fallback. Android 13+ uses the registered platform dispatcher above.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation") @Override public void onBackPressed() { showDrawer(false); }

    private void scan() {
        catalog.scan(found-> {
            apps=found;
            if(!prefs.contains("home")) prefs.edit().putString("home",HomeOrder.encode(keys(found,12))).putString("dock",HomeOrder.encode(keys(found,4))).apply();
            refreshApps();
        });
    }
    private static List<String> keys(List<AppCatalog.Entry> source,int count) { List<String> keys=new ArrayList<>(); for(int i=0;i<Math.min(count,source.size());i++) keys.add(source.get(i).key); return keys; }
    private List<AppCatalog.Entry> selected(String key) {
        List<AppCatalog.Entry> out=new ArrayList<>(); Map<String,AppCatalog.Entry> byKey=new HashMap<>(); for(AppCatalog.Entry app:apps) byKey.put(app.key,app);
        for(String item:HomeOrder.decode(prefs.getString(key,""))) if(byKey.containsKey(item)) out.add(byKey.get(item)); return out;
    }
    private void refreshApps() {
        homeAdapter.set(selected("home")); dockAdapter.set(selected("dock"));
        String query=search==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<AppCatalog.Entry> filtered=new ArrayList<>(); for(AppCatalog.Entry app:apps) if(app.label.toLowerCase(Locale.ROOT).contains(query)) filtered.add(app);
        drawerAdapter.set(filtered); if(heading!=null) heading.setText(drawer?"All apps · "+filtered.size():"Favourites");
        if(library && libraryView!=null) { libraryView.removeAllViews(); libraryView.addView(AppLibrary.build(this,theme,catalog,filtered,app->launch(app),app->appMenu(app,null))); }
    }
    private void build() {
        theme=new MobileTheme(prefs); FrameLayout root=new GestureFrame(this,new GestureFrame.Route() {
            public boolean accept(boolean down,float y) { android.graphics.Rect area=new android.graphics.Rect(); boolean inGrid=grid!=null&&grid.getGlobalVisibleRect(area)&&y>=area.top&&y<area.bottom; return down?(!inGrid||!grid.canScrollVertically(-1)):(!drawer&&(!inGrid||!grid.canScrollVertically(1))); }
            public void swipe(boolean down) { if(down) { if(drawer) showDrawer(false); else panel(); } else showDrawer(true); }
        }); wallpaper=new Wallpaper(this); root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        body=Ui.column(this); root.addView(body,new FrameLayout.LayoutParams(-1,-1)); setContentView(root); Ui.insets(this,body,20);
        LinearLayout bar=Ui.row(this);
        TextView brand=Ui.text(this,theme,"PL",19); brand.setTypeface(theme.font,Typeface.BOLD); brand.setTextColor(Color.WHITE); brand.setShadowLayer(4,0,1,Color.BLACK); bar.addView(brand,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1)); brand.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageButton panelButton=Ui.icon(this,theme,"panel","Open control center",Color.WHITE,this::panel); panelButton.setId(R.id.open_panel); bar.addView(panelButton);
        bar.addView(Ui.icon(this,theme,"settings","Launcher settings",Color.WHITE,this::settings)); body.addView(bar);
        widgetCard=Ui.column(this); widgetCard.setPadding(Ui.dp(this,16),Ui.dp(this,10),Ui.dp(this,16),Ui.dp(this,12)); widgetCard.setBackground(theme.panel(getResources().getDisplayMetrics().density,false)); body.addView(widgetCard,new LinearLayout.LayoutParams(-1,-2));
        clock=Ui.text(this,theme,"",82); clock.setSingleLine(true); clock.setHorizontallyScrolling(false); clock.setAutoSizeTextTypeUniformWithConfiguration(44,82,1,android.util.TypedValue.COMPLEX_UNIT_SP); clock.setTypeface(theme.font,theme.id==2||theme.id==6?Typeface.NORMAL:Typeface.BOLD); clock.setTextColor(Color.WHITE); clock.setShadowLayer(Ui.dp(this,6),0,Ui.dp(this,2),0xdd000000); clock.setPadding(0,Ui.dp(this,4),0,0); widgetCard.addView(clock);
        date=Ui.text(this,theme,"",16); date.setTextColor(Color.WHITE); date.setShadowLayer(5,0,2,Color.BLACK); widgetCard.addView(date);
        weather=Ui.text(this,theme,"",14); weather.setTextColor(Color.WHITE); weather.setShadowLayer(5,0,2,Color.BLACK); weather.setVisibility(View.GONE); weather.setOnClickListener(v -> open(new Intent(this,WeatherActivity.class))); widgetCard.addView(weather);
        LinearLayout tools=Ui.row(this); tools.setPadding(0,Ui.dp(this,20),0,Ui.dp(this,8));
        heading=Ui.text(this,theme,"Home",16); heading.setTextColor(Color.WHITE); heading.setShadowLayer(4,0,1,Color.BLACK); tools.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(Ui.icon(this,theme,"search","Search apps",Color.WHITE,()-> { showDrawer(true); search.requestFocus(); ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT); })); tools.addView(Ui.icon(this,theme,"apps","All apps",Color.WHITE,()->showDrawer(!drawer))); body.addView(tools);
        search=new EditText(this); search.setSingleLine(true); search.setTextSize(16); search.setTextColor(theme.ink); search.setHintTextColor(theme.muted()); search.setHint("Search apps"); search.setTypeface(theme.font);
        search.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0); search.setBackground(theme.panel(getResources().getDisplayMetrics().density,true));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT); search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE); search.setVisibility(drawer?View.VISIBLE:View.GONE); body.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,50)));
        search.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s,int start,int count,int after) {} public void onTextChanged(CharSequence s,int start,int before,int count) { refreshApps(); } public void afterTextChanged(Editable e) {} });
        libraryButton=Ui.text(this,theme,"App Library",14); libraryButton.setTextColor(theme.focusInk()); libraryButton.setGravity(Gravity.END); libraryButton.setPadding(0,Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8)); libraryButton.setVisibility(View.GONE); libraryButton.setOnClickListener(v->showLibrary(!library)); body.addView(libraryButton);
        homeAdapter=new AppCatalog.Adapter(this,theme,catalog,true); drawerAdapter=new AppCatalog.Adapter(this,theme,catalog); dockAdapter=new AppCatalog.Adapter(this,theme,catalog,false,false);
        grid=new GridView(this); grid.setNumColumns(Math.max(3,Math.min(6,prefs.getInt(MobileTheme.key(theme.id,"columns"),4)))); grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH); grid.setVerticalSpacing(Ui.dp(this,6)); grid.setVerticalScrollBarEnabled(false); grid.setClipToPadding(false); grid.setBackground(drawer?theme.panel(getResources().getDisplayMetrics().density,true):null); grid.setAdapter(drawer?drawerAdapter:homeAdapter);
        grid.setOnItemClickListener((p,v,pos,id)->launch(((AppCatalog.Adapter)grid.getAdapter()).getItem(pos)));
        grid.setOnItemLongClickListener((p,v,pos,id)-> { appMenu(((AppCatalog.Adapter)grid.getAdapter()).getItem(pos),drawer?null:"home"); return true; });
        body.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        dock=new GridView(this); dock.setNumColumns(4); dock.setAdapter(dockAdapter); dock.setBackgroundColor(Color.TRANSPARENT); dock.setVerticalScrollBarEnabled(false);
        dock.setOnItemClickListener((p,v,pos,id)->launch(dockAdapter.getItem(pos))); dock.setOnItemLongClickListener((p,v,pos,id)-> { appMenu(dockAdapter.getItem(pos),"dock"); return true; });
        dockShell=Ui.row(this); dockShell.setPadding(Ui.dp(this,4),Ui.dp(this,2),Ui.dp(this,10),Ui.dp(this,2)); dockShell.setBackground(theme.dockPanel(getResources().getDisplayMetrics().density));
        dockShell.addView(dock,new LinearLayout.LayoutParams(0,Ui.dp(this,84),1)); dockShell.addView(Ui.icon(this,theme,"apps","All apps",theme.ink,()->showDrawer(true)));
        body.addView(dockShell,new LinearLayout.LayoutParams(-1,Ui.dp(this,88))); dockShell.setVisibility(drawer?View.GONE:View.VISIBLE);
        libraryView=new ScrollView(this); libraryView.setVerticalScrollBarEnabled(false); libraryView.setVisibility(View.GONE); body.addView(libraryView,new LinearLayout.LayoutParams(-1,0,1));
        View.OnTouchListener swipe=new View.OnTouchListener() {
            float x,y;
            public boolean onTouch(View v,android.view.MotionEvent e) {
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN) { x=e.getX(); y=e.getY(); }
                if(e.getActionMasked()==MotionEvent.ACTION_UP && Math.abs(e.getX()-x)<Ui.dp(HomeActivity.this,90)) {
                    float delta=e.getY()-y;
                    if(delta<-Ui.dp(HomeActivity.this,90)) { showDrawer(true); return true; }
                    if(delta>Ui.dp(HomeActivity.this,90)) { if(drawer) showDrawer(false); else panel(); return true; }
                }
                if(e.getActionMasked()==MotionEvent.ACTION_UP) v.performClick();
                return true;
            }
        };
        clock.setOnClickListener(v->showDrawer(true)); date.setOnClickListener(v->panel());
        clock.setOnTouchListener(swipe); date.setOnTouchListener(swipe);
        library=false; updateTime(); showDrawer(drawer); refreshApps(); loadPhoto(); weather();
    }
    private void updateTime() { if(clock!=null) { String value=android.text.format.DateFormat.getTimeFormat(this).format(new Date()); android.text.SpannableString styled=new android.text.SpannableString(value);
        if(!android.text.format.DateFormat.is24HourFormat(this)) for(String part:new java.text.DateFormatSymbols().getAmPmStrings()) { int at=value.lastIndexOf(part); if(at>=0&&!part.isEmpty()) styled.setSpan(new android.text.style.RelativeSizeSpan(.35f),at,at+part.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); }
        clock.setText(styled); date.setText(android.text.format.DateFormat.format("EEEE, d MMMM",new Date())); } }
    private void showDrawer(boolean open) {
        drawer=open; if(search==null) return;
        if(!open) showLibrary(false);
        search.setVisibility(open?View.VISIBLE:View.GONE); libraryButton.setVisibility(open?View.VISIBLE:View.GONE); grid.setBackground(open?theme.panel(getResources().getDisplayMetrics().density,true):null); grid.setAdapter(open?drawerAdapter:homeAdapter); dockShell.setVisibility(open?View.GONE:View.VISIBLE);
        widgetCard.setVisibility(open?View.GONE:View.VISIBLE);
        if(!open) { search.setText(""); search.clearFocus(); ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0); }
        refreshApps();
    }
    private void showLibrary(boolean open) {
        library=open; if(libraryView==null) return;
        libraryView.setVisibility(open?View.VISIBLE:View.GONE); grid.setVisibility(open?View.GONE:View.VISIBLE);
        libraryButton.setText(open?"All apps":"App Library");
        if(open) refreshApps();
    }
    /** Home widget weather: current coarse location once per refresh, Okara fallback. Coordinates are never stored. */
    private void weather() {
        String cached=prefs.getString("weather_text","");
        if(!cached.isEmpty() && weather!=null) { weather.setText(cached); weather.setVisibility(View.VISIBLE); }
        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            if(!prefs.getBoolean("location_asked",false)) { prefs.edit().putBoolean("location_asked",true).apply(); requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},45); }
            else fetchWeather(Weather.OKARA_LATITUDE,Weather.OKARA_LONGITUDE,"Okara · fallback");
            return;
        }
        fetchWeatherWithLocation();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==45) fetchWeatherWithLocation();
    }
    @android.annotation.SuppressLint("MissingPermission")
    private void fetchWeatherWithLocation() {
        double latitude=Weather.OKARA_LATITUDE,longitude=Weather.OKARA_LONGITUDE; String label="Okara · fallback";
        try {
            LocationManager locations=getSystemService(LocationManager.class);
            Location last=null;
            if(locations!=null) { last=locations.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); if(last==null) last=locations.getLastKnownLocation(LocationManager.GPS_PROVIDER); }
            if(last!=null) { latitude=last.getLatitude(); longitude=last.getLongitude(); label="Current location"; }
        } catch(SecurityException denied) { /* No location: Okara fallback below. */ }
        fetchWeather(latitude,longitude,label);
    }
    private void fetchWeather(double latitude,double longitude,String label) {
        String url=Weather.url(latitude,longitude);
        images.execute(()-> {
            String summary=null;
            try {
                java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
                connection.setConnectTimeout(5000); connection.setReadTimeout(5000); connection.setRequestMethod("GET");
                if(connection.getResponseCode()==200) {
                    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); byte[] block=new byte[4096]; int length;
                    try(java.io.InputStream in=connection.getInputStream()) { while((length=in.read(block))!=-1) { if(out.size()+length>65536) break; out.write(block,0,length); } }
                    summary=Weather.summary(out.toString("UTF-8"),label);
                }
                connection.disconnect();
            } catch(Exception offline) { /* Keep the cached reading; no weather line if there is none. */ }
            String shown=summary;
            main.post(()-> {
                if(isDestroyed() || weather==null) return;
                if(shown!=null) { prefs.edit().putString("weather_text",shown).apply(); weather.setText(shown); weather.setVisibility(View.VISIBLE); }
            });
        });
    }
    private void launch(AppCatalog.Entry app) {
        try { ComponentName name=ComponentName.unflattenFromString(app.key); if(name!=null) startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)); }
        catch(RuntimeException gone) { Ui.toast(this,"App unavailable"); scan(); }
    }
    private void appMenu(AppCatalog.Entry app,String from) {
        LinearLayout options=Ui.column(this); final Dialog[] sheet={null};
        java.util.function.BiConsumer<String,Runnable> add=(label,action)->options.addView(Ui.button(this,theme,label,()-> { if(sheet[0]!=null) sheet[0].dismiss(); action.run(); }));
        add.accept("Add to Home",()->pin("home",app)); add.accept("Pin to dock (4 apps)",()->pin("dock",app));
        if(from!=null) {
            add.accept("Move earlier",()->move(from,app,-1)); add.accept("Move later",()->move(from,app,1));
            add.accept("Remove from "+from,()-> { List<String> order=HomeOrder.decode(prefs.getString(from,"")); order.remove(app.key); prefs.edit().putString(from,HomeOrder.encode(order)).apply(); refreshApps(); });
        }
        add.accept("App information",()->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.info.activityInfo.packageName))));
        sheet[0]=Ui.sheet(this,theme,app.label,options);
    }
    private void pin(String where,AppCatalog.Entry app) {
        List<String> order=HomeOrder.decode(prefs.getString(where,""));
        if(!order.contains(app.key)) { if("dock".equals(where)&&order.size()>=4) { Ui.toast(this,"Remove a dock app first"); return; } order.add(app.key); }
        prefs.edit().putString(where,HomeOrder.encode(order)).apply(); refreshApps();
    }
    private void move(String where,AppCatalog.Entry app,int delta) { prefs.edit().putString(where,HomeOrder.encode(HomeOrder.move(HomeOrder.decode(prefs.getString(where,"")),app.key,delta))).apply(); refreshApps(); }
    private void panel() { startActivity(new Intent(this,NotificationPanelActivity.class)); }
    private void open(Intent i) { try { startActivity(i); } catch(RuntimeException unavailable) { Ui.toast(this,"This screen is unavailable on this phone"); } }
    private void settings() {
        LinearLayout options=Ui.column(this); final Dialog[] sheet={null};
        options.addView(Ui.button(this,theme,"Set as default Home",this::defaultHome));
        options.addView(Ui.button(this,theme,EdgeSwipeService.active()?"Disable swipe over apps":"Enable swipe over apps",()-> {
            if(EdgeSwipeService.active()) { prefs.edit().putBoolean("edge_enabled",false).apply(); EdgeSwipeService.stop(); sheet[0].dismiss(); }
            else { prefs.edit().putBoolean("edge_enabled",true).apply(); sheet[0].dismiss(); open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        }));
        TextView edgeInfo=Ui.text(this,theme,"Optional: enable Personal Panel edge swipe in Accessibility. Pull down from the visible handle just below the top-right status bar. Android’s own top-edge shade remains available. Screen text is not read.",13); edgeInfo.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,14)); options.addView(edgeInfo);
        options.addView(Ui.button(this,theme,"Notification access",()->open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        TextView note=Ui.text(this,theme,"Notification access reads notifications for your panel. Content stays in memory on this phone; no server or analytics. Android still owns its permission screens and system shade.",14); note.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10)); options.addView(note);
        options.addView(Ui.button(this,theme,"Weather location",()->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))));
        TextView weatherInfo=Ui.text(this,theme,"Weather uses your current coarse location, once per refresh. Deny it in Android settings and the widget falls back to Okara. Coordinates are never stored.",13); weatherInfo.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,14)); options.addView(weatherInfo);
        options.addView(Ui.button(this,theme,"Import saved settings (.xml)",()-> { sheet[0].dismiss(); startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),44); }));
        options.addView(Ui.button(this,theme,"Choose photo wallpaper",()-> { sheet[0].dismiss(); startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),42); }));
        options.addView(Ui.button(this,theme,"Use theme wallpaper",()-> { prefs.edit().remove("wallpaper").apply(); sheet[0].dismiss(); build(); }));
        TextView label=Ui.text(this,theme,"Visual style",18); label.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,8)); options.addView(label);
        for(int i=0;i<MobileTheme.NAMES.length;i++) { int style=i; options.addView(Ui.button(this,theme,(i==theme.id?"✓  ":"")+MobileTheme.NAMES[i],()-> { prefs.edit().putInt("style",style).apply(); sheet[0].dismiss(); build(); })); }
        SeekBar strength=new SeekBar(this); strength.setMax(100); strength.setProgress(theme.intensity); strength.setProgressTintList(ColorStateList.valueOf(theme.focusInk()));
        options.addView(Ui.text(this,theme,"Panel intensity",16)); options.addView(strength);
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(SeekBar b,int v,boolean user) {} public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) { prefs.edit().putInt(MobileTheme.key(theme.id,"intensity"),b.getProgress()).apply(); sheet[0].dismiss(); build(); } });
        Switch solid=new Switch(this); solid.setText("Panel transparency"); solid.setTextColor(theme.ink); solid.setChecked(prefs.getBoolean(MobileTheme.key(theme.id,"transparent"),true)); options.addView(solid);
        solid.setOnCheckedChangeListener((v,on)-> { prefs.edit().putBoolean(MobileTheme.key(theme.id,"transparent"),on).apply(); sheet[0].dismiss(); build(); });
        options.addView(Ui.button(this,theme,"App columns: "+grid.getNumColumns(),()-> { prefs.edit().putInt(MobileTheme.key(theme.id,"columns"),grid.getNumColumns()>=6?3:grid.getNumColumns()+1).apply(); sheet[0].dismiss(); build(); }));
        options.addView(Ui.button(this,theme,"Icon corners",()-> { String key=MobileTheme.key(theme.id,"round"); prefs.edit().putInt(key,theme.radius==12?24:theme.radius==24?29:12).apply(); sheet[0].dismiss(); build(); }));
        options.addView(Ui.button(this,theme,"Change font",()-> { String key=MobileTheme.key(theme.id,"font"); prefs.edit().putInt(key,(prefs.getInt(key,theme.id==6?1:0)+1)%3).apply(); sheet[0].dismiss(); build(); }));
        for(int color:new int[]{0xff8fd0b2,0xff40d9ff,0xfff5b942,0xffb388ff,0xffff9a5c,0xff111111,0xffffffff}) {
            TextView choice=Ui.button(this,theme,"Accent  "+String.format(Locale.ROOT,"#%06X",color&0xffffff),()-> { prefs.edit().putInt(MobileTheme.key(theme.id,"accent"),color).apply(); sheet[0].dismiss(); build(); }); choice.setTextColor(MobileTheme.contrast(color,theme.top)>=3?color:theme.ink); options.addView(choice);
        }
        options.addView(Ui.button(this,theme,"Reset this theme",()-> { android.content.SharedPreferences.Editor e=prefs.edit(); for(String key:prefs.getAll().keySet()) if(key.startsWith("appearance."+theme.id+".")) e.remove(key); e.apply(); sheet[0].dismiss(); build(); }));
        sheet[0]=Ui.sheet(this,theme,"Launcher settings",options);
    }
    private void defaultHome() {
        if(Build.VERSION.SDK_INT>=29) { RoleManager roles=getSystemService(RoleManager.class); if(roles!=null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) { if(roles.isRoleHeld(RoleManager.ROLE_HOME)) Ui.toast(this,"Already your default Home"); else startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME),43); return; } }
        open(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==44 && result==RESULT_OK && data!=null && data.getData()!=null) {
            Uri uri=data.getData(); Ui.toast(this,"Reading settings backup"); images.execute(()-> {
                try(java.io.InputStream in=getContentResolver().openInputStream(uri)) {
                    Map<String,Object> restored=SettingsImport.read(in);
                    main.post(()-> { if(isDestroyed()) return; android.content.SharedPreferences.Editor edit=prefs.edit();
                        for(Map.Entry<String,Object> entry:restored.entrySet()) { Object value=entry.getValue(); if(value instanceof String) edit.putString(entry.getKey(),(String)value); else if(value instanceof Integer) edit.putInt(entry.getKey(),(Integer)value); else if(value instanceof Boolean) edit.putBoolean(entry.getKey(),(Boolean)value); }
                        edit.apply(); build(); Ui.toast(this,"Settings restored. Android permissions must be approved separately."); });
                } catch(Exception invalid) { main.post(()-> { if(!isDestroyed()) Ui.toast(this,"Could not read this settings backup"); }); }
            });
        }
        if(request==42 && result==RESULT_OK && data!=null && data.getData()!=null) {
            Uri uri=data.getData(); try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); prefs.edit().putString("wallpaper",uri.toString()).apply(); loadPhoto(); } catch(SecurityException denied) { Ui.toast(this,"Photo access unavailable"); }
        }
    }
    private void loadPhoto() {
        String uri=prefs.getString("wallpaper",""); if(uri.equals(photoUri)) return; photoUri=uri; int request=++photoRequest;
        if(uri.isEmpty()) { photo=null; wallpaper.invalidate(); return; }
        images.execute(()-> {
            Bitmap bitmap=null;
            try {
                Uri source=Uri.parse(uri); BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
                try(java.io.InputStream in=getContentResolver().openInputStream(source)) { BitmapFactory.decodeStream(in,null,options); }
                options.inSampleSize=1; while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>2048) options.inSampleSize*=2;
                options.inJustDecodeBounds=false;
                try(java.io.InputStream in=getContentResolver().openInputStream(source)) { bitmap=BitmapFactory.decodeStream(in,null,options); }
            } catch(Exception unavailable) { /* Keep the theme fallback; no photo path is logged. */ }
            Bitmap ready=bitmap; main.post(()-> { if(!isDestroyed() && request==photoRequest) { photo=ready; wallpaper.invalidate(); if(ready==null) Ui.toast(this,"Photo unavailable; using theme wallpaper"); } });
        });
    }
    private final class Wallpaper extends View {
        final Paint ink=new Paint(3); final RectF destination=new RectF();
        Shader base,glow,shade;
        Wallpaper(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
            base=new LinearGradient(0,0,Math.max(1,w),Math.max(1,h),theme.top,theme.bottom,Shader.TileMode.CLAMP);
            glow=new RadialGradient(w*.8f,h*.35f,Math.max(1,w),theme.accent&0x55ffffff,Color.TRANSPARENT,Shader.TileMode.CLAMP);
            shade=new LinearGradient(0,0,0,Math.max(1,h*.55f),0x99000000,Color.TRANSPARENT,Shader.TileMode.CLAMP);
        }
        @Override protected void onDraw(Canvas c) {
            int w=getWidth(),h=getHeight();
            if(photo!=null) {
                float scale=Math.max(w/(float)photo.getWidth(),h/(float)photo.getHeight()); float x=(w-photo.getWidth()*scale)/2,y=(h-photo.getHeight()*scale)/2;
                destination.set(x,y,x+photo.getWidth()*scale,y+photo.getHeight()*scale); c.drawBitmap(photo,null,destination,ink);
            } else {
                ink.setShader(base); c.drawRect(0,0,w,h,ink);
                ink.setShader(glow); c.drawRect(0,0,w,h,ink);
            }
            ink.setShader(shade); c.drawRect(0,0,w,h,ink); ink.setShader(null);
        }
    }
}
