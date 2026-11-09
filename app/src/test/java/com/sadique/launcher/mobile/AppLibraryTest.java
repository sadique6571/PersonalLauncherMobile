package com.sadique.launcher.mobile;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class AppLibraryTest {
    @Test public void sectionIsFirstLetterOrHash() {
        assertEquals("W",AppLibrary.sectionFor("WhatsApp"));
        assertEquals("A",AppLibrary.sectionFor("apple"));
        assertEquals("#",AppLibrary.sectionFor("42 Wallaby"));
        assertEquals("#",AppLibrary.sectionFor(""));
        assertEquals("#",AppLibrary.sectionFor(null));
    }
    @Test public void sectionsAreOrderedAZWithHashLast() {
        LinkedHashMap<String,List<Integer>> grouped=AppLibrary.sections(Arrays.asList("Banana","Apple","avocado","42","Cherry"));
        assertEquals(Arrays.asList("A","B","C","#"),new ArrayList<>(grouped.keySet()));
        assertEquals(Arrays.asList(1,2),grouped.get("A"));
        assertEquals(Arrays.asList(0),grouped.get("B"));
        assertEquals(Arrays.asList(4),grouped.get("C"));
        assertEquals(Arrays.asList(3),grouped.get("#"));
    }
    @Test public void emptyListHasNoSections() {
        assertTrue(AppLibrary.sections(Collections.emptyList()).isEmpty());
    }
}
