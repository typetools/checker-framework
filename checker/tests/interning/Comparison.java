import org.checkerframework.checker.interning.qual.Interned;

public class Comparison {

  void testInterned() {

    @Interned String a = "foo";
    @Interned String b = "bar";

    if (a == b) {
      System.out.println("yes");
    } else {
      System.out.println("no");
    }

    if (a != b) {
      System.out.println("no");
    } else {
      System.out.println("yes");
    }
  }

  void testNotInterned() {

    String c = new String("foo");
    String d = new String("bar");

    // :: error: (not.interned)
    if (c == d) {
      System.out.println("yes");
    } else {
      System.out.println("no");
    }

    // :: error: (not.interned)
    if (c != d) {
      System.out.println("no");
    } else {
      System.out.println("yes");
    }
  }
}
