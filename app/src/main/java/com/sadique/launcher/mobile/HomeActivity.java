package com.sadique.launcher.mobile;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
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
    private LinearLayout body;
    private EditText search;
    private TextView clock,date,heading;
    private boolean drawer,resumed;
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
    @Override protected void onResume() { super.onResume(); resumed=true; main.removeCallbacks(tick); tick.run(); if(!photoUri.equals(prefs.getString("wallpaper",""))) loadPhoto(); }
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
        drawerAdapter.set(filtered); if(heading!=null) heading.setText(drawer?"All apps · "+filtered.size():"Home");
    }
    private void build() {
        theme=new MobileTheme(prefs); FrameLayout root=new FrameLayout(this); wallpaper=new Wallpaper(this); root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        body=Ui.column(this); root.addView(body,new FrameLayout.LayoutParams(-1,-1)); setContentView(root); Ui.insets(this,body,20);
        LinearLayout bar=Ui.row(this);
        TextView brand=Ui.text(this,theme,"PL  /  "+MobileTheme.NAMES[theme.id],14); brand.setTextColor(Color.WHITE); brand.setShadowLayer(6,0,2,Color.BLACK); bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(Ui.button(this,theme,"Panel",this::panel)); bar.addView(Ui.button(this,theme,"Settings",this::settings)); body.addView(bar);
        clock=Ui.text(this,theme,"",72); clock.setTypeface(theme.font,theme.id==2||theme.id==6?Typeface.NORMAL:Typeface.BOLD); clock.setTextColor(Color.WHITE); clock.setShadowLayer(Ui.dp(this,6),0,Ui.dp(this,2),0xdd000000); clock.setPadding(0,Ui.dp(this,26),0,0); body.addView(clock);
        date=Ui.text(this,theme,"",16); date.setTextColor(Color.WHITE); date.setShadowLayer(5,0,2,Color.BLACK); body.addView(date);
        LinearLayout tools=Ui.row(this); tools.setPadding(0,Ui.dp(this,20),0,Ui.dp(this,8));
        heading=Ui.text(this,theme,"Home",16); heading.setTextColor(Color.WHITE); heading.setShadowLayer(4,0,1,Color.BLACK); tools.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(Ui.button(this,theme,"All apps",()->showDrawer(!drawer))); body.addView(tools);
        search=new EditText(this); search.setSingleLine(true); search.setTextSize(16); search.setTextColor(theme.ink); search.setHintTextColor(theme.muted()); search.setHint("Search apps"); search.setTypeface(theme.font);
        search.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0); search.setBackground(theme.panel(getResources().getDisplayMetrics().density,true));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT); search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE); search.setVisibility(drawer?View.VISIBLE:View.GONE); body.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,50)));
        search.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s,int start,int count,int after) {} public void onTextChanged(CharSequence s,int start,int before,int count) { refreshApps(); } public void afterTextChanged(Editable e) {} });
        homeAdapter=new AppCatalog.Adapter(this,theme,catalog,true); drawerAdapter=new AppCatalog.Adapter(this,theme,catalog); dockAdapter=new AppCatalog.Adapter(this,theme,catalog);
        grid=new GridView(this); grid.setNumColumns(Math.max(3,Math.min(6,prefs.getInt(MobileTheme.key(theme.id,"columns"),4)))); grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH); grid.setVerticalSpacing(Ui.dp(this,6)); grid.setClipToPadding(false); grid.setBackground(drawer?theme.panel(getResources().getDisplayMetrics().density,true):null); grid.setAdapter(drawer?drawerAdapter:homeAdapter);
        grid.setOnItemClickListener((p,v,pos,id)->launch(((AppCatalog.Adapter)grid.getAdapter()).getItem(pos)));
        grid.setOnItemLongClickListener((p,v,pos,id)-> { appMenu(((AppCatalog.Adapter)grid.getAdapter()).getItem(pos),drawer?null:"home"); return true; });
        body.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        dock=new GridView(this); dock.setNumColumns(4); dock.setAdapter(dockAdapter); dock.setBackground(theme.panel(getResources().getDisplayMetrics().density,false)); dock.setVerticalScrollBarEnabled(false);
        dock.setOnItemClickListener((p,v,pos,id)->launch(dockAdapter.getItem(pos))); dock.setOnItemLongClickListener((p,v,pos,id)-> { appMenu(dockAdapter.getItem(pos),"dock"); return true; });
        body.addView(dock,new LinearLayout.LayoutParams(-1,Ui.dp(this,114)));
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
        updateTime(); refreshApps(); loadPhoto();
    }
    private void updateTime() { if(clock!=null) { clock.setText(android.text.format.DateFormat.getTimeFormat(this).format(new Date())); date.setText(android.text.format.DateFormat.format("EEEE, d MMMM",new Date())); } }
    private void showDrawer(boolean open) {
        drawer=open; if(search==null) return;
        search.setVisibility(open?View.VISIBLE:View.GONE); grid.setBackground(open?theme.panel(getResources().getDisplayMetrics().density,true):null); grid.setAdapter(open?drawerAdapter:homeAdapter); dock.setVisibility(open?View.GONE:View.VISIBLE);
        clock.setVisibility(open?View.GONE:View.VISIBLE); date.setVisibility(open?View.GONE:View.VISIBLE);
        if(!open) { search.setText(""); search.clearFocus(); ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0); }
        refreshApps();
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
        options.addView(Ui.button(this,theme,"Notification access",()->open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        TextView note=Ui.text(this,theme,"Notification access reads notifications for your panel. Content stays in memory on this phone; no server or analytics. Android still owns its permission screens and system shade.",14); note.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10)); options.addView(note);
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
