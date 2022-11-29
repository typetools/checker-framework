// test for crashes when delocalizing array variables, indices, array creation expressions, etc
// in annotations

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LessThan;

public class DelocalizeAtCallsites {

  void a1(int[][] a, @IndexFor("#1") int y) {
    int[] z = a[y];
  }

  void a2(int y, int x) {

  }

  void testArrayAccess() {
    int[][] arr = new int[5][5];
    int x1 = 3;
    @SuppressWarnings("all")
    @IndexFor(value={"arr[x1]", "arr"}) int y1 = 2;
    // test that out-of-scope indices are handled properly
    a1(arr, y1);
    // test that out-of-scope arrays are handled properly
    a2(y1, x1);
  }

  void a3(int x) { }

  void testArrayCreation() {
    int x = 10;
    @SuppressWarnings("all")
    @IndexFor("new int[x]") int y = 0;
    a3(y);
    @SuppressWarnings("all")
    @IndexFor("new int[x]{x, x, x, x, x, x, x, x, x, x}") int z = 0;
    a3(z);
  }

  void testBinary() {
    int x1 = 5;
    @LessThan("x1 + 1") int y = 4;
    a3(y);
  }

  void testUnary() {
    int x1 = 5;
    @LessThan("x1++") int y = 4;
    a3(y);
  }

  public int f;

  void testFieldAccess() {
    DelocalizeAtCallsites d = new DelocalizeAtCallsites();
    d.f = 3;
    @LessThan("d.f") int y = 2;
    a3(y);
  }
}
