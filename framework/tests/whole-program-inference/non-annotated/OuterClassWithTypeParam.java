// test file for https://github.com/typetools/checker-framework/issues/3438

import testlib.wholeprograminference.qual.Sibling1;

public class OuterClassWithTypeParam<T> {
    public class InnerClass {
        Object o = (@Sibling1 Object) null;
    }
}
