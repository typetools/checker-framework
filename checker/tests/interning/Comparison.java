import org.checkerframework.checker.interning.qual.Interned;

public class Comparison {

  @Interned String a = "foo";
  @Interned String b = "bar";

  void testInterned() {

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

  String c = new String("foo");
  String d = new String("bar");

  void testNotInterned() {

    // :: error: [not.interned]
    if (c == d) {
      System.out.println("yes");
    } else {
      System.out.println("no");
    }

    // :: error: [not.interned]
    if (c != d) {
      System.out.println("no");
    } else {
      System.out.println("yes");
    }
  }
}
