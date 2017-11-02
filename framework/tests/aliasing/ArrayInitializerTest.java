import org.checkerframework.common.aliasing.qual.Unique;

class ArrayInitializerTest {

    void foo() {
        @Unique Object o = new Object();
        // :: error: (unique.leaked)
        Object[] ar = new Object[] {o};

        @Unique Object o2 = new Object();
        // :: error: (unique.leaked)
        Object @Unique [] ar2 = new Object[] {o2};

        Object[] arr = new Object[] {new Object()};

        Object @Unique [] arrr = new Object[2];
    }
}
