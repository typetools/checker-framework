import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

abstract class MethodDefinedInSupertype {

  void test() {
    // :: warning: argument
    expectsAinferSibling1(shouldReturnAinferSibling1());
  }

  public void expectsAinferSibling1(@AinferSibling1 int t) {}

  public abstract int shouldReturnAinferSibling1();

  void testMultipleOverrides() {
    // :: warning: argument
    expectsParent(shouldReturnParent());
  }

  public void expectsParent(@AinferParent int t1) {}

  public abstract int shouldReturnParent();
}
