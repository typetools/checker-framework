import org.checkerframework.common.value.qual.*;

// Test case for switch statements. Not really about the value checker (more about
// whether the semantics of switch are correct in general), but I needed some
// checker to try it out on.
public class Switch {
  void test1(@IntVal({1, 2, 3, 4, 5}) int x) {

    // easy version, no fall through
    switch (x) {
      case 1:
        @IntVal({1}) int y = x;
        break;
      case 2:
        @IntVal({2}) int w = x;
        // :: error: (assignment.type.incompatible)
        @IntVal({1}) int z = x;
        break;
      default:
        @IntVal({3, 4, 5}) int q = x;
        break;
    }
  }

  void test2(@IntVal({1, 2, 3, 4, 5}) int x) {

    // harder version, fall through
    switch (x) {
      case 1:
        @IntVal({1}) int y = x;
      case 2:
      case 3:
        @IntVal({1, 2, 3}) int w = x;
        // :: error: (assignment.type.incompatible)
        @IntVal({2, 3}) int z = x;
        // :: error: (assignment.type.incompatible)
        @IntVal({3}) int z1 = x;
        break;
      default:
        @IntVal({4, 5}) int q = x;

        // :: error: (assignment.type.incompatible)
        @IntVal(5) int q2 = x;
        break;
    }
  }

  void test3(@IntVal({1, 2, 3, 4, 5}) int x) {

    // harder version, fall through
    switch (x) {
      case 1:
        @IntVal({1}) int y = x;
      case 2:
      case 3:
        @IntVal({1, 2, 3}) int w = x;
        // :: error: (assignment.type.incompatible)
        @IntVal({2, 3}) int z = x;
        // :: error: (assignment.type.incompatible)
        @IntVal({3}) int z1 = x;
        break;
      case 4:
      default:
        @IntVal({4, 5}) int q = x;

        // :: error: (assignment.type.incompatible)
        @IntVal(5) int q2 = x;
        break;
    }
  }

  void test4(int x) {
    switch (x) {
      case 1:
        @IntVal({1}) int y = x;
        break;
      case 2:
      case 3:
        @IntVal({2, 3}) int z = x;
        break;
      case 4:
      default:
        return;
    }
    @IntVal({1, 2, 3}) int y = x;
    // :: error: (assignment.type.incompatible)
    @IntVal(4) int y2 = x;
  }

  void test5(@IntVal({0, 1, 2, 3, 4}) int x) {
    @IntVal({0, 1, 2, 3, 4, 5}) int y = x;
    switch (y = y + 1) {
      case 1:
        @IntVal({1}) int a = y;
        // :: error: (assignment.type.incompatible)
        @IntVal({2}) int b = y;
      case 2:
      case 3:
        @IntVal({1, 2, 3}) int c = y;
        break;
      default:
        // :: error: (assignment.type.incompatible)
        @IntVal({4}) int d = y;
        // :: error: (assignment.type.incompatible)
        @IntVal({5}) int e = y;
        @IntVal({4, 5}) int f = y;
        break;
    }
  }

  void testInts1(@IntRange(from = 0, to = 100) int x) {
    switch (x) {
      case 0:
      case 1:
      case 2:
        @IntVal({0, 1, 2}) int z = x;
        return;
      default:
    }

    @IntRange(from = 3, to = 100) int z = x;
  }

  void testInts2(@IntRange(from = 0, to = 100) int x) {

    // harder version, fall through
    switch (x) {
      case 0:
        @IntVal(0) int a = x;
        break;
      case 1:
        @IntVal(1) int b = x;
        break;
      case 2:
        @IntVal(2) int c = x;
      default:
        @IntRange(from = 2, to = 100) int d = x;
        break;
    }
  }

  void testChars(char x) {
    switch (x) {
      case 'a':
      case 2:
        @IntVal({'a', 2}) int z = x;
        break;
      case 'b':
        @IntVal('b') int v = x;
        break;
      default:
        return;
    }
    @IntVal({'a', 2, 'b'}) int y = x;
  }

  void testStrings1(String s) {
    switch (s) {
      case "Good":
        @StringVal("Good") String x = s;
      case "Bye":
        @StringVal({"Good", "Bye"}) String y = s;
        break;
      case "Hello":
        @StringVal("Hello") String z = s;
        break;
      default:
        return;
    }
    @StringVal({"Good", "Bye", "Hello"}) String q = s;
  }

  void testStrings2(String s) {
    String a;
    switch (a = s) {
      case "Good":
        @StringVal("Good") String x1 = a;
        @StringVal("Good") String x2 = s;
      case "Bye":
        @StringVal({"Good", "Bye"}) String y1 = a;
        @StringVal({"Good", "Bye"}) String y2 = s;
        break;
      case "Hello":
        @StringVal("Hello") String z1 = a;
        @StringVal("Hello") String z2 = s;
        break;
      default:
        return;
    }
    @StringVal({"Good", "Bye", "Hello"}) String q1 = a;
    @StringVal({"Good", "Bye", "Hello"}) String q2 = s;
  }
}
