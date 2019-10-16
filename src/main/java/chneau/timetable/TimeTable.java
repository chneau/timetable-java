package chneau.timetable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chneau.openhours.Whenable;

public final class TimeTable{
    private List<Point> rel;
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

    private void simplify() {
        var x = new ArrayList<Point>();
        for (int i = 0; i < rel.size(); i++) {
            if (x.size() == 0) {
                x.add(rel.get(i));
                continue;
            }
            if (rel.get(i).time.equals(x.get(i - 1).time)) {
                x.get(i - 1).value += rel.get(i).value;
                if (x.get(x.size() - 1).value == 0) {
                    x.remove(x.size() - 1);
                }
            } else {
                x.add(rel.get(i));
            }
        }
        this.rel = x;
    }

    public TimeTable add(LocalDateTime from, Duration dur, double cap) {
        var x = new ArrayList<>(rel);
        x.add(new Point(from, cap));
        x.add(new Point(from.plus(dur), -cap));
        Collections.sort(x);
        var tt = new TimeTable(max, this.contraint, x);
        tt.simplify();
        return tt;
    }

    public LocalDateTime when(LocalDateTime ldt, Duration d, double cap) {
        {
            var t = contraint.when(ldt, d);
            if (t != null) {
                ldt = t;
            }
        }
        if (add(ldt, d, cap).check()) {
            return ldt;
        }
        for (int i = 0; i < rel.size(); i++) {
            if (rel.get(i).time.isAfter(ldt)) {
                ldt = rel.get(i).time;
            } else {
                continue;
            }
            var t = contraint.when(ldt, d);
            if (t != null) {
                ldt = t;
            }
            if (add(ldt, d, cap).check()) {
                return ldt;
            }
        }
        return null;
    }
}
