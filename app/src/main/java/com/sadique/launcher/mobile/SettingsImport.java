package com.sadique.launcher.mobile;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Explicit user-selected legacy settings backup. Never imports permissions, secrets or notifications. */
final class SettingsImport {
    private static final Pattern APPEARANCE=Pattern.compile("appearance\\.([0-9]{1,3})\\.(accent|intensity|round|font|columns|transparent)");
    private SettingsImport() { }
    static boolean allowed(String name) {
        if(Arrays.asList("style","home","dock","wallpaper","edge_enabled").contains(name)) return true;
        Matcher match=APPEARANCE.matcher(name==null?"":name);
        return match.matches()&&Integer.parseInt(match.group(1))<MobileTheme.NAMES.length;
    }
    static Map<String,Object> read(InputStream source) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] block=new byte[8192]; int length;
        while((length=source.read(block))!=-1) { if(bytes.size()+length>1048576) throw new IOException("Backup too large"); bytes.write(block,0,length); }
        XmlPullParser parser=Xml.newPullParser(); parser.setInput(new ByteArrayInputStream(bytes.toByteArray()),null);
        Map<String,Object> values=new LinkedHashMap<>(); boolean root=false;
        for(int type=parser.getEventType();type!=XmlPullParser.END_DOCUMENT;type=parser.nextToken()) {
            if(type==XmlPullParser.DOCDECL) throw new IOException("Document declarations are not supported");
            if(type!=XmlPullParser.START_TAG) continue;
            String tag=parser.getName(); if(!root) { if(!"map".equals(tag)) throw new IOException("Invalid backup"); root=true; continue; }
            String name=parser.getAttributeValue(null,"name"); if(!allowed(name)) continue;
            Object value=null;
            if("string".equals(tag)) { String text=parser.nextText(); if(text.length()>32768) throw new IOException("Value too large"); if("home".equals(name)||"dock".equals(name)||"wallpaper".equals(name)) value=text; }
            else if("boolean".equals(tag)) { String text=parser.getAttributeValue(null,"value"); if(!"true".equals(text)&&!"false".equals(text)) throw new IOException("Invalid boolean"); if("edge_enabled".equals(name)||name.endsWith(".transparent")) value=Boolean.parseBoolean(text); }
            else if("int".equals(tag)) { int number=Integer.parseInt(parser.getAttributeValue(null,"value")); if("style".equals(name)) value=MobileTheme.clamp(number); else if(name.startsWith("appearance.")&&!name.endsWith(".transparent")) value=number; }
            if(value!=null) values.put(name,value); if(values.size()>200) throw new IOException("Too many settings");
        }
        if(values.isEmpty()) throw new IOException("No launcher settings in this file"); return values;
    }
}
