package annotations.tests.classfile.cases;

import java.util.Set;

public class TestLocalVariableA<T> extends Object {
  public int i;

  public Set<Set> s;

  public TestLocalVariableA() {
    int t = 0;
    i = 0;
  }

  public TestLocalVariableA(int i) {
    this.i = i;
  }

  public TestLocalVariableA(Integer j) {
    int k = 1;
    k++;
    this.i = j;
    k--;
    this.i = k;
  }

  public int i() {
    return i;
  }

  public int j() {
    int temp = 1;
    return j();
  }

  public static void someMethod() {
    TestLocalVariableA t = new TestLocalVariableA();
    String s = new String();
    Double d = Double.valueOf(2);
  }

  public static void main(String[] args) {
    boolean b = true;
    boolean b1 = Boolean.TRUE;
    boolean b2 = (boolean) Boolean.FALSE;
    b = b1 && b2;
    if (b || b2) {
      b1 = b;
    }
    if (b1) {
      System.out.println("Message");
    }
  }
}
