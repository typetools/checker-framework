// test case for https://github.com/typetools/checker-framework/issues/3461

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class LocalClassTest {
  public void method() {
    class Local {
      Object o = (@AinferSibling1 Object) null;
    }
  }
}
