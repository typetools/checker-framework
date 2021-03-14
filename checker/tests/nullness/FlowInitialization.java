import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class FlowInitialization {

  @NonNull String f;
  @Nullable String g;

  // :: error: (initialization.fields.uninitialized)
  public FlowInitialization() {}

  public FlowInitialization(long l) {
    g = "";
    f = g;
  }

  // :: error: (initialization.fields.uninitialized)
  public FlowInitialization(boolean b) {
    if (b) {
      f = "";
    }
  }

  // :: error: (initialization.fields.uninitialized)
  public FlowInitialization(int i) {
    if (i == 0) {
      throw new RuntimeException();
    }
  }

  // :: error: (initialization.fields.uninitialized)
  public FlowInitialization(char c) {
    if (c == 'c') {
      return;
    }
    f = "";
  }

  public FlowInitialization(double d) {
    setField();
  }

  @EnsuresQualifier(expression = "f", qualifier = NonNull.class)
  public void setField(@UnknownInitialization FlowInitialization this) {
    f = "";
  }
}

class FlowPrimitives {
  boolean b;
  int t;
  char c;
}
