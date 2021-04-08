public class TestAnon {
  public void foo() {
    String s = "";
    new Object() {
      public String bar() {
        return s;
      }
    };
  }
}
