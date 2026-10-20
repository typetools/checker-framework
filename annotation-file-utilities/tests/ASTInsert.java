import java.io.PrintStream;
import java.util.List;
import java.util.Map;

@interface Bla {}

public class ASTInsert {

  PrintStream out;
  private int c = 12 + 13;
  private String str = "this" + "is".concat("string");
  private String[] sa = {};

  void m() {
    int i;
  }

  int m(String y, String[] z, int i) {
    String x = new String();
    String s;
    s = x + x;
    s = y;
    s = z[0];
    s = x;
    int j = 0;
    switch (i + 2) {
      case 1:
        j = i + 1;
        System.out.println(1);
        break;
      case 2:
        j = i + 2;
        System.out.println(2);
        break;
      default:
        j = i + 3;
        System.out.println(-1);
    }
    j *= i;
    j = s != x ? j : i;
    do {
      int h = i & j;
    } while (i < j);
    for (int i2 : new int[5]) {
      j = i2;
    }
    for (int a = 0, b = 0; a < j; a = a + 1, b++) a = b;
    if (i < j) i = j;
    else j = i;
    boolean b = x instanceof String;
    label:
    b = false;
    Object o = this.out;
    m(y, z, i);
    int[][] test = new int[4][5];
    int[][] test2 = {{1, 2}, {1, 2, 3}};
    new String("test");
    if (i < 1) return 18;
    synchronized (o) {
      i = i + i;
    }
    if (j < 1) throw new IllegalStateException();
    try {
      int t = 1;
    } catch (Error e) {
      i = j;
    } catch (RuntimeException e) {
      j = i;
    } finally {
      j = i + j;
    }
    j = (int) (i + j);
    j = -j;
    while (i < j) i = i + 1;
    this.out.println();
    System.out.println();
    Object obj = null;
    return 0;
  }

  public <T> void invoc(T t1, T t2) {}

  public void context() {
    this.<String>invoc("a", null);
  }
}

class Wild<X extends List<?>> {
  Wild(Wild<X> n, X p) {}
}

class Unbound<X> {}

class Bound<X extends Object & Comparable<int[]> & Map<? extends Object, ?>, Y> {}
