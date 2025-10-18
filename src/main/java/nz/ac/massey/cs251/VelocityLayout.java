package nz.ac.massey.cs251;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

public class VelocityLayout extends Layout {
    private final VelocityEngine engine;
    private String pattern;


    public VelocityLayout() {
        this("[$p] $c $d: $m$n");
    }

    public VelocityLayout(String pattern) {
        this.pattern = pattern;
        this.engine = new VelocityEngine();
        Properties p = new Properties();
        p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        engine.init(p);
    }


    public void setPattern(String pattern) {
        if(pattern == null) {
            throw new IllegalArgumentException("pattern cannot be null");
        }
        this.pattern = pattern;
    }

    public String getPattern(){
        return pattern;
    }

    @Override
    public String format(LoggingEvent event) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("c", event.getLoggerName());
        ctx.put("d", new Date(event.getTimeStamp()).toString());
        Object msg = event.getMessage();
        ctx.put("m", (msg == null) ? "null": msg.toString());
        ctx.put("p", event.getLevel().toString());
        ctx.put("t", event.getThreadName());
        ctx.put("n", System.lineSeparator());

        StringWriter writer = new StringWriter();
        engine.evaluate(ctx, writer, "log", pattern);
        return writer.toString();
    }

    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    @Override
    public void activateOptions() {

    }
}
