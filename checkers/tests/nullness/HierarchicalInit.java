
import checkers.nullness.quals.*;

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
