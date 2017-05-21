import org.checkerframework.checker.interning.qual.*;

public class UnboxUninterned {
    void negation() {
        Boolean t = new Boolean(true);
        boolean b1 = !t.booleanValue();
        boolean b2 = !t;

        Integer x = new Integer(222222);
        int i1 = -x.intValue();
        int i2 = -x;
    }
}
