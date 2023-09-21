// JEP 440: Record Patterns
// These are examples copied from:
// https://openjdk.org/jeps/440

// @below-java21-jdk-skip-test
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
public class JEP440 {

  record Point(int x, int y) {}

  static void printSum(Object obj) {
    if (obj instanceof Point(int x, int y)) {
      System.out.println(x + y);
    }
  }

  enum Color {RED, GREEN, BLUE}

  record ColoredPoint(Point p, Color c) {}

  record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}

  static void printUpperLeftColoredPoint(Rectangle r) {
    if (r instanceof Rectangle(ColoredPoint ul, ColoredPoint lr)) {
      System.out.println(ul.c());
    }
  }

  static void printColorOfUpperLeftPoint(Rectangle r) {
    if (r instanceof Rectangle(
        ColoredPoint(Point p, Color c),
        ColoredPoint lr
    )) {
      System.out.println(c);
    }
  }

  static void printXCoordOfUpperLeftPointWithPatterns(Rectangle r) {
    if (r instanceof Rectangle(
        ColoredPoint(Point(var x, var y), var c),
        var lr
    )) {
      System.out.println("Upper-left corner: " + x);
    }
  }

  void failToMatch() {
    record Pair(Object x, Object y) {}
    Pair p = new Pair(42, 42);
    if (p instanceof Pair(String s, String t)) {
      System.out.println(s + ", " + t);
    } else {
      System.out.println("Not a pair of strings");
    }
  }

  record Box<T>(T t) {}

  static void test1(Box<Box<String>> bbs) {
    if (bbs instanceof Box<Box<String>>(Box(var s))) {
      System.out.println("String " + s);
    }
  }

  static void test2(Box<Box<String>> bbs) {
    if (bbs instanceof Box(Box(var s))) {
      System.out.println("String " + s);
    }
  }
}
