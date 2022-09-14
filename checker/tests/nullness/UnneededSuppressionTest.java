public class UnneededSuppressionTest {
  @SuppressWarnings({"nullness:method.invocation", "nullness:unneeded.suppression"})
  public String getClassAndUid() {
    return "hello";
  }
}
