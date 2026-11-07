package com.sadique.launcher.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class NotificationTextTest {
    @Test public void controlsAndOversizedTextAreBoundedWithoutHtmlInterpretation() {
        assertEquals("hello\nworld",NotificationText.clean("hello\nworld\u0000\u0007",100));
        assertEquals("<b>test</b>",NotificationText.clean("<b>test</b>",50));
        assertEquals(600,NotificationText.clean("a".repeat(10000),600).length());
        assertEquals("",NotificationText.clean(null,10)); assertEquals("",NotificationText.clean("abc",0));
    }
    @Test public void truncationDoesNotLeaveHalfAnEmoji() {
        assertEquals("a",NotificationText.clean("a\ud83d\ude00b",2));
        assertEquals("a\ud83d\ude00",NotificationText.clean("a\ud83d\ude00b",3));
    }
}
