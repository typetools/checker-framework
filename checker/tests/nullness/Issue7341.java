public class Issue7341 {
  public static void main(String[] args) {
    Object[] x = new Object[] {"a"};
    Integer index = null;
    // :: error: (unboxing.of.nullable)
    if (x[index] instanceof String) {}
  }
}
