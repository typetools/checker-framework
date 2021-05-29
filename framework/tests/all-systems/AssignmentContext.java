import org.checkerframework.common.value.qual.MinLen;

@SuppressWarnings("nullness") // Don't want to depend on @Nullable
public class AssignmentContext {

  void foo(String[] a) {}

  void t1(boolean b) {
    String[] s = b ? new String[] {""} : null;
  }

  void t2(boolean b) {
    foo(b ? new String[] {""} : null);
  }

  String[] t3(boolean b) {
    return b ? new String[] {""} : null;
  }

  void t4(boolean b) {
    String[] s = null;
    s = b ? new String[] {""} : null;
  }

  void assignToCast(String @MinLen(4) [] @MinLen(5) [] currentSample) {
    // This statement used to cause a null pointer exception.
    ((String @MinLen(5) []) currentSample[3])[4] = currentSample[3][4];
  }
}
