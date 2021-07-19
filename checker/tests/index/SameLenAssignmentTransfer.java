import org.checkerframework.checker.index.qual.*;

public class SameLenAssignmentTransfer {
  void transfer5(int @SameLen("#2") [] a, int[] b) {
    int[] c = a;
    for (int i = 0; i < c.length; i++) { // i's type is @LTL("c")
      b[i] = 1;
    }
  }
}
