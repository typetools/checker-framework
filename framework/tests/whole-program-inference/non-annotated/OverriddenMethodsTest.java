import tests.wholeprograminference.qual.*;
class Parent {
    public void foo(@Sibling1 Object obj) {}
    public void bar(@Sibling1 Parent this) {}
}

class Child extends Parent {
    @Override
    public void foo(Object obj) {
        //:: error: (assignment.type.incompatible)
        @Sibling1 Object obj2 = obj;
    }

    @Override
    public void bar() {
        //:: error: (assignment.type.incompatible)
        @Sibling1 Child child = this;
    }
}
