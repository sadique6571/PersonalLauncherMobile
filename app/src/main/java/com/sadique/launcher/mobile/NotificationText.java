package com.sadique.launcher.mobile;

/** Bound untrusted notification text without interpreting HTML or loading remote content. */
final class NotificationText {
    private NotificationText() { }
    static String clean(CharSequence input,int limit) {
        if(input==null || limit<=0) return "";
        StringBuilder text=new StringBuilder();
        for(int i=0;i<input.length() && text.length()<limit;i++) {
            char c=input.charAt(i);
            if(!Character.isISOControl(c) || c=='\n' || c=='\t') text.append(c);
        }
        if(text.length()>0 && Character.isHighSurrogate(text.charAt(text.length()-1))) text.setLength(text.length()-1);
        return text.toString().trim();
    }
}
