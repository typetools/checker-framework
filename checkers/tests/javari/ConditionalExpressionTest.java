import java.util.List;
import checkers.javari.quals.*;

class ConditionalExpressionTest {

    void test() {

        @ReadOnly Object ro = null;
        Object mut = null;
        @PolyRead Object rm = null;

        boolean c = false;

        ro = (c ? ro : ro);
        ro = (c ? ro : mut);
        ro = (c ? ro : rm);
        ro = (c ? mut : ro);
        ro = (c ? mut : mut);
        ro = (c ? mut : rm);
        ro = (c ? rm : ro);
        ro = (c ? rm : mut);
        ro = (c ? rm : rm);

        mut = (c ? ro : ro);   // error
        mut = (c ? ro : mut);  // error
        mut = (c ? ro : rm);   // error
        mut = (c ? mut : ro);  // error
        mut = (c ? mut : mut);
        mut = (c ? mut : rm);  // error
        mut = (c ? rm : ro);   // error
        mut = (c ? rm : mut);  // error
        mut = (c ? rm : rm);   // error

        rm = (c ? ro : ro);    // error
        rm = (c ? ro : mut);   // error
        rm = (c ? ro : rm);    // error
        rm = (c ? mut : ro);   // error
        rm = (c ? mut : mut);
        rm = (c ? mut : rm);
        rm = (c ? rm : ro);    // error
        rm = (c ? rm : mut);
        rm = (c ? rm : rm);
    }
}
