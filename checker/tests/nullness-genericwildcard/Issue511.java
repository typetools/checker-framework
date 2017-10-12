// Additional test case for issue #511:
// https://github.com/typetools/checker-framework/issues/511
class MyGeneric<T extends Number> {}

class MySuperClass {
    public void method(MyGeneric<? extends Object> x) {}
}

public class Issue511 extends MySuperClass {
    @Override
    public void method(MyGeneric<?> x) {
        super.method(x);
    }

    // public void method(MyGeneric<? extends Number> x) {}
    // On the above method, javac issues the following error:
    // Issue511.java:19: error: name clash: method(MyGeneric<? extends Number>) in Issue511 and
    // method(MyGeneric<? extends Object>) in MySuperClass have the same erasure, yet neither
    // overrides the other
    // public void method(MyGeneric<? extends Number> x) {}
    //    ^
    //            1 error

}

class Use {
    MyGeneric<? extends Object> wildCardExtendsObject = new MyGeneric<>();
    MyGeneric<? extends Number> wildCardExtendsNumber = wildCardExtendsObject;
    MyGeneric<?> wildCardNoBound = new MyGeneric<>();
    MyGeneric<? extends Number> wildCardExtendsNumber2 = wildCardNoBound;
}
