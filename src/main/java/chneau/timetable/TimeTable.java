package chneau.timetable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import chneau.openhours.Whenable;

public final class TimeTable {
    private List<Point> rel;
    private final Whenable contraint;
    private final double max;

    public TimeTable(double max) {
        this(max, null);
    }

    public TimeTable(double max, Whenable contraint) {
        this(max, contraint, new ArrayList<>());
    }

    private TimeTable(double max, Whenable contraint, List<Point> rel) {
        this.rel = rel;
        this.max = max;
        if (contraint == null) {
            contraint = NoopWhen.getInstance();
        }
        this.contraint = contraint;
    }

    private boolean check() {
        var res = .0;
        for (int i = 0; i < rel.size(); i++) {
            res += rel.get(i).value;
            if (res > max) {
                return false;
            }
        }
        if (res != 0) {
            return false;
        }
        return true;
    }

    private void simplify() {
        var map = new HashMap<LocalDateTime, Point>();
        for (var point : rel) {
            if (map.containsKey(point.time)) {
                var p = map.get(point.time);
                p.value += point.value;
                if (p.value == 0) {
                    map.remove(point.time);
                } else {
                    map.put(point.time, p);
                }
            } else {
                map.put(point.time, point);
            }
        }
        rel = map.values().stream().sorted().collect(Collectors.toList());
    }

    public TimeTable add(LocalDateTime from, Duration dur, double cap) {
        var x = new ArrayList<Point>();
        for (var p : rel) {
            var copy = new Point(p.time, p.value);
            x.add(copy);
        }
        x.add(new Point(from, cap));
        x.add(new Point(from.plus(dur), -cap));
        Collections.sort(x);
        var tt = new TimeTable(max, contraint, x);
        tt.simplify();
        if (!tt.check()) {
            return null;
        }
        return tt;
    }

    public String toString() {
        return rel.toString();
    }

    public LocalDateTime when(LocalDateTime ldt, Duration d, double cap) {
        {
            var t = contraint.when(ldt, d);
            if (t != null) {
                ldt = t;
            }
        }
        if (add(ldt, d, cap) != null) {
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
            var test = add(ldt, d, cap);
            if (test == null) {
                continue;
            }
            if (test.check()) {
                return ldt;
            }
        }
        return null;
    }
}
