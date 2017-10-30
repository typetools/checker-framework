import org.checkerframework.common.aliasing.qual.Unique;

class LoaderTest {
    void foo() {
        @Unique Object o = new Object();
        // :: error: (unique.leaked)
        Object[] ar = new Object[] {o};
    }
}
