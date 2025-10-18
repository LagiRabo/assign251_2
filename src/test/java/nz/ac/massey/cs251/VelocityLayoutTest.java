package nz.ac.massey.cs251;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VelocityLayoutTest {

    @Test
    void default_pattern_formats_all_core_fields() {
        VelocityLayout layout = new VelocityLayout(); // "[$p] $c $d: $m$n"
        Logger logger = Logger.getLogger("VLTest");
        LoggingEvent ev = new LoggingEvent(
                logger.getClass().getName(),
                logger,
                System.currentTimeMillis(),
                Level.INFO,
                "hello world",
                Thread.currentThread().getName(),
                null, null, null, null
        );

        String s = layout.format(ev);
        assertTrue(s.contains("[INFO]"), "should include level");
        assertTrue(s.contains("VLTest"), "should include logger name");
        assertTrue(s.contains("hello world"), "should include message");
        assertTrue(s.endsWith(System.lineSeparator()), "should end with newline via $n");
    }

    @Test
    void custom_pattern_works_and_supports_variables() {
        VelocityLayout layout = new VelocityLayout("$p|$c|$m|$t$n");
        Logger logger = Logger.getLogger("CustomLogger");
        LoggingEvent ev = new LoggingEvent(
                logger.getClass().getName(),
                logger,
                System.currentTimeMillis(),
                Level.WARN,
                "custom message",
                Thread.currentThread().getName(),
                null, null, null, null
        );

        String s = layout.format(ev);
        assertTrue(s.startsWith("WARN|CustomLogger|custom message|"), "pattern replacement should work");
        assertTrue(s.endsWith(System.lineSeparator()));
    }

    @Test
    void null_message_is_handled_gracefully() {
        VelocityLayout layout = new VelocityLayout("$m$n");
        Logger logger = Logger.getLogger("NullMsg");
        LoggingEvent ev = new LoggingEvent(
                logger.getClass().getName(),
                logger,
                System.currentTimeMillis(),
                Level.DEBUG,
                null,
                Thread.currentThread().getName(),
                null, null, null, null
        );

        String s = layout.format(ev);
        assertEquals("null" + System.lineSeparator(), s);
    }
}
