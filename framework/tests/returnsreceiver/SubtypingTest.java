import org.checkerframework.common.returnsreceiver.qual.*;

// Test basic subtyping relationships for the Returns Receiver Checker.
public class SubtypingTest {
  void allSubtypingRelationships(@UnknownThis int x, @BottomThis int y) {
    @UnknownThis int a = x;
    @UnknownThis int b = y;
    // :: error: assignment
    @BottomThis int c = x; // expected error on this line
    @BottomThis int d = y;
  }
}
