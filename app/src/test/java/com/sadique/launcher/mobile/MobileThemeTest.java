package com.sadique.launcher.mobile;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class MobileThemeTest {
    @Test public void allNineAppearanceNamespacesStayIndependent() {
        Set<String> keys=new HashSet<>();
        for(int style=0;style<MobileTheme.NAMES.length;style++) keys.add(MobileTheme.key(style,"intensity"));
        assertEquals(9,keys.size()); assertEquals("appearance.6.intensity",MobileTheme.key(6,"intensity"));
        assertEquals("appearance.7.intensity",MobileTheme.key(7,"intensity"));
        assertEquals("appearance.8.intensity",MobileTheme.key(8,"intensity"));
        assertEquals(0,MobileTheme.clamp(-5)); assertEquals(8,MobileTheme.clamp(100));
    }
    @Test public void iphoneGlassIsNinthThemeWithSquircleIconCap() {
        assertEquals(9,MobileTheme.NAMES.length); assertEquals("iPhone Glass",MobileTheme.NAMES[8]);
        assertEquals(8,MobileTheme.IPHONE); assertEquals(8,MobileTheme.clamp(8));
        assertEquals(18,MobileTheme.iconRadiusFor(8,24)); assertEquals(18,MobileTheme.iconRadiusFor(8,29));
        assertEquals(12,MobileTheme.iconRadiusFor(8,12));
        assertEquals(24,MobileTheme.iconRadiusFor(0,24)); assertEquals(29,MobileTheme.iconRadiusFor(7,29));
    }
    @Test public void contrastUsesLinearLightAndMixKeepsColourChannelsIndependent() {
        assertEquals(21,MobileTheme.contrast(0xffffffff,0xff000000),.001);
        assertEquals(1,MobileTheme.contrast(0xff808080,0xff808080),.001);
        assertEquals(0xff800080,MobileTheme.mix(0xffff0000,0xff0000ff,.5f));
    }
}
