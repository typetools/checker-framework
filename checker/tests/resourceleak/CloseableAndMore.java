// A test that when a class implements autocloseable and has another must-call obligation,
// errors are still issued about the other obligation even when it used as a resource variable.

import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

@InheritableMustCall({"close", "foo"})
public class CloseableAndMore implements AutoCloseable {
  void foo() {}

  @Override
  public void close() throws IOException {}

  public static void test_bad() {
    // :: error: required.method.not.called
    try (CloseableAndMore c = new CloseableAndMore()) {

    } catch (Exception e) {
    }
  }

  public static void test_good() {
    try (CloseableAndMore c = new CloseableAndMore()) {
      c.foo();
    } catch (Exception e) {
    }
  }
}
