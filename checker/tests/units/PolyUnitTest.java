import org.checkerframework.checker.units.*;
import org.checkerframework.checker.units.qual.*;
import org.checkerframework.framework.qual.PolyAll;

public class PolyUnitTest {

    @PolyUnit int triplePolyUnit(@PolyUnit int amount) {
        return 3 * amount;
    }

    void testPolyUnit() {
        @m int m1 = 7 * UnitsTools.m;
        @m int m2 = triplePolyUnit(m1);

        @s int sec1 = 7 * UnitsTools.s;
        @s int sec2 = triplePolyUnit(sec1);

        // :: error: (assignment.type.incompatible)
        @s int sec3 = triplePolyUnit(m1);
    }

    @PolyAll int triplePolyAll(@PolyAll int amount) {
        return 3 * amount;
    }

    void testPolyAll() {
        @m int m1 = 7 * UnitsTools.m;
        @m int m2 = triplePolyAll(m1);

        @s int sec1 = 7 * UnitsTools.s;
        @s int sec2 = triplePolyAll(sec1);

        // :: error: (assignment.type.incompatible)
        @s int sec3 = triplePolyAll(m1);
    }
}
