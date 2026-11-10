import java.util.Arrays;
import java.util.List;

class ArraysAsListZeroArgs {
  // Regression test for zero-arg varargs calls with @PolyMustCall. This used to crash the
  // Resource Leak Checker while resolving the return type of Arrays.asList().
  static final List<String> VALUES = Arrays.asList();
}
