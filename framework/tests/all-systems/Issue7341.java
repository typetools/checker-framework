public class Issue7341 {
  public static void main(String[] args) {
    Object[] x = new Object[] { "a" };
    Integer index = null;
    if (x[index] instanceof String) {
    }
  }
}
