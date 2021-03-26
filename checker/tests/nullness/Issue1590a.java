// This test case shows that @SuppressWarnings("initialization.") has no effect
@SuppressWarnings("initialization.")
public class Issue1590a {

  private String a;

  // :: error: (initialization.fields.uninitialized)
  public Issue1590a() {
    // :: error: (method.invocation.invalid)
    init();
  }

  public void init() {
    a = "gude";
  }
}
