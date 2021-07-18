import org.checkerframework.checker.units.qual.m;
import org.checkerframework.checker.units.qual.mPERs;
import org.checkerframework.checker.units.qual.s;
import org.checkerframework.checker.units.util.UnitsTools;

// Include all the examples from the manual here,
// to ensure they work as expected.
public class Manual {
    void demo1() {
        @m int meters = 5 * UnitsTools.m;
        @s int secs = 2 * UnitsTools.s;
        @mPERs int speed = meters / secs;
    }
}
