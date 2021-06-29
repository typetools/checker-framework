// test file for https://github.com/typetools/checker-framework/issues/3438

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

public class OuterClassWithTypeParam<T> {
  public class InnerClass {
    Object o = (@Sibling1 Object) null;
  }
}
