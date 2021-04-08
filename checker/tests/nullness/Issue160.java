public class Issue160 {
  public static void t1() {
    String s = null;
    if (s != null) {
    } else {
      return;
    }
    System.out.println(s.toString());
  }

  public static void t2() {
    String s = null;
    if (s != null) {
    } else {
      throw new RuntimeException();
    }
    System.out.println(s.toString());
  }

  public static void t3() {
    String s = null;
    if (s != null) {
    } else {
      System.exit(0);
    }
    System.out.println(s.toString());
  }

  public static void t1b() {
    String s = null;
    if (s == null) {
    } else {
      return;
    }
    // :: error: (dereference.of.nullable)
    System.out.println(s.toString());
  }

  public static void t2b() {
    String s = null;
    if (s == null) {
    } else {
      throw new RuntimeException();
    }
    // :: error: (dereference.of.nullable)
    System.out.println(s.toString());
  }

  public static void t3b() {
    String s = null;
    if (s == null) {
    } else {
      System.exit(0);
    }
    // :: error: (dereference.of.nullable)
    System.out.println(s.toString());
  }
}
