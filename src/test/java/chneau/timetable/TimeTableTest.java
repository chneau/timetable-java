package chneau.timetable;

import chneau.openhours.OpenHours;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeTableTest {

    @Test
    public void testEmptyAndDefaultConstructor() {
        var tt = new TimeTable(5);
        assertEquals(5.0, tt.getMax());
        assertTrue(tt.isEmpty());
        assertEquals(0, tt.size());
        assertNotNull(tt.getConstraint());
        assertEquals("[]", tt.toString());
    }

    @Test
    public void testValidation() {
        var tt = new TimeTable(5);
        assertThrows(NullPointerException.class, () -> tt.add(null, Duration.ofHours(1), 1));
        assertThrows(NullPointerException.class, () -> tt.add(LocalDateTime.now(), null, 1));
        assertThrows(IllegalArgumentException.class, () -> tt.add(LocalDateTime.now(), Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> tt.add(LocalDateTime.now(), Duration.ofHours(-1), 1));
    }

    @Test
    public void testOverflow() {
        var tt = new TimeTable(1, OpenHours.parse("mo-fr 11:00-16:00"));
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(5), 1);
        assertNotNull(tt, "tt should be instantiated");
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 12, 0), Duration.ofMillis(1), 0.0001);
        assertNull(tt, "tt should be null on overflow");
    }

    @Test
    public void testRangeOverlapAtSameTime() {
        var tt = new TimeTable(2, OpenHours.parse("mo-fr 11:00-16:00"));
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(1), 1);
        assertNotNull(tt);
        tt = tt.add(LocalDateTime.of(2019, 3, 12, 13, 0), Duration.ofHours(1), 2);
        assertNotNull(tt);
        tt = tt.add(LocalDateTime.of(2019, 3, 13, 11, 0), Duration.ofHours(1), 1);
        assertNotNull(tt);
        assertEquals(2.0, tt.getMax());
        assertFalse(tt.isEmpty());
    }

    @Test
    public void testOverlappingAndSimplifying() {
        var tt = new TimeTable(5, OpenHours.parse("mo-fr 11:00-16:00"));
        for (int i = 0; i < 5; i++) {
            tt = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(2), 1);
            assertNotNull(tt);
            tt = tt.add(LocalDateTime.of(2019, 3, 12, 13, 0), Duration.ofHours(2), 1);
            assertNotNull(tt);
        }
        var overflow = tt.add(LocalDateTime.of(2019, 3, 12, 11, 0), Duration.ofHours(2), 1);
        assertNull(overflow);
    }

    @Test
    public void testMicroOverflow() {
        var tt = new TimeTable(10, OpenHours.parse("mo-fr 11:00-16:00"));
        var d = LocalDateTime.of(2019, 3, 12, 10, 0);
        for (int i = 0; i < 1000; i++) {
            var when = tt.when(d, Duration.ofHours(1), 1);
            assertNotNull(when);
            if (when.isAfter(d)) {
                d = when;
            }
            var newtt = tt.add(when, Duration.ofHours(1), 1);
            assertNotNull(newtt);
            tt = newtt;
        }
    }

    @Test
    public void testPointRecordAndEquality() {
        var time1 = LocalDateTime.of(2026, 8, 26, 10, 0);
        var p1 = new Point(time1, 2.5);
        var p2 = new Point(time1, 2.5);
        var p3 = new Point(time1.plusHours(1), 2.5);

        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertTrue(p1.compareTo(p3) < 0);
        assertEquals("{" + time1 + ",2.5}", p1.toString());
        assertTrue(p1.epochSecond() > 0);
    }

    @Test
    public void testJacksonSerialization() throws Exception {
        var mapper = new ObjectMapper();
        var tt = new TimeTable(3.0);
        var t1 = LocalDateTime.of(2026, 8, 26, 9, 0);
        tt = tt.add(t1, Duration.ofHours(2), 1.5);
        assertNotNull(tt);

        String json = mapper.writeValueAsString(tt);
        assertTrue(json.contains("\"max\":3.0"));
        assertTrue(json.contains("\"points\""));

        TimeTable deserialized = mapper.readValue(json, TimeTable.class);
        assertNotNull(deserialized);
        assertEquals(tt.getMax(), deserialized.getMax());
        assertEquals(tt.getPoints(), deserialized.getPoints());
        assertEquals(tt, deserialized);
    }
}

