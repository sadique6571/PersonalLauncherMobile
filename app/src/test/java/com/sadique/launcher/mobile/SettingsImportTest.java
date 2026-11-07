package com.sadique.launcher.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class SettingsImportTest {
    @Test public void onlyMobilePreferencesAreAllowedAcrossAllRegisteredThemes() {
        for(int style=0;style<MobileTheme.NAMES.length;style++) assertTrue(SettingsImport.allowed(MobileTheme.key(style,"accent")));
        assertTrue(SettingsImport.allowed("home")); assertTrue(SettingsImport.allowed("dock"));
        for(String key:new String[]{"MOBILE_KEYSTORE_PASSWORD","notifications","enabled_accessibility_services","appearance.999.accent","appearance.0.secret","weather_lat"}) assertFalse(SettingsImport.allowed(key));
    }
}
