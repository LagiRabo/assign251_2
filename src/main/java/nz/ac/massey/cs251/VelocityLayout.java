/*
 * 159.251 – Software Design and Construction
 * Assignment 2: Custom Log4j Layout (VelocityLayout)
 * Author: Lagi Rabo (ID: 04225368)
 *
 * Description:
 *   This class defines a custom Log4j layout that uses the Apache Velocity
 *   template engine to format log messages. It supports variables for
 *   message content, date, level, thread name, and more, allowing flexible
 *   log message patterns.
 */

// package for assessment
package nz.ac.massey.cs251;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

/**
 * VelocityLayout formats Log4j log messages using a Velocity template.
 * Example pattern:
 *     "[$p] $c $d: $m$n"
 * Supported variables:
 *   $p → log level (priority)
 *   $c → logger name (category)
 *   $d → date/time of event
 *   $m → log message
 *   $t → thread name
 *   $n → new line
 */
public class VelocityLayout extends Layout {
    // The Velocity template engine handles variable substitution in patterns
    private final VelocityEngine engine;

    // The pattern used to format the log messages
    private String pattern;


    /**
     * Default constructor — sets a default pattern if none is provided.
     * Example output: [INFO] nz.ac.massey.MyClass Mon Oct 14 12:00:00 NZST 2025: Hello world
     */
    public VelocityLayout() {
        this("[$p] $c $d: $m$n");
    }

    /**
     * Main constructor — allows user to specify a custom Velocity pattern.
     * @param pattern custom log message pattern (must not be null)
     */
    public VelocityLayout(String pattern) {
        this.pattern = pattern;

        // Create and configure a Velocity engine instance
        this.engine = new VelocityEngine();

        // Disable Velocity's own logging  - keeps output clean
        Properties p = new Properties();
        p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");

        // Initialize Velocity with these properties
        engine.init(p);
    }

    /**
     * Changes the pattern used by this layout.
     * You can call this at runtime to reconfigure how logs look.
     */
    public void setPattern(String pattern) {
        if(pattern == null) {
            throw new IllegalArgumentException("pattern cannot be null");
        }
        this.pattern = pattern;
    }

    /** Returns the current pattern being used by the layout. */
    public String getPattern(){
        return pattern;
    }

    /**
     * Converts a Log4j LoggingEvent into a formatted string using Velocity.
     *
     * This is where the "magic" happens — we build a context (like a map)
     * of all variables and pass it to the Velocity engine, which replaces
     * variables ($m, $p, $c, etc.) in the pattern with real values.
     */
    @Override
    public String format(LoggingEvent event) {
        // Create a Velocity context to hold variables for this log event
        VelocityContext ctx = new VelocityContext();

        // Populate the context with values from the LoggingEvent
        ctx.put("c", event.getLoggerName());
        ctx.put("d", new Date(event.getTimeStamp()).toString());
        Object msg = event.getMessage();
        ctx.put("m", (msg == null) ? "null": msg.toString());
        ctx.put("p", event.getLevel().toString());
        ctx.put("t", event.getThreadName());
        ctx.put("n", System.lineSeparator());

        // Prepare a Writer to capture the formatted log message
        StringWriter writer = new StringWriter();

        // Ask Velocity to apply the pattern to the provided data
        engine.evaluate(ctx, writer, "log", pattern);

        // Returns the fully formatted log line as a string
        return writer.toString();
    }

    /**
     * Informs Log4j that this layout ignores exceptions (throwables).
     * Returning true means we do not handle stack traces here.
     */
    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    /**
     * Called when layout options are activated.
     * Not used in this implementation, but method must exist.
     */
    @Override
    public void activateOptions() {

    }
}
