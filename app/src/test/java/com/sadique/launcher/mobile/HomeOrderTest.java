package com.sadique.launcher.mobile;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class HomeOrderTest {
    @Test public void duplicatesAndEmptyEntriesCannotCreateDuplicateTiles() {
        assertEquals(Arrays.asList("one/.A","one/.B"),HomeOrder.decode("one/.A\n\none/.B\none/.A"));
        assertEquals("one/.A\none/.B",HomeOrder.encode(Arrays.asList("one/.A","one/.B","one/.A")));
    }
    @Test public void movingAtEitherEdgeKeepsEveryAppAndDoesNotMutateTheSource() {
        List<String> source=Arrays.asList("a","b","c");
        assertEquals(source,HomeOrder.move(source,"a",-1)); assertEquals(source,HomeOrder.move(source,"c",1));
        assertEquals(Arrays.asList("b","a","c"),HomeOrder.move(source,"a",1));
        assertEquals(Arrays.asList("a","b","c"),source); assertEquals(source,HomeOrder.move(source,"missing",1));
    }
}
