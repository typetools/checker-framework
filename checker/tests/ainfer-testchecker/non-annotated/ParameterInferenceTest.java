import org.checkerframework.checker.testchecker.wholeprograminference.qual.Parent;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Top;

public class ParameterInferenceTest {

  void test1() {
    @Parent int parent = (@Parent int) 0;
    expectsParentNoSignature(parent);
  }

  void expectsParentNoSignature(int t) {
    // :: warning: (assignment)
    @Parent int parent = t;
  }

  void test2() {
    @Top int top = (@Top int) 0;
    // :: warning: (argument)
    expectsTopNoSignature(top);
  }

  void expectsTopNoSignature(int t) {}
}
