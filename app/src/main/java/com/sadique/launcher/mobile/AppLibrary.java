package com.sadique.launcher.mobile;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * App Library: the drawer apps grouped by the first letter of their label.
 * Android exposes no reliable per-app category, so grouping is by label
 * initial only — A–Z sections in order, then "#" for labels that do not
 * start with a letter. Search still filters by label, as before.
 */
final class AppLibrary {
    private AppLibrary() { }

    /** Section key for one label: its first letter, upper-cased, or "#". */
    static String sectionFor(String label) {
        if(label==null || label.isEmpty()) return "#";
        char first=label.charAt(0);
        if(first>='a' && first<='z') return String.valueOf((char)(first-'a'+'A'));
        if(first>='A' && first<='Z') return String.valueOf(first);
        return "#";
    }

    /** Ordered section title → positions in the label list. A–Z first, "#" last. */
    static LinkedHashMap<String,List<Integer>> sections(List<String> labels) {
        LinkedHashMap<String,List<Integer>> bySection=new LinkedHashMap<>();
        for(int i=0;i<labels.size();i++) bySection.computeIfAbsent(sectionFor(labels.get(i)),k->new ArrayList<>()).add(i);
        LinkedHashMap<String,List<Integer>> ordered=new LinkedHashMap<>();
        List<String> titles=new ArrayList<>(bySection.keySet());
        titles.sort((a,b)-> "#".equals(a)?1:"#".equals(b)?-1:a.compareTo(b));
        for(String title:titles) ordered.put(title,bySection.get(title));
        return ordered;
    }

    /** Library sections view, for the Home ScrollView: one header per section, four tiles per row. */
    static LinearLayout build(Context context,MobileTheme theme,AppCatalog catalog,List<AppCatalog.Entry> apps,Consumer<AppCatalog.Entry> launch,Consumer<AppCatalog.Entry> longPress) {
        LinearLayout body=Ui.column(context); body.setPadding(0,Ui.dp(context,4),0,Ui.dp(context,8));
        List<String> labels=new ArrayList<>(); for(AppCatalog.Entry app:apps) labels.add(app.label);
        for(Map.Entry<String,List<Integer>> section:sections(labels).entrySet()) {
            TextView header=Ui.text(context,theme,section.getKey(),15); header.setTypeface(theme.font,Typeface.BOLD);
            header.setTextColor(Color.WHITE); header.setShadowLayer(Ui.dp(context,3),0,Ui.dp(context,1),Color.BLACK);
            header.setPadding(Ui.dp(context,4),Ui.dp(context,14),0,Ui.dp(context,4)); body.addView(header);
            List<Integer> positions=section.getValue();
            for(int start=0;start<positions.size();start+=4) {
                LinearLayout row=Ui.row(context);
                for(int i=start;i<Math.min(start+4,positions.size());i++) {
                    AppCatalog.Entry app=apps.get(positions.get(i));
                    row.addView(tile(context,theme,catalog,app,launch,longPress),new LinearLayout.LayoutParams(0,-2,1));
                }
                for(int i=positions.size()-start;i<4 && i>0;i++) row.addView(new View(context),new LinearLayout.LayoutParams(0,-2,1));
                body.addView(row);
            }
        }
        if(apps.isEmpty()) { TextView empty=Ui.text(context,theme,"No apps match",15); empty.setPadding(Ui.dp(context,4),Ui.dp(context,16),0,0); body.addView(empty); }
        return body;
    }

    /** One tile, mirroring the AppCatalog adapter tile (58dp squircle plate + label). */
    private static View tile(Context context,MobileTheme theme,AppCatalog catalog,AppCatalog.Entry app,Consumer<AppCatalog.Entry> launch,Consumer<AppCatalog.Entry> longPress) {
        LinearLayout tile=Ui.column(context); tile.setGravity(Gravity.CENTER); tile.setPadding(Ui.dp(context,4),Ui.dp(context,10),Ui.dp(context,4),Ui.dp(context,6));
        ImageView icon=new ImageView(context); icon.setScaleType(ImageView.ScaleType.FIT_CENTER); icon.setClipToOutline(true);
        android.graphics.drawable.GradientDrawable plate=new android.graphics.drawable.GradientDrawable();
        plate.setColor(0x14ffffff); plate.setCornerRadius(Ui.dp(context,theme.iconRadius())); icon.setBackground(plate);
        icon.setPadding(Ui.dp(context,2),Ui.dp(context,2),Ui.dp(context,2),Ui.dp(context,2));
        tile.addView(icon,new LinearLayout.LayoutParams(Ui.dp(context,58),Ui.dp(context,58)));
        TextView label=Ui.text(context,theme,app.label,12); label.setTextColor(Color.WHITE);
        label.setShadowLayer(Ui.dp(context,3),0,Ui.dp(context,1),Color.BLACK); label.setGravity(Gravity.CENTER);
        label.setMaxLines(2); label.setEllipsize(android.text.TextUtils.TruncateAt.END); label.setMinHeight(Ui.dp(context,36));
        tile.addView(label,new LinearLayout.LayoutParams(-1,Ui.dp(context,36)));
        tile.setContentDescription(app.label);
        catalog.icon(app,icon);
        tile.setOnClickListener(v->launch.accept(app));
        tile.setOnLongClickListener(v-> { longPress.accept(app); return true; });
        return tile;
    }
}
