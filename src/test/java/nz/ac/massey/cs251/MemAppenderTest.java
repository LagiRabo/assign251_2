package nz.ac.massey.cs251;

import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemAppenderTest {

    private static final String LOGGER_NAME = "MemAppenderTestLogger";
    private Logger logger;

    @BeforeEach
    void setup() {
        MemAppender._resetForTests();
        logger = Logger.getLogger(LOGGER_NAME);
    }

    @AfterEach
    void teardown() {
        // Detach all appenders from this logger
        logger.removeAllAppenders();
        MemAppender._resetForTests();
    }

    @Test
    void singleton_is_enforced() {
        MemAppender a = MemAppender.getInstance();
        MemAppender b = MemAppender.getInstance();
        assertSame(a, b, "Expected same singleton instance");
    }

    @Test
    void dependency_injection_backing_list_is_used() throws Exception {
        List<LoggingEvent> injected = new LinkedList<>();
        MemAppender app = MemAppender.getInstance(injected, null);

        // reflect to confirm the exact backing list instance is in use
        Field f = MemAppender.class.getDeclaredField("logs");
        f.setAccessible(true);
        Object internal = f.get(app);
        assertSame(injected, internal, "MemAppender should use injected backing list");
    }

    @Test
    void dependency_injection_layout_via_constructor_or_setter() {
        Layout velocity = new VelocityLayout("X $m");
        MemAppender app = MemAppender.getInstance(new ArrayList<>(), velocity);
        assertTrue(app.requiresLayout());

        // Works via setter too
        Layout pattern = new PatternLayout("%m%n");
        app.setLayout(pattern);
        // No exception expected
    }

    @Test
    void maxSize_trims_oldest_and_tracks_discarded() {
        MemAppender app = MemAppender.getInstance();
        app.setLayout(new PatternLayout("%m%n"));
        app.setMaxSize(3);

        logger.addAppender(app);
        logger.info("A");
        logger.info("B");
        logger.info("C");
        assertEquals(3, app.getCurrentLogs().size());
        assertEquals(0L, app.getDiscardedLogCount());

        // Next insertion should discard one (A)
        logger.info("D");
        assertEquals(3, app.getCurrentLogs().size());
        assertEquals(1L, app.getDiscardedLogCount());

        List<String> lines = app.getEventStrings();
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("B"));
        assertTrue(lines.get(1).contains("C"));
        assertTrue(lines.get(2).contains("D"));
    }

    @Test
    void getEventStrings_requires_layout() {
        MemAppender app = MemAppender.getInstance();
        // no layout set yet
        assertThrows(IllegalStateException.class, app::getEventStrings);
    }

    @Test
    void printLogs_prints_and_clears_but_does_not_increase_discarded() {
        MemAppender app = MemAppender.getInstance();
        app.setLayout(new PatternLayout("%p - %m%n"));
        app.setMaxSize(10);

        logger.addAppender(app);
        logger.warn("W1");
        logger.error("E1");
        assertEquals(2, app.getCurrentLogs().size());
        long before = app.getDiscardedLogCount();

        // Should print to stdout and clear
        app.printLogs();

        assertEquals(0, app.getCurrentLogs().size());
        assertEquals(before, app.getDiscardedLogCount(), "Clearing should not increase discarded count");
    }
}
