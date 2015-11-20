// @skip-test

// Addtional test case for issue #511:
// https://github.com/typetools/checker-framework/issues/511
class MyGeneric<T extends Number> {
}

class MySuperClass {
    public void method(MyGeneric<? extends Object> x) {
    }
}

public class Issue511 extends MySuperClass {
    @Override
    public void method(MyGeneric<?> x) {
        super.method(x);
    }
}



