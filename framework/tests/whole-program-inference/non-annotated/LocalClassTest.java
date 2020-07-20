// test case for https://github.com/typetools/checker-framework/issues/3461

import testlib.wholeprograminference.qual.Sibling1;

public class LocalClassTest {
    public void method() {
        class Local {
            Object o = (@Sibling1 Object) null;
        }
    }
}
