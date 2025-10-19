/*
 * 159.251 – Software Design and Construction
 * Assignment 2: Custom Log4j Appender (MemAppender) Unit Tests
 * Author: Lagi Rabo (ID: 04225368)
 *
 * Description:
 *   This test suite validates the behaviour of the MemAppender class.
 *   It checks correct Singleton behaviour, dependency injection,
 *   size management, layout enforcement, and correct log discarding logic.
 */
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

/**
 * Unit tests for the MemAppender class.
 * These tests confirm that:
 *  - Singleton design pattern is enforced.
 *  - Dependency injection works for both List and Layout objects.
 *  - The appender trims old logs correctly and tracks discarded counts.
 *  - Layouts are required before formatting.
 *  - Clearing logs does not count as discarding.
 */
class MemAppenderTest {

    // Shared Logger used across tests
    private static final String LOGGER_NAME = "MemAppenderTestLogger";
    private Logger logger;

    /**
     * Runs before each test case.
     * Ensures a fresh logger and a reset MemAppender state.
     */
    @BeforeEach
    void setup() {
        MemAppender._resetForTests();               // Reset singleton instance
        logger = Logger.getLogger(LOGGER_NAME);     // Get a dedicated logger for tests
    }

    /**
     * Runs after each test case.
     * Cleans up by detaching appenders and resetting state.
     */
    @AfterEach
    void teardown() {
        // Detach all appenders from this logger
        logger.removeAllAppenders();
        MemAppender._resetForTests();
    }

    /**
     * Test 1:
     * Confirms that only one instance of MemAppender can exist.
     * Calling getInstance() multiple times should always return the same object.
     */
    @Test
    void singleton_is_enforced() {
        MemAppender a = MemAppender.getInstance();
        MemAppender b = MemAppender.getInstance();
        assertSame(a, b, "Expected same singleton instance");
    }

    /**
     * Test 2:
     *  Ensures that dependency injection for the backing list works correctly.
     *  The injected List (LinkedList here) should be the one used internally
     *  by MemAppender — verified using reflection to inspect the private field.
     */
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


    /**
     * Test 3:
     * Verifies that layouts can be injected either through the constructor
     * or later via the setter method. Both should work safely.
     */
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

    /**
     * Test 4:
     * Confirms that when the maximum size (maxSize) is reached:
     *  - The oldest log entries are removed.
     *  - The discarded log count increases correctly.
     */
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

    /**
     * Test 5:
     * Ensures that calling getEventStrings() without a layout set
     * results in an IllegalStateException (as required by specification).
     */
    @Test
    void getEventStrings_requires_layout() {
        MemAppender app = MemAppender.getInstance();
        // no layout set yet
        assertThrows(IllegalStateException.class, app::getEventStrings);
    }

    /**
     * Test 6:
     * Verifies that printLogs():
     *  - Prints log messages and then clears them from memory.
     *  - Does NOT increase the discarded log counter (since clearing ≠ discarding).
     */
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
