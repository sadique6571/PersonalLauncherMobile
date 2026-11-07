package com.sadique.launcher.mobile;

import java.util.*;

/** Component-based ordering keeps separate launch activities from the same app distinct. */
final class HomeOrder {
    private HomeOrder() { }
    static List<String> decode(String value) {
        LinkedHashSet<String> unique=new LinkedHashSet<>();
        for(String key:value.split("\\n")) if(!key.isEmpty()) unique.add(key);
        return new ArrayList<>(unique);
    }
    static String encode(List<String> keys) { return String.join("\n",new LinkedHashSet<>(keys)); }
    static List<String> move(List<String> keys,String key,int delta) {
        List<String> copy=new ArrayList<>(keys); int index=copy.indexOf(key);
        if(index>=0) Collections.swap(copy,index,Math.max(0,Math.min(copy.size()-1,index+delta)));
        return copy;
    }
}
