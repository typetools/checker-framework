@SuppressWarnings("deprecation") // Test uses `new Integer(), which is deprecated in Java 9.
public class Issue2353 {

  public static void play() {
    Integer a = new Integer("2");
  }
}
