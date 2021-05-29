import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
public class JavaCopExplosion {
  public static class ExplosiveException extends Exception {}

  @NonNull Integer m_nni = 1;
  final String m_astring;

  JavaCopExplosion() {
    // m_nni = 1;\
    m_astring = "hi";
    try {
      throw new RuntimeException();
    } catch (Exception e) {
      System.out.println(m_astring.length());
    }
    return;
  }

  static void main(String @NonNull [] args) {
    @NonNull String s = "Dan";
    String s2;
    s2 = null;
    // :: warning: (nulltest.redundant)
    if (s2 != null || s != null) {
      // :: error: (assignment)
      s = s2;
    } else {
      s = new String("Levitan");
    }
    s2 = args[0];
    // :: error: (dereference.of.nullable)
    System.out.println("Possibly cause null pointer with this: " + s2.length());
    // :: warning: (nulltest.redundant)
    if (s2 == null) {
      // do nothing
    } else {
      System.out.println("Can't cause null pointer here: " + s2.length());
      s = s2;
    }
    // :: warning: (nulltest.redundant)
    if (s == null ? s2 != null : s2 != null) {
      s = s2;
    }
    System.out.println("Hello " + s);
    System.out.println("Hello " + s.length());
    f();
  }

  private static int f() {
    while (true) {
      try {
        throw new ExplosiveException();
      } finally {
        // break;
        return 1;
        // throw new RuntimeException();
      }
    }
  }

  public static int foo() {
    final int v;
    int x;
    Integer z;
    Integer y;
    @NonNull Integer nnz = 3;
    z = Integer.valueOf(5);
    try {
      x = 3;
      x = 5;
      // y = z;
      nnz = z;
      z = null;
      // :: error: (assignment)
      nnz = z;

      while (z == null) {
        break;
      }
      // :: error: (assignment)
      nnz = z;
      while (z == null) {
        // do nothing
      }
      nnz = z;
      // v = 1;
      return 1;
      // v = 2;
      // throw new RuntimeException ();
    } catch (NullPointerException e) {
      e.printStackTrace();
      // e = null;
      // v = 1;
    } catch (RuntimeException e) {
      // nnz = z;
      // v = 2;
    } finally {
      // v = 1 + x;
    }
    return 1;
    // return v + x;
  }

  private void bar(List<@NonNull String> ss, String b, String c) {
    @NonNull String a;
    // :: error: (iterating.over.nullable)
    for (@NonNull String s : ss) {
      a = s;
    }
    if (b == null || b.length() == 0) {
      System.out.println("hey");
    }
    if (b != null) {
      // :: error: (dereference.of.nullable)
      for (; b.length() > 0; b = null) {
        System.out.println(b.length());
      }
    }
  }
}
