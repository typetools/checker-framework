import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

public class ConstructorTest {

  public ConstructorTest(int top) {}

  void test() {
    @AinferTop int top = (@AinferTop int) 0;
    // :: warning: (argument)
    new ConstructorTest(top);
  }
}
