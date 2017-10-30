import testlib.wholeprograminference.qual.*;

class OverriddenMethodsTestParent {
    public void foo(@Sibling1 Object obj, @Sibling2 Object obj2) {}

    public void bar(@Sibling1 OverriddenMethodsTestParent this, @Sibling2 Object obj) {}

    public void barz(@Sibling1 OverriddenMethodsTestParent this, @Sibling2 Object obj) {}
}

class OverriddenMethodsTestChild extends OverriddenMethodsTestParent {
    @Override
    public void foo(Object obj, Object obj2) {
        // :: error: (assignment.type.incompatible)
        @Sibling1 Object o = obj;
        // :: error: (assignment.type.incompatible)
        @Sibling2 Object o2 = obj2;
    }

    @Override
    public void bar(Object obj) {
        // :: error: (assignment.type.incompatible)
        @Sibling1 OverriddenMethodsTestChild child = this;
        // :: error: (assignment.type.incompatible)
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
