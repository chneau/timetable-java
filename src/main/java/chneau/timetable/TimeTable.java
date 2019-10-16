package chneau.timetable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chneau.openhours.Whenable;

public final class TimeTable implements Whenable {
    private final List<Point> rel;
    private final Whenable contraint;
    private final double max;

    public TimeTable(double max) {
        this(max, null);
    }

    public TimeTable(double max, Whenable contraint) {
        this(max, contraint, new ArrayList<>());
    }

    public TimeTable(double max, Whenable contraint, List<Point> rel) {
        this.rel = rel;
        this.max = max;
        if (contraint == null) {
            contraint = NoopWhen.getInstance();
        }
        this.contraint = contraint;
    }

    public boolean check() {
        var res = 0;
        for (int i = 0; i < rel.size(); i++) {
            res += rel.get(i).value;
            if (res > max) {
                return false;
            }
        }
        return true;
    }

    public TimeTable add(LocalDateTime from, Duration dur, long cap) {
        var x = new ArrayList<>(rel);
        x.add(new Point(from, cap));
        x.add(new Point(from.plus(dur), -cap));
        Collections.sort(x);
        return new TimeTable(max,this.contraint, x);
    }

    @Override
    public LocalDateTime when(LocalDateTime ldt, Duration d) {
        var t = contraint.when(ldt, d);
        if (t != null) {
            ldt = t;
        }
        return null;
    }
}
