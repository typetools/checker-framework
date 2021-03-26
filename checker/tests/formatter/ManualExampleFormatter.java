// Example from the manual

import static org.checkerframework.checker.formatter.qual.ConversionCategory.FLOAT;
import static org.checkerframework.checker.formatter.qual.ConversionCategory.INT;

import org.checkerframework.checker.formatter.qual.Format;

public class ManualExampleFormatter {

  void m(boolean flag) {

    @Format({FLOAT, INT}) String f;

    f = "%f %d"; // OK
    f = "%s %d"; // OK, %s is weaker than %f
    // :: warning: (format.missing.arguments)
    f = "%f"; // warning: last argument is ignored
    // :: warning: (format.missing.arguments)
    f = flag ? "%f %d" : "%f";

    if (flag) {
      f = "%f %d";
    } else {
      // :: warning: (format.missing.arguments)
      f = "%f";
    }
    @Format({FLOAT, INT}) String f2 = f;

    // :: error: (assignment.type.incompatible)
    f = "%f %d %s"; // error: too many arguments
    // :: error: (assignment.type.incompatible)
    f = "%d %d"; // error: %d is not weaker than %f

    String.format(f, 0.8, 42);
  }
}
