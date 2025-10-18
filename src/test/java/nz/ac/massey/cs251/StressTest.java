package nz.ac.massey.cs251;

import org.apache.log4j.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple stress tests to generate ~10k log events and compare basic behavior and
 * print (to stdout) rough time/memory measurements across:
 *  - MemAppender (ArrayList vs LinkedList)
 *  - ConsoleAppender
 *  - FileAppender
 *  - VelocityLayout vs PatternLayout
 * These tests avoid brittle timing assertions; they validate counts and emit measurements
 * for your performance-analysis.pdf.
 */
class StressTest {

    private static final String LAYOUT_PATTERN = "%p %c - %m%n";
    private static final int N = 10_000;       // per brief
    private static final int MAX_SIZE = 5_000; // exercise overflow behavior too

    private Logger logger;

    @BeforeEach
    void setup() {
        MemAppender._resetForTests();
        logger = Logger.getLogger("StressLogger");
        logger.removeAllAppenders();
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void cleanup() {
        logger.removeAllAppenders();
        MemAppender._resetForTests();
    }

    private long usedMem() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private long time(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return System.nanoTime() - t0;
    }

    private void produceLogs() {
        for (int i = 0; i < N; i++) {
            logger.info("msg-" + i);
        }
    }
    // ---- CSV writer (idempotent header) ----
    private void writeCsv(String scenario, long nanos, long memDeltaKb, int stored, long discarded, long fileBytes) throws Exception {
        Path out = Paths.get("target", "perf-results.csv");
        if (!Files.exists(out)) {
            Files.createDirectories(out.getParent());
            String header = "scenario,time_ms,mem_kb,stored,discarded,file_bytes" + System.lineSeparator();
            Files.write(out, header.getBytes(StandardCharsets.UTF_8));
        }
        String line = String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d%n",
                scenario,
                TimeUnit.NANOSECONDS.toMillis(nanos),
                memDeltaKb, stored, discarded, fileBytes);
        Files.write(out, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }

    @Test
    void compare_memAppender_backing_list_and_layouts() throws Exception {
        // --- ArrayList + Velocity ---
        {
            List<org.apache.log4j.spi.LoggingEvent> backing = new ArrayList<>();
            VelocityLayout vLayout = new VelocityLayout("[$p] $c: $m$n");
            MemAppender app = MemAppender.getInstance(backing, vLayout);
            app.setMaxSize(MAX_SIZE);
            logger.addAppender(app);

            long memBefore = usedMem();
            long t = time(this::produceLogs);
            long memAfter = usedMem();

            // Count at most MAX_SIZE stored
            int stored = app.getCurrentLogs().size();
            long discarded = app.getDiscardedLogCount();

            assertTrue(stored <= MAX_SIZE);

            long memDeltaKb = (memAfter - memBefore) / 1024;

            // Some discarded expected if N > MAX_SIZE
            //assertEquals(Math.max(0, N - MAX_SIZE), app.getDiscardedLogCount());

            System.out.printf(Locale.ROOT,
                    "[Stress] MemAppender(ArrayList)+Velocity: time=%d ms, memDelta=%d KB, stored=%d, discarded=%d",
                    TimeUnit.NANOSECONDS.toMillis(t),
                    memDeltaKb,
                    stored,
                    discarded);
            System.out.flush();

            writeCsv("MemAppender(ArrayList)+Velocity", t, memDeltaKb, stored,discarded, 0);

            logger.removeAppender(app);
            MemAppender._resetForTests();
        }

        // --- LinkedList + PatternLayout ---
        {
            List<org.apache.log4j.spi.LoggingEvent> backing = new LinkedList<>();
            PatternLayout pLayout = new PatternLayout(LAYOUT_PATTERN);
            MemAppender app = MemAppender.getInstance(backing, pLayout);
            app.setMaxSize(MAX_SIZE);
            logger.addAppender(app);

            long memBefore = usedMem();
            long t = time(this::produceLogs);
            long memAfter = usedMem();

            int stored = app.getCurrentLogs().size();
            long discarded = app.getDiscardedLogCount();
            assertTrue(stored <= MAX_SIZE);

            long memDeltaKb = (memAfter - memBefore) / 1024;
            //assertEquals(Math.max(0, N - MAX_SIZE), app.getDiscardedLogCount());

            System.out.printf(Locale.ROOT,
                    "[Stress] MemAppender(LinkedList)+Pattern: time=%d ms, memDelta=%d KB, stored=%d, discarded=%d",
                    TimeUnit.NANOSECONDS.toMillis(t),
                    memDeltaKb,
                    stored,
                    discarded);

            logger.removeAppender(app);
            MemAppender._resetForTests();
        }
    }

    @Test
    void compare_console_and_file_appenders() throws Exception {
        // ConsoleAppender
        {
            ConsoleAppender ca = new ConsoleAppender(new PatternLayout(LAYOUT_PATTERN));
            logger.addAppender(ca);

            long memBefore = usedMem();
            long t = time(this::produceLogs);
            long memAfter = usedMem();
            long memDeltaKb = (memAfter - memBefore) / 1024;

            System.out.printf(Locale.ROOT,
                    "[Stress] ConsoleAppender+Pattern: time=%d ms, memDelta=%d KB",
                    TimeUnit.NANOSECONDS.toMillis(t),memDeltaKb);
            System.out.flush();

            writeCsv("ConsoleAppender+Pattern", t, memDeltaKb, -1, -1, 0);

            logger.removeAppender(ca);
        }

        // FileAppender
        {
            File tmp = File.createTempFile("stress-", ".log");
            tmp.deleteOnExit();
            FileAppender fa = new FileAppender(new PatternLayout(LAYOUT_PATTERN), tmp.getAbsolutePath(), false);
            logger.addAppender(fa);

            long memBefore = usedMem();
            long t = time(this::produceLogs);
            long memAfter = usedMem();
            long memDeltaKb = (memAfter - memBefore) / 1024;

            System.out.printf(Locale.ROOT,
                    "[Stress] FileAppender+Pattern (%s): time=%d ms, memDelta=%d KB, size=%d bytes",
                    tmp.getName(),
                    TimeUnit.NANOSECONDS.toMillis(t),
                    memDeltaKb,
                    tmp.length());
            System.out.flush();

            writeCsv("FileAppender + Pattern", t, memDeltaKb, -1, -1, tmp.length());

            logger.removeAppender(fa);
            fa.close();
        }

        // Sanity: nothing left attached
        assertFalse(logger.getAllAppenders().hasMoreElements());
    }
}
