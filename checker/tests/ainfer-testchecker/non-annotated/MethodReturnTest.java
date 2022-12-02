import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class MethodReturnTest {

  static int getAinferSibling1NotAnnotated() {
    return (@AinferSibling1 int) 0;
  }

  static @AinferSibling1 int getAinferSibling1() {
    // :: warning: (return)
    return getAinferSibling1NotAnnotated();
  }

  public static boolean bool = false;

  public static int lubTest() {
    if (bool) {
      return (@AinferSibling1 int) 0;
    } else {
      return (@AinferSibling2 int) 0;
    }
  }

  public static @AinferParent int getParent() {
    int x = lubTest();
    // :: warning: (return)
    return x;
  }

  class InnerClass {
    int field = 0;

    int getParent2() {
      field = getParent();
      return getParent();
    }

    void receivesAinferSibling1(int i) {
      // :: warning: (argument)
      expectsAinferSibling1(i);
    }

    void expectsAinferSibling1(@AinferSibling1 int i) {}

    void test() {
      @AinferSibling1 int sib = (@AinferSibling1 int) 0;
      receivesAinferSibling1(sib);
    }
  }
}
