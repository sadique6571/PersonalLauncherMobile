package com.sadique.launcher.mobile;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class MobileThemeTest {
    @Test public void allEightAppearanceNamespacesStayIndependent() {
        Set<String> keys=new HashSet<>();
        for(int style=0;style<MobileTheme.NAMES.length;style++) keys.add(MobileTheme.key(style,"intensity"));
        assertEquals(8,keys.size()); assertEquals("appearance.6.intensity",MobileTheme.key(6,"intensity"));
        assertEquals("appearance.7.intensity",MobileTheme.key(7,"intensity"));
        assertEquals(0,MobileTheme.clamp(-5)); assertEquals(7,MobileTheme.clamp(100));
    }
    @Test public void contrastUsesLinearLightAndMixKeepsColourChannelsIndependent() {
        assertEquals(21,MobileTheme.contrast(0xffffffff,0xff000000),.001);
        assertEquals(1,MobileTheme.contrast(0xff808080,0xff808080),.001);
        assertEquals(0xff800080,MobileTheme.mix(0xffff0000,0xff0000ff,.5f));
    }
}
