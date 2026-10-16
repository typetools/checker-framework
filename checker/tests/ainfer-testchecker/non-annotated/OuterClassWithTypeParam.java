// test file for https://github.com/typetools/checker-framework/issues/3438

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class OuterClassWithTypeParam<T> {
  public class InnerClass {
    Object o = (@AinferSibling1 Object) null;
  }
}
