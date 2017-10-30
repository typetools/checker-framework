// @skip-test

// To reproduce the problem, run:
//   javac -processor nullness Issue556b.java
//   java Issue556b
// and observe that the javac execution issues no warnings but the java
// execution suffers a null pointer exception.

// Before the constructor is invoked, static initializers and static blocks
// are executed.  This suggests that the Initialization Checker can assume
// that static fields are initialized in the constructor.
//
// However, if any user-defined code -- including callbacks such as
// executions of equals() and hashCode() -- appears in a static initializer
// or static block, then static fields cannot be assumed to be
// initialized within the constructor.

public class Issue556b {
    static class Parent {
        private final Object o;

        public Parent(final Object o) {
            this.o = o;
        }

        @Override
        public String toString() {
            return o.toString();
        }
    }

    static class Child extends Parent {
        public static final Child CHILD = new Child();
        private static final Object OBJ = new Object();

        private Child() {
            // This call should not be legal, because at the time that the
            // call occurs, the static initializers of Child have not yet
            // finished executing and therefore CHILD and OBJ are not
            // necessarily initialized and are not necessarily non-null.
            // :: error: (method.invocation.invalid)
            super(OBJ);
        }
    }

    static class Child2 extends Parent {
        public static final Child2 CHILD;
        private static final Object OBJ;

        static {
            CHILD = new Child2();
            OBJ = new Object();
        }

        private Child2() {
            // This call should not be legal, because at the time that the
            // call occurs, the static initializers of Child have not yet
            // finished executing and therefore CHILD and OBJ are not
            // necessarily initialized and are not necessarily non-null.
            // :: error: (method.invocation.invalid)
            super(OBJ);
        }
    }

    // Changing the order of the OBJ and CHILD fields prevents a null pointer
    // exception.
    static class ChildOk1 extends Parent {
        private static final Object OBJ = new Object();
        public static final Child CHILD = new Child();

        private ChildOk1() {
            // This call is legal, because OBJ is non-null at the time of the
            // call.  That's because OBJ is initialized before CHILD and
            // therefore before the call to "new Child()".
            super(OBJ);
        }
    }

    // Changing the order of the OBJ and CHILD field assignments prevents a
    // null pointer exception.
    static class ChildOk2 extends Parent {
        public static final ChildOk2 CHILD;
        private static final Object OBJ;

        static {
            OBJ = new Object();
            CHILD = new ChildOk2();
        }

        private ChildOk2() {
            // This call is legal, because OBJ is non-null at the time of the
            // call.  That's because OBJ is initialized before CHILD and
            // therefore before the call to "new Child()".
            super(OBJ);
        }
    }

    public static void main(final String[] args) {
        System.out.println(Child.CHILD);
    }
}
