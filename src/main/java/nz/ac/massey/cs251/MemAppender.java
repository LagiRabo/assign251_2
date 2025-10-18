package nz.ac.massey.cs251;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.util.*;

public class MemAppender extends AppenderSkeleton {
    private static MemAppender instance;

    private final List<LoggingEvent> logs;
    private int maxSize = 1000;
    private long discardedCount = 0;

    private MemAppender(List<LoggingEvent> injectedList, Layout layout) {
        this.logs = injectedList != null ? injectedList : new ArrayList<>();
        if (layout != null) {
            this.setLayout(layout);
        }
    }

    public static MemAppender getInstance() {
        return getInstance(null, null);
    }

    public static synchronized MemAppender getInstance(List<LoggingEvent> injectedList, Layout layout) {
        if (instance == null) {
            instance = new MemAppender(injectedList, layout);
        } else {
            if (layout != null) {
                instance.setLayout(layout);
            }
        }
        return instance;
    }

    public static synchronized void _resetForTests(){
        instance = null;
    }

    public void setMaxSize(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        this.maxSize = maxSize;

        while (logs.size() > this.maxSize) {
            logs.remove(0);
            discardedCount++;
        }
    }

    public int getMaxSize() {
        return maxSize;

    }

    public long getDiscardedLogCount() {
        return discardedCount;
    }

    @Override
    protected void append(LoggingEvent event) {
        while (logs.size() >= maxSize) {
            logs.remove(0);
            discardedCount++;
        }
        logs.add(event);

    }


    public List<LoggingEvent> getCurrentLogs() {
        return Collections.unmodifiableList(logs);
    }

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

    public void printLogs() {
        if (this.layout == null) {
            throw new IllegalStateException("Layout not set for MemAppender");
        }
        for (LoggingEvent e : logs) {
            System.out.print(this.layout.format(e));
        }
        logs.clear(); // clearing does not count as discarded
    }

    @Override
    public void close() {
        logs.clear();
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
}
