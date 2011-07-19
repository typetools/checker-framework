import checkers.units.quals.*;
import checkers.units.UnitsTools;

// Include all the examples from the manual here,
// to ensure they work as expected.
public class Manual {
  void demo1() {
    @m int meters = 5 * UnitsTools.m;
    @s int secs = 2 * UnitsTools.s;
    @mPERs int speed = meters / secs;
  }
}
