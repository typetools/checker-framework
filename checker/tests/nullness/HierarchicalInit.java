import org.checkerframework.checker.nullness.qual.*;

class HierarchicalInit {

    String a;

    public HierarchicalInit() {
        a = "";
    }

    public static class B extends HierarchicalInit {
        String b;

        public B() {
            super();
            b = "";
        }
    }
}
