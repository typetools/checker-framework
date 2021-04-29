import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.guieffect.qual.UI;

public class AssignmentTests {
  public static @PolyUIType class P {}
  // We must separate these tests, otherwise the flow sensitivity kicks in and confounds the test
  // results
  public void testBody1(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    ui = safe;
  }

  public void testBody2(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    ui = ui;
  }

  public void testBody3(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    ui = poly;
  }

  public void testBody4(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    safe = safe;
  }

  public void testBody5(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    // :: error: (assignment)
    safe = ui;
  }

  public void testBody6(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    // :: error: (assignment)
    safe = poly;
  }

  public void testBody7(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    poly = safe;
  }

  public void testBody8(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    poly = poly;
  }

  public void testBody9(@UI P ui, @AlwaysSafe P safe, @PolyUI P poly) {
    // :: error: (assignment)
    poly = ui;
  }
}
