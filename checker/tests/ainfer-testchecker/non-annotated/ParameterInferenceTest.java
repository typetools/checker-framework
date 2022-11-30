import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

public class ParameterInferenceTest {

  void test1() {
    @AinferParent int parent = (@AinferParent int) 0;
    expectsParentNoSignature(parent);
  }

  void expectsParentNoSignature(int t) {
    // :: warning: (assignment)
    @AinferParent int parent = t;
  }

  void test2() {
    @AinferTop int top = (@AinferTop int) 0;
    // :: warning: (argument)
    expectsAinferTopNoSignature(top);
  }

  void expectsAinferTopNoSignature(int t) {}
}
