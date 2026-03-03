public class InnerClass {

  void m() {
    Object o = new Object();
    if (o instanceof String) {
      String s = (String) o;
    }

    class Inner {

      void m() {
        Object o = new Object();
        if (o instanceof String) {
          String s = (String) o;
        }
      }
    }

    new InnerClass() {

      void m() {
        Object o = new Object();
        if (o instanceof String) {
          String s = (String) o;
        }
      }
    };

    o = new Object();
    if (o instanceof String) {
      String s = (String) o;
    }
  }
}
