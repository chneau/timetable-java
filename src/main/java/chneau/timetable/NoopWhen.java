package chneau.timetable;

import java.time.Duration;
import java.time.LocalDateTime;

import chneau.openhours.Whenable;

public final class NoopWhen implements Whenable {
    private static final NoopWhen INSTANCE = new NoopWhen();
    @Override
    public LocalDateTime when(LocalDateTime ldt, Duration d) {
        return ldt;
    }

    private NoopWhen() {
    }

    public static NoopWhen getInstance() {
        return INSTANCE;
    }
}
