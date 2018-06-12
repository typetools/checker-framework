import org.checkerframework.checker.determinism.qual.*;

class MyClass<T> {
    T data;

    MyClass() {}

    MyClass(T data) {
        this.data = data;
    }

    MyClass(int x) {}
}

public class TestTypeParams {
    void testtypes(@Det int a, @NonDet int y) {
        MyClass<Integer> obj = new MyClass<Integer>(a);
        System.out.println(obj);
        MyClass<String> sobj = new MyClass<String>();
        System.out.println(sobj);
        MyClass<Integer> nobj = new MyClass<Integer>(y);
        System.out.println(nobj);
    }
}
