import tests.wholeprograminference.qual.*;
class Parent {
    public void foo(@Sibling1 Object obj, @Sibling2 Object obj2) {}
    public void bar(@Sibling1 Parent this, @Sibling2 Object obj) {}
    public void barz(@Sibling1 Parent this, @Sibling2 Object obj) {}
}

class Child extends Parent {
    @Override
    public void foo(Object obj, Object obj2) {
        //:: error: (assignment.type.incompatible)
        @Sibling1 Object o = obj;
        //:: error: (assignment.type.incompatible)
        @Sibling2 Object o2 = obj2;
    }

    @Override
    public void bar(Object obj) {
        //:: error: (assignment.type.incompatible)
        @Sibling1 Child child = this;
        //:: error: (assignment.type.incompatible)
        @Sibling2 Object o = obj;
    }

    @SuppressWarnings("")
    @Override
    public void barz(Object obj) {}

    public void callbarz(Object obj) {
        // If the @SuppressWarnings("") on the overridden version of barz above is not
        // respected, and the annotations on the receiver and parameter of barz are
        // inferred, then the following call to barz will result in a method.invocation.invalid
        // and an argument.type.incompatible type checking errors.
        barz(obj);
    }
}
