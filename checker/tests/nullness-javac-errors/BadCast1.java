public class BadCast1 {
  public void m() {
    // :: error: illegal start of type :: error: not a statement
    (@NonNull) "";
  }
}
