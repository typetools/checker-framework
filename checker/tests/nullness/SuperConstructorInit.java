import org.checkerframework.checker.nullness.qual.*;

class SuperConstructorInit {

    String a;

    public SuperConstructorInit() {
        a = "";
    }

    public static class B extends SuperConstructorInit {
        String b;
        // :: error: (initialization.fields.uninitialized)
        public B() {
            super();
            a.toString();
        }
    }
}
