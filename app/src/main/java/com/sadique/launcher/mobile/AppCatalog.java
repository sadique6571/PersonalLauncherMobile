package com.sadique.launcher.mobile;

import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

final class AppCatalog implements AutoCloseable {
    static final class Entry {
        final String key,label; final ResolveInfo info;
        Entry(ResolveInfo info,PackageManager pm) { this.info=info; key=new ComponentName(info.activityInfo.packageName,info.activityInfo.name).flattenToString(); label=info.loadLabel(pm).toString(); }
    }
    private final Context context;
    private final Handler main=new Handler(android.os.Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final LruCache<String,Drawable.ConstantState> icons=new LruCache<>(64);
    private final Set<String> pending=new HashSet<>();
    private int generation;
    private boolean closed;
    AppCatalog(Context context) { this.context=context; }
    void scan(java.util.function.Consumer<List<Entry>> receive) {
        int request=++generation;
        worker.execute(()-> {
            List<Entry> entries=new ArrayList<>(); Set<String> seen=new HashSet<>(); PackageManager pm=context.getPackageManager();
            for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)) {
                if(info.activityInfo.packageName.equals(context.getPackageName())) continue;
                try { Entry entry=new Entry(info,pm); if(seen.add(entry.key)) entries.add(entry); } catch(RuntimeException removed) { /* Package changed during scan. */ }
            }
            entries.sort((a,b)->a.label.compareToIgnoreCase(b.label));
            main.post(()-> { if(!closed && request==generation) receive.accept(entries); });
        });
    }
    void icon(Entry app,ImageView view) {
        view.setTag(app.key); Drawable.ConstantState cached=icons.get(app.key);
        if(cached!=null) { view.setImageDrawable(cached.newDrawable(context.getResources())); return; }
        view.setImageDrawable(context.getDrawable(android.R.drawable.sym_def_app_icon));
        if(closed || !pending.add(app.key)) return;
        worker.execute(()-> {
            Drawable image; try { image=app.info.loadIcon(context.getPackageManager()); } catch(RuntimeException removed) { image=null; }
            Drawable loaded=image;
            main.post(()-> {
                pending.remove(app.key); if(closed || loaded==null) return;
                if(loaded.getConstantState()!=null) icons.put(app.key,loaded.getConstantState());
                if(app.key.equals(view.getTag())) view.setImageDrawable(loaded);
                if(onIcon!=null) onIcon.run();
            });
        });
    }
    Runnable onIcon;
    void invalidate() { icons.evictAll(); }
    @Override public void close() { closed=true; generation++; onIcon=null; worker.shutdownNow(); main.removeCallbacksAndMessages(null); }
    static final class Adapter extends BaseAdapter {
        final AppCatalog catalog; final MobileTheme theme; final Context context;
        final boolean wallpaperLabels;
        List<Entry> entries=Collections.emptyList();
        Adapter(Context c,MobileTheme t,AppCatalog catalog) { this(c,t,catalog,false); }
        Adapter(Context c,MobileTheme t,AppCatalog catalog,boolean wallpaperLabels) { context=c; theme=t; this.catalog=catalog; this.wallpaperLabels=wallpaperLabels; }
        void set(List<Entry> entries) { this.entries=entries; notifyDataSetChanged(); }
        public int getCount() { return entries.size(); }
        public Entry getItem(int position) { return entries.get(position); }
        public long getItemId(int position) { return position; }
        public View getView(int position,View recycled,ViewGroup parent) {
            LinearLayout tile;
            if(recycled instanceof LinearLayout) tile=(LinearLayout)recycled;
            else {
                tile=Ui.column(context); tile.setGravity(Gravity.CENTER); tile.setPadding(Ui.dp(context,4),Ui.dp(context,10),Ui.dp(context,4),Ui.dp(context,6));
                ImageView icon=new ImageView(context); icon.setScaleType(ImageView.ScaleType.FIT_CENTER); icon.setClipToOutline(true); icon.setBackground(theme.panel(context.getResources().getDisplayMetrics().density,false));
                icon.setPadding(Ui.dp(context,7),Ui.dp(context,7),Ui.dp(context,7),Ui.dp(context,7)); tile.addView(icon,new LinearLayout.LayoutParams(Ui.dp(context,58),Ui.dp(context,58)));
                TextView label=Ui.text(context,theme,"",12); if(wallpaperLabels) { label.setTextColor(android.graphics.Color.WHITE); label.setShadowLayer(Ui.dp(context,3),0,Ui.dp(context,1),android.graphics.Color.BLACK); } label.setGravity(Gravity.CENTER); label.setMaxLines(2); label.setEllipsize(android.text.TextUtils.TruncateAt.END); label.setMinHeight(Ui.dp(context,36));
                tile.addView(label,new LinearLayout.LayoutParams(-1,Ui.dp(context,36)));
            }
            Entry app=getItem(position); ((TextView)tile.getChildAt(1)).setText(app.label); tile.setContentDescription(app.label);
            catalog.icon(app,(ImageView)tile.getChildAt(0)); return tile;
        }
    }
}
