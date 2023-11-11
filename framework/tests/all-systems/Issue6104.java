public class Issue6104 {
  public void m() {
    try {
    } catch (Exception e) {
      // Because the try block is empty, this lambda is unreachable.
      Runnable r = () -> {};
    }
  }
}
