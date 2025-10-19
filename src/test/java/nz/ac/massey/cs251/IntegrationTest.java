/*
 * 159.251 – Software Design and Construction
 * Assignment 2: Integration Testing for Custom Log4j Components
 * Author: Lagi Rabo (ID: 04225368)
 *
 * Description:
 *   This class contains integration tests that verify both MemAppender and
 *   VelocityLayout work correctly together, and that MemAppender also behaves
 *   as expected with the standard Log4j PatternLayout.
 */
package nz.ac.massey.cs251;



import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IntegrationTest ensures that:
 *  - MemAppender and VelocityLayout interact correctly.
 *  - MemAppender can also work with the standard Log4j PatternLayout.
 *  - Log messages are captured, formatted, and verified end-to-end.
 */
class IntegrationTest {

    // --- Constants and shared test objects ---
    private static final String LOGGER_NAME = "IntegrationLogger";
    private Logger logger;

    /**
     * Runs before each test case.
     * - Resets the MemAppender singleton to ensure a clean slate.
     * - Creates a new Logger instance.
     * - Removes any existing appenders so tests are isolated.
     */
    @BeforeEach
    void setup() {
        MemAppender._resetForTests();
        logger = Logger.getLogger(LOGGER_NAME);
        logger.removeAllAppenders();
    }

    /**
     * Runs after each test case.
     * - Cleans up logger configuration.
     * - Resets MemAppender again so future tests are unaffected.
     */
    @AfterEach
    void cleanup() {
        logger.removeAllAppenders();
        MemAppender._resetForTests();
    }

    /**
     * Test 1:
     * Ensures that MemAppender works correctly when paired with VelocityLayout.
     * Steps:
     *   1. Create a VelocityLayout with a simple pattern.
     *   2. Attach MemAppender (using that layout) to the logger.
     *   3. Log three messages of different levels.
     *   4. Retrieve logs from MemAppender and confirm they were stored and formatted as expected.
     */
    @Test
    void memAppender_with_velocityLayout_collects_expected_output() {
        // Use a Velocity pattern that includes level, category and message
        VelocityLayout vLayout = new VelocityLayout("[$p] $c: $m$n");

        // Create a singleton MemAppender using the custom layout
        MemAppender app = MemAppender.getInstance(null, vLayout);
        app.setMaxSize(10);
        logger.addAppender(app);    // Attach appender to the logger

        // Send log messages at different levels
        logger.info("alpha");
        logger.warn("beta");
        logger.error("gamma");

        // Retrieve the formatted event strings from memory
        List<String> lines = app.getEventStrings();

        // Validate: three log entries, formatted as expected
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("[INFO] IntegrationLogger: alpha"));
        assertTrue(lines.get(1).contains("[WARN] IntegrationLogger: beta"));
        assertTrue(lines.get(2).contains("[ERROR] IntegrationLogger: gamma"));
    }

    /**
     * Test 2:
     * Ensures that MemAppender also works properly with the built-in PatternLayout.
     * This checks compatibility between the custom in-memory appender and a
     * standard Log4j layout, proving that our appender integrates seamlessly
     * with existing Log4j components.
     */
    @Test
    void memAppender_also_works_with_patternLayout() {
        // Create a MemAppender with the default instance (no injected layout)
        MemAppender app = MemAppender.getInstance();

        // Apply a standard Log4j PatternLayout format
        app.setLayout(new PatternLayout("%p %c - %m%n"));
        logger.addAppender(app);

        // Log two messages at different levels
        logger.debug("d1");
        logger.info("i1");

        // Fetch formatted log output from memory
        List<String> out = app.getEventStrings();

        // Validate: two log entries captured with correct formatting
        assertEquals(2, out.size());
        assertTrue(out.get(0).startsWith("DEBUG IntegrationLogger - d1"));
        assertTrue(out.get(1).startsWith("INFO IntegrationLogger - i1"));
    }
}
