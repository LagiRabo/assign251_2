/*
159.251 - Assignment 2
Compiled by Lagi Rabo ID: 04225368
 * Description:
 *   This class implements a custom in-memory Log4j appender (MemAppender)
 *   designed to temporarily store logging events in memory and print them
 *   on demand. It enforces the Singleton design pattern and supports
 *   dependency injection for its internal list and layout components.
 */

// package provided that connects to GitHub project
package nz.ac.massey.cs251;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.util.*;

/**
 * The MemAppender class acts as a custom Log4j Appender that stores logs
 * in memory instead of writing them to a file or console. When its maximum
 * size is reached, the oldest logs are discarded.
 */
public class MemAppender extends AppenderSkeleton {

    // --- Static instance (Singleton pattern) ---
    // Ensures only one MemAppender instance exists throughout the application.
    private static MemAppender instance;

    // --- Core data members ---
    private final List<LoggingEvent> logs;
    private int maxSize = 1000;
    private long discardedCount = 0;

    /**
     * Private constructor (enforcing Singleton).
     * Uses dependency injection to accept an external list and layout.
     *
     * @param injectedList an externally provided list to store LoggingEvents
     * @param layout       an optional layout to format log output
     */
    private MemAppender(List<LoggingEvent> injectedList, Layout layout) {
        // If no list is injected, default to ArrayList
        this.logs = injectedList != null ? injectedList : new ArrayList<>();

        // Optional set layout if provided
        if (layout != null) {
            this.setLayout(layout);
        }
    }

    // --- Singleton Accessors ---

    /**
     * Retrieves the singleton instance of MemAppender using default settings.
     */
    public static MemAppender getInstance() {
        return getInstance(null, null);
    }

    /**
     * Retrieves the singleton instance, creating it if it does not exist.
     * Allows dependency injection of a custom list and/or layout.
     *
     * @param injectedList external list implementation (e.g., ArrayList, LinkedList)
     * @param layout       optional layout for log formatting
     * @return the single MemAppender instance
     */
    public static synchronized MemAppender getInstance(List<LoggingEvent> injectedList, Layout layout) {
        if (instance == null) {
            instance = new MemAppender(injectedList, layout);
        } else {
            // Allow runtime layout updates if already instantiated
            if (layout != null) {
                instance.setLayout(layout);
            }
        }
        return instance;
    }

    /**
     * Helper for resetting the singleton instance.
     * This is used exclusively for testing to ensure a clean state.
     */
    public static synchronized void _resetForTests(){
        instance = null;
    }

    // --- Configuration Methods ---

    /**
     * Sets the maximum size of the in-memory log list.
     * If reduced, oldest logs are discarded until the limit is met.
     *
     * @param maxSize maximum number of logs retained in memory
     * @throws IllegalArgumentException if maxSize <= 0
     */
    public void setMaxSize(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        this.maxSize = maxSize;

        // if reducing size, trim older logs immediately
        while (logs.size() > this.maxSize) {
            logs.remove(0);
            discardedCount++;
        }
    }

    /** @return the configured maximum number of retained logs */
    public int getMaxSize() {
        return maxSize;

    }

    /** @return total count of discarded logs due to buffer overflow */
    public long getDiscardedLogCount() {
        return discardedCount;
    }

    // --- Core Logging Logic ---

    /**
     * Appends a new log event to the internal list.
     * If the buffer is full, removes the oldest entry.
     *
     * @param event the log event to add
     */
    @Override
    protected void append(LoggingEvent event) {
        // Discard oldest logs when capacity is reached
        while (logs.size() >= maxSize) {
            logs.remove(0);
            discardedCount++;
        }
        logs.add(event);

    }

    // --- Retrieval and Output Methods ---

    /**
     * Returns an unmodifiable view of current LoggingEvent objects.
     *
     * @return immutable list of LoggingEvent entries
     */
    public List<LoggingEvent> getCurrentLogs() {
        return Collections.unmodifiableList(logs);
    }

    /**
     * Returns formatted log strings using the configured layout.
     *
     * @return list of formatted log strings
     * @throws IllegalStateException if no layout is set
     */
    public List<String> getEventStrings() {
        if (this.layout == null) {
            throw new IllegalStateException("Layout not set for MemAppender");
        }
        List<String> result = new ArrayList<>();
        for (LoggingEvent e : logs) {
            result.add(this.layout.format(e));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Prints all stored logs to the console using the layout,
     * then clears the internal memory buffer.
     *
     * Note: cleared logs are *not* counted as discarded.
     *
     * @throws IllegalStateException if no layout is set
     */
    public void printLogs() {
        if (this.layout == null) {
            throw new IllegalStateException("Layout not set for MemAppender");
        }

        // Print all formatted logs to console
        for (LoggingEvent e : logs) {
            System.out.print(this.layout.format(e));
        }
        logs.clear(); // clearing does not count as discarded
    }

    // --- Required Abstract Methods from AppenderSkeleton ---

    /** Clears the log buffer and releases resources (no I/O resources used here). */
    @Override
    public void close() {
        logs.clear();
    }

    /** Indicates that this appender requires a Layout object for formatting. */
    @Override
    public boolean requiresLayout() {
        return true;
    }
}
