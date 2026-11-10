package com.sadique.launcher.mobile;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaceNameTest {
    // Sample Nominatim reverse responses (shape only, no live network).

    @Test public void villageNameWins() {
        String json = "{\"place_id\":1,\"name\":\"54-2L\",\"display_name\":\"54-2L, Sahiwal, Punjab, Pakistan\","
                + "\"address\":{\"village\":\"54-2L\",\"county\":\"Sahiwal\",\"state\":\"Punjab\",\"country\":\"Pakistan\"}}";
        assertEquals("54-2L", PlaceName.pickName(json));
    }

    @Test public void cityFallbackWhenNoVillage() {
        String json = "{\"place_id\":2,\"display_name\":\"Okara, Punjab, Pakistan\","
                + "\"address\":{\"city\":\"Okara\",\"state\":\"Punjab\",\"country\":\"Pakistan\"}}";
        assertEquals("Okara", PlaceName.pickName(json));
    }

    @Test public void displayNameFirstPartAsLastResort() {
        String json = "{\"place_id\":3,\"display_name\":\"Chak 44, Okara, Punjab, Pakistan\","
                + "\"address\":{\"state\":\"Punjab\",\"country\":\"Pakistan\"}}";
        assertEquals("Chak 44", PlaceName.pickName(json));
    }

    @Test public void nullWhenNothingKnown() {
        assertNull(PlaceName.pickName(null));
        assertNull(PlaceName.pickName("{}"));
        assertNull(PlaceName.pickName("{\"address\":{}}"));
    }
}
