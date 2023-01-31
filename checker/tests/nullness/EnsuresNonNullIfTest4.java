import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

public class EnsuresNonNullIfTest4 {

  public void add_bottom_up(MyInvariant inv) {

    // The problem goes away if the below line is deleted or replaced with:
    //   Object x = new Object[100];
    Object x = new @Nullable Object[100];

    if (inv.is_ni_suppressed()) {
      Object ss = inv.get_ni_suppressions();
      ss.toString();
    }
  }
}

class MyInvariant {
  @Pure
  public @Nullable Object get_ni_suppressions() {
    return (null);
  }

  @SuppressWarnings("nullness")
  @EnsuresNonNullIf(result = true, expression = "get_ni_suppressions()")
  @Pure
  public boolean is_ni_suppressed() {
    return true;
  }
}
