// Example from the manual

// @skip-test Temporary until issue #740 is fixed:
// https://github.com/typetools/checker-framework/issues/740

import static org.checkerframework.checker.formatter.qual.ConversionCategory.*;
import org.checkerframework.checker.formatter.qual.Format;

public class ManualExample {

  void m() {

    @Format({FLOAT, INT}) String f;

    f = "%f %d";       // OK
    f = "%s %d";       // OK, %s is weaker than %f
    //:: warning: (format.missing.arguments)
    f = "%f";          // warning: last argument is ignored
    //:: error: (assignment.type.incompatible)
    f = "%f %d %s";    // error: too many arguments
    //:: error: (assignment.type.incompatible)
    f = "%d %d";       // error: %d is not weaker than %f

    String.format(f, 0.8, 42);

  }

}
