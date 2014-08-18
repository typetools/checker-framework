import org.checkerframework.common.value.qual.*;

class ClassNotFound {

    public @StaticallyExecutable static int foo(int a) {
        return a + 2;
    }

    public void bar() {
        int a = 0;
        //:: warning: (class.find.failed)
        foo(a);

    }

}