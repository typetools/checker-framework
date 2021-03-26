@org.checkerframework.framework.qual.DefaultQualifier(
    org.checkerframework.checker.nullness.qual.Nullable.class)
public class MyException extends Exception {

  public MyException() {}

  public final String getTotalTrace() {
    final StringBuilder sb = new StringBuilder();
    // :: error: (iterating.over.nullable)
    for (StackTraceElement st : getStackTrace()) {
      // :: error: (dereference.of.nullable)
      sb.append(st.toString());
      sb.append(System.lineSeparator());
    }
    return sb.toString();
  }

  @SuppressWarnings("nullness")
  public StackTraceElement[] getStackTrace() {
    throw new RuntimeException("not implemented yet");
  }
}
