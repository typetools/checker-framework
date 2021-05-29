import org.checkerframework.checker.testchecker.wholeprograminference.qual.Parent;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;

abstract class MethodDefinedInSupertype {

  void test() {
    // :: warning: argument
    expectsSibling1(shouldReturnSibling1());
  }

  public void expectsSibling1(@Sibling1 int t) {}

  public abstract int shouldReturnSibling1();

  void testMultipleOverrides() {
    // :: warning: argument
    expectsParent(shouldReturnParent());
  }

  public void expectsParent(@Parent int t1) {}

  public abstract int shouldReturnParent();
}
