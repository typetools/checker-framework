import checkers.units.quals.*;
import checkers.units.UnitsTools;

// Include all the examples from the manual here,
// to ensure they work as expected.
public class Manual {
  void demo1() {
    @m int meters = UnitsTools.toMeter(5);
    @s int secs = UnitsTools.toSecond(2);
    @mPERs int speed = meters / secs;
  }
}
