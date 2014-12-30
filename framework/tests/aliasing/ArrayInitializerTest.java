import org.checkerframework.common.aliasing.qual.Unique;
class ArrayInitializerTest {

    void foo() {
        @Unique Object o = new Object();
        //:: error: (unique.leaked)
        Object[] ar = new Object[]{o};

        Object[] arr = new Object[]{new Object()};

        Object[] arrr = new Object[2];
    }

}