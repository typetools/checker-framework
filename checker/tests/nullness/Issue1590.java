@SuppressWarnings("initialization")
public class Issue1590 {

  private String a;

  public Issue1590() {
    // :: error: method.invocation
    init();
  }

  public void init() {
    a = "gude";
  }
}
