import org.checkerframework.checker.units.*;
import org.checkerframework.checker.units.qual.*;

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
}
