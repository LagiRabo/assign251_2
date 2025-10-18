package nz.ac.massey.cs251;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ensures MemAppender + VelocityLayout work together and with standard Log4j Logger.
 */
class IntegrationTest {

    private static final String LOGGER_NAME = "IntegrationLogger";
    private Logger logger;

    @BeforeEach
    void setup() {
        MemAppender._resetForTests();
        logger = Logger.getLogger(LOGGER_NAME);
        logger.removeAllAppenders();
    }

    @AfterEach
    void cleanup() {
        logger.removeAllAppenders();
        MemAppender._resetForTests();
    }

    @Test
    void memAppender_with_velocityLayout_collects_expected_output() {
        VelocityLayout vLayout = new VelocityLayout("[$p] $c: $m$n");
        MemAppender app = MemAppender.getInstance(null, vLayout);
        app.setMaxSize(10);
        logger.addAppender(app);

        logger.info("alpha");
        logger.warn("beta");
        logger.error("gamma");

        List<String> lines = app.getEventStrings();
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("[INFO] IntegrationLogger: alpha"));
        assertTrue(lines.get(1).contains("[WARN] IntegrationLogger: beta"));
        assertTrue(lines.get(2).contains("[ERROR] IntegrationLogger: gamma"));
    }

    @Test
    void memAppender_also_works_with_patternLayout() {
        MemAppender app = MemAppender.getInstance();
        app.setLayout(new PatternLayout("%p %c - %m%n"));
        logger.addAppender(app);

        logger.debug("d1");
        logger.info("i1");

        List<String> out = app.getEventStrings();
        assertEquals(2, out.size());
        assertTrue(out.get(0).startsWith("DEBUG IntegrationLogger - d1"));
        assertTrue(out.get(1).startsWith("INFO IntegrationLogger - i1"));
    }
}
